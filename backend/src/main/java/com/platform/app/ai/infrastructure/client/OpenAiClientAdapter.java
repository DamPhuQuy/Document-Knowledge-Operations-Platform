package com.platform.app.ai.infrastructure.client;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.app.ai.application.port.out.LlmClientPort;
import com.platform.app.ai.domain.exception.LlmProviderException;
import com.platform.app.ai.domain.exception.LlmSchemaValidationException;
import com.platform.app.ai.domain.exception.LlmTimeoutException;
import com.platform.app.ai.domain.model.AssistantResponse;
import com.platform.app.ai.domain.model.Confidence;
import com.platform.app.ai.domain.model.LlmMessage;
import com.platform.app.ai.infrastructure.client.dto.OpenAiChatRequest;
import com.platform.app.ai.infrastructure.client.dto.OpenAiChatResponse;
import com.platform.app.ai.infrastructure.client.dto.OpenAiMessageDto;
import com.platform.app.ai.infrastructure.client.dto.OpenAiStructuredOutputPayload;
import com.platform.app.ai.infrastructure.config.LlmProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClientAdapter implements LlmClientPort {

    private final RestClient llmRestClient;
    private final LlmProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public AssistantResponse generateResponse(List<LlmMessage> messages, Double temperature) {
        List<OpenAiMessageDto> messageDtos = messages.stream()
            .map(m -> OpenAiMessageDto.of(m.role().name().toLowerCase(), m.content()))
            .toList();

        OpenAiChatRequest chatRequest = OpenAiChatRequest.builder()
            .model(properties.model())
            .temperature(temperature != null ? temperature : properties.defaultTemperature())
            .messages(messageDtos)
            .responseFormat(Map.of("type", "json_object"))
            .build();

        String rawResponse = executeWithRetry(chatRequest);
        return parseResponse(rawResponse);
    }

    private String executeWithRetry(OpenAiChatRequest chatRequest) {
        int maxAttempts = Math.max(1, properties.maxRetries());
        Duration delay = properties.retryDelay() != null ? properties.retryDelay() : Duration.ofMillis(500);

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.debug("Calling LLM provider (attempt {}/{})", attempt, maxAttempts);

                return llmRestClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(chatRequest)
                    .retrieve()
                    .body(String.class);

            } catch (ResourceAccessException ex) {
                lastException = ex;
                log.warn("LLM network/timeout on attempt {}/{}: {}", attempt, maxAttempts, ex.getMessage());

                if (attempt < maxAttempts) {
                    sleepWithBackoff(delay, attempt);
                }
            } catch (RestClientResponseException ex) {
                lastException = ex;
                log.warn("LLM provider HTTP error on attempt {}/{}: status={} body={}",
                    attempt, maxAttempts, ex.getStatusCode(), ex.getResponseBodyAsString());

                // Only retry on 5xx server errors or 429 rate limit
                if (ex.getStatusCode().is5xxServerError() || ex.getStatusCode().value() == 429) {
                    if (attempt < maxAttempts) {
                        sleepWithBackoff(delay, attempt);
                        continue;
                    }
                }
                throw new LlmProviderException(
                    "LLM provider error (" + ex.getStatusCode() + "): " + ex.getResponseBodyAsString(), ex);
            } catch (Exception ex) {
                lastException = ex;
                log.error("Unexpected error calling LLM provider: {}", ex.getMessage(), ex);
                throw new LlmProviderException("Unexpected error communicating with LLM provider: " + ex.getMessage(), ex);
            }
        }

        if (lastException instanceof ResourceAccessException) {
            throw new LlmTimeoutException(
                "LLM request timed out or connection failed after " + maxAttempts + " attempts", lastException);
        }

        throw new LlmProviderException("Failed to get response from LLM provider after " + maxAttempts + " attempts", lastException);
    }

    private AssistantResponse parseResponse(String rawResponseJson) {
        if (rawResponseJson == null || rawResponseJson.isBlank()) {
            throw new LlmSchemaValidationException("Received empty response from LLM provider");
        }

        OpenAiChatResponse openAiResponse;
        try {
            openAiResponse = objectMapper.readValue(rawResponseJson, OpenAiChatResponse.class);
        } catch (JsonProcessingException ex) {
            throw new LlmSchemaValidationException("Failed to deserialize LLM provider response envelope: " + ex.getMessage(), ex);
        }

        if (openAiResponse.choices() == null || openAiResponse.choices().isEmpty()) {
            throw new LlmSchemaValidationException("LLM provider returned no choices");
        }

        var choice = openAiResponse.choices().getFirst();
        if (choice.message() == null || choice.message().content() == null) {
            throw new LlmSchemaValidationException("LLM provider returned choice without message content");
        }

        String rawContent = choice.message().content().trim();
        String sanitizedContent = sanitizeJsonContent(rawContent);

        OpenAiStructuredOutputPayload structuredPayload;
        try {
            structuredPayload = objectMapper.readValue(sanitizedContent, OpenAiStructuredOutputPayload.class);
        } catch (JsonProcessingException ex) {
            log.error("Failed to parse structured JSON from LLM: {}", sanitizedContent, ex);
            throw new LlmSchemaValidationException(
                "LLM output is not valid JSON conforming to AssistantResponse schema: " + ex.getMessage(), ex);
        }

        if (structuredPayload.answer() == null || structuredPayload.answer().isBlank()) {
            throw new LlmSchemaValidationException("LLM response is missing required 'answer' field");
        }

        Confidence confidence = structuredPayload.confidence() != null ? structuredPayload.confidence() : Confidence.MEDIUM;

        int promptTokens = openAiResponse.usage() != null ? openAiResponse.usage().promptTokens() : 0;
        int completionTokens = openAiResponse.usage() != null ? openAiResponse.usage().completionTokens() : 0;

        return AssistantResponse.builder()
            .answer(structuredPayload.answer().trim())
            .confidence(confidence)
            .promptTokens(promptTokens)
            .completionTokens(completionTokens)
            .build();
    }

    private String sanitizeJsonContent(String content) {
        if (content.startsWith("```json")) {
            content = content.substring(7);
        } else if (content.startsWith("```")) {
            content = content.substring(3);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }
        return content.trim();
    }

    private void sleepWithBackoff(Duration baseDelay, int attempt) {
        try {
            long sleepMillis = (long) (baseDelay.toMillis() * Math.pow(2, attempt - 1));
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
