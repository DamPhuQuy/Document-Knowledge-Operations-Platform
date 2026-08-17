package com.platform.app.ai.infrastructure.client;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIException;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.services.blocking.ResponseService;
import com.platform.app.ai.domain.exception.LlmProviderException;
import com.platform.app.ai.domain.exception.LlmSchemaValidationException;
import com.platform.app.ai.domain.exception.LlmTimeoutException;
import com.platform.app.ai.domain.model.AssistantResponse;
import com.platform.app.ai.domain.model.Confidence;
import com.platform.app.ai.domain.model.LlmMessage;
import com.platform.app.ai.infrastructure.config.LlmProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAiClientAdapterTest {

    @Mock
    private OpenAIClient openAIClient;

    @Mock
    private ResponseService responseService;

    private LlmProperties properties;
    private ObjectMapper objectMapper;
    private OpenAiClientAdapter adapter;

    @BeforeEach
    void setUp() {
        properties = new LlmProperties(
            "sk-test-key",
            "https://api.openai.com/v1",
            "gpt-4o-mini",
            0.2,
            Duration.ofSeconds(2),
            1,
            Duration.ofMillis(10)
        );
        objectMapper = new ObjectMapper();
        adapter = new OpenAiClientAdapter(openAIClient, properties, objectMapper);
    }

    private void mockResponse(Response response) {
        when(openAIClient.responses()).thenReturn(responseService);
        when(responseService.create(any(ResponseCreateParams.class))).thenReturn(response);
    }

    private Response createMockResponse(String id, String content, long promptTokens, long completionTokens) {
        try {
            String json = """
                {
                  "id": "%s",
                  "created_at": 1700000000,
                  "model": "gpt-4o-mini",
                  "status": "completed",
                  "parallel_tool_calls": false,
                  "output": [
                    {
                      "id": "msg-1",
                      "type": "message",
                      "role": "assistant",
                      "status": "completed",
                      "content": [
                        {
                          "type": "output_text",
                          "text": %s,
                          "annotations": []
                        }
                      ]
                    }
                  ],
                  "usage": {
                    "input_tokens": %d,
                    "output_tokens": %d,
                    "total_tokens": %d,
                    "input_tokens_details": {"cached_tokens": 0},
                    "output_tokens_details": {"reasoning_tokens": 0}
                  }
                }
                """.formatted(
                    id,
                    objectMapper.writeValueAsString(content),
                    promptTokens,
                    completionTokens,
                    promptTokens + completionTokens
                );
            return objectMapper.readValue(json, Response.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize mock Response JSON", e);
        }
    }

    @Test
    @DisplayName("Should successfully parse clean JSON structured response from OpenAI Responses API")
    void shouldParseCleanJsonResponse() {
        Response response = createMockResponse(
            "resp-123",
            "{\"answer\": \"Your balance is $50.\", \"confidence\": \"HIGH\"}",
            15,
            10
        );

        mockResponse(response);

        AssistantResponse result = adapter.generateResponse(List.of(
            LlmMessage.system("You are an AI assistant"),
            LlmMessage.user("What is my balance?")
        ), 0.2);

        assertThat(result).isNotNull();
        assertThat(result.answer()).isEqualTo("Your balance is $50.");
        assertThat(result.confidence()).isEqualTo(Confidence.HIGH);
        assertThat(result.promptTokens()).isEqualTo(15);
        assertThat(result.completionTokens()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should parse structured response when content is wrapped in markdown code fence")
    void shouldParseMarkdownWrappedJsonResponse() {
        Response response = createMockResponse(
            "resp-456",
            "```json\n{\"answer\": \"Ticket #100 is resolved.\", \"confidence\": \"MEDIUM\"}\n```",
            20,
            12
        );

        mockResponse(response);

        AssistantResponse result = adapter.generateResponse(List.of(LlmMessage.user("Status of ticket 100?")), 0.2);

        assertThat(result).isNotNull();
        assertThat(result.answer()).isEqualTo("Ticket #100 is resolved.");
        assertThat(result.confidence()).isEqualTo(Confidence.MEDIUM);
    }

    @Test
    @DisplayName("Should throw LlmSchemaValidationException when Responses API returns non-JSON text")
    void shouldThrowSchemaValidationExceptionWhenNotJson() {
        Response response = createMockResponse(
            "resp-789",
            "I cannot answer in JSON format.",
            10,
            5
        );

        mockResponse(response);

        assertThatThrownBy(() -> adapter.generateResponse(List.of(LlmMessage.user("Hello")), null))
            .isInstanceOf(LlmSchemaValidationException.class)
            .hasMessageContaining("LLM output is not valid JSON");
    }

    @Test
    @DisplayName("Should throw LlmSchemaValidationException when required 'answer' field is missing")
    void shouldThrowSchemaValidationExceptionWhenAnswerMissing() {
        Response response = createMockResponse(
            "resp-789",
            "{\"confidence\": \"LOW\"}",
            10,
            5
        );

        mockResponse(response);

        assertThatThrownBy(() -> adapter.generateResponse(List.of(LlmMessage.user("Hello")), null))
            .isInstanceOf(LlmSchemaValidationException.class)
            .hasMessageContaining("missing required 'answer' field");
    }

    @Test
    @DisplayName("Should throw LlmTimeoutException on I/O or connection error from Responses API")
    void shouldThrowTimeoutExceptionOnNetworkError() {
        when(openAIClient.responses()).thenReturn(responseService);
        when(responseService.create(any(ResponseCreateParams.class)))
            .thenThrow(new OpenAIIoException("Connection reset / timed out"));

        assertThatThrownBy(() -> adapter.generateResponse(List.of(LlmMessage.user("Hello")), null))
            .isInstanceOf(LlmTimeoutException.class)
            .hasMessageContaining("OpenAI connection/timeout failure");
    }

    @Test
    @DisplayName("Should throw LlmProviderException on Responses API error")
    void shouldThrowProviderExceptionOnApiError() {
        when(openAIClient.responses()).thenReturn(responseService);
        when(responseService.create(any(ResponseCreateParams.class)))
            .thenThrow(new OpenAIException("OpenAI Responses API error"));

        assertThatThrownBy(() -> adapter.generateResponse(List.of(LlmMessage.user("Hello")), null))
            .isInstanceOf(LlmProviderException.class)
            .hasMessageContaining("OpenAI provider error");
    }
}
