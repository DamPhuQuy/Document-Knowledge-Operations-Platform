package com.platform.app.ai.infrastructure.client;

import java.util.List;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIIoException;
import com.openai.models.ResponseFormatJsonObject;
import com.openai.models.ResponsesModel;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseFormatTextConfig;
import com.openai.models.responses.ResponseInputItem;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseTextConfig;
import com.platform.app.ai.application.port.out.LlmClientPort;
import com.platform.app.ai.domain.exception.LlmProviderException;
import com.platform.app.ai.domain.exception.LlmSchemaValidationException;
import com.platform.app.ai.domain.exception.LlmTimeoutException;
import com.platform.app.ai.domain.model.AssistantResponse;
import com.platform.app.ai.domain.model.Confidence;
import com.platform.app.ai.domain.model.LlmMessage;
import com.platform.app.ai.domain.model.LlmRole;
import com.platform.app.ai.infrastructure.client.dto.OpenAiStructuredOutputPayload;
import com.platform.app.ai.infrastructure.config.LlmProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClientAdapter implements LlmClientPort {

    private final OpenAIClient openAIClient;
    private final LlmProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public AssistantResponse generateResponse(List<LlmMessage> messages, Double temperature) {
        String instructions = messages.stream()
            .filter(m -> m.role() == LlmRole.SYSTEM)
            .map(LlmMessage::content)
            .findFirst()
            .orElse(null);

        List<ResponseInputItem> inputItems = messages.stream()
            .filter(m -> m.role() != LlmRole.SYSTEM)
            .map(m -> {
                EasyInputMessage.Role role = (m.role() == LlmRole.ASSISTANT)
                    ? EasyInputMessage.Role.ASSISTANT
                    : EasyInputMessage.Role.USER;
                EasyInputMessage easyMessage = EasyInputMessage.builder()
                    .role(role)
                    .content(EasyInputMessage.Content.ofTextInput(m.content()))
                    .build();
                return ResponseInputItem.ofEasyInputMessage(easyMessage);
            })
            .toList();

        ResponseCreateParams.Builder paramsBuilder = ResponseCreateParams.builder()
            .model(ResponsesModel.ofString(properties.model()))
            .temperature(temperature != null ? temperature : properties.defaultTemperature())
            .text(ResponseTextConfig.builder()
                .format(ResponseFormatTextConfig.ofJsonObject(ResponseFormatJsonObject.builder().build()))
                .build());

        if (instructions != null && !instructions.isBlank()) {
            paramsBuilder.instructions(instructions);
        }

        if (!inputItems.isEmpty()) {
            paramsBuilder.inputOfResponse(inputItems);
        }

        Response response;
        try {
            response = openAIClient.responses().create(paramsBuilder.build());
        } catch (OpenAIIoException ex) {
            log.warn("I/O or timeout error communicating with OpenAI Responses API: {}", ex.getMessage());
            throw new LlmTimeoutException("OpenAI connection/timeout failure: " + ex.getMessage(), ex);
        } catch (OpenAIException ex) {
            log.warn("OpenAI Responses API error: {}", ex.getMessage());
            throw new LlmProviderException("OpenAI provider error: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error calling OpenAI Responses API: {}", ex.getMessage(), ex);
            throw new LlmProviderException("Unexpected error communicating with OpenAI: " + ex.getMessage(), ex);
        }

        return parseResponse(response);
    }

    private AssistantResponse parseResponse(Response response) {
        if (response == null || response.output().isEmpty()) {
            throw new LlmSchemaValidationException("OpenAI Responses API returned no output");
        }

        String rawContent = response.output().stream()
            .filter(ResponseOutputItem::isMessage)
            .map(ResponseOutputItem::asMessage)
            .flatMap(m -> m.content().stream())
            .filter(ResponseOutputMessage.Content::isOutputText)
            .map(c -> c.asOutputText().text())
            .findFirst()
            .orElseThrow(() -> new LlmSchemaValidationException("OpenAI Responses API returned no text output message"));

        String sanitizedContent = sanitizeJsonContent(rawContent.trim());

        OpenAiStructuredOutputPayload structuredPayload;
        try {
            structuredPayload = objectMapper.readValue(sanitizedContent, OpenAiStructuredOutputPayload.class);
        } catch (JsonProcessingException ex) {
            log.error("Failed to parse structured JSON from LLM: {}", sanitizedContent, ex);
            throw new LlmSchemaValidationException(
                "LLM output is not valid JSON conforming to schema: " + ex.getMessage(), ex);
        }

        if (structuredPayload.answer() == null || structuredPayload.answer().isBlank()) {
            throw new LlmSchemaValidationException("LLM response is missing required 'answer' field");
        }

        Confidence confidence = structuredPayload.confidence() != null ? structuredPayload.confidence() : Confidence.MEDIUM;

        int promptTokens = response.usage().map(u -> (int) u.inputTokens()).orElse(0);
        int completionTokens = response.usage().map(u -> (int) u.outputTokens()).orElse(0);

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
}
