package com.platform.app.ai.infrastructure.client;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAiClientAdapterTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

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
            1, // 1 retry attempt for fast tests
            Duration.ofMillis(10)
        );
        objectMapper = new ObjectMapper();
        adapter = new OpenAiClientAdapter(restClient, properties, objectMapper);
    }

    private void mockRestClientChain(String responseBody) {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(responseBody);
    }

    @Test
    @DisplayName("Should successfully parse clean JSON structured response from OpenAI")
    void shouldParseCleanJsonResponse() {
        String openaiResponse = """
            {
              "id": "chatcmpl-123",
              "model": "gpt-4o-mini",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "{\\"answer\\": \\"Your balance is $50.\\", \\"confidence\\": \\"HIGH\\"}"
                  },
                  "finish_reason": "stop"
                }
              ],
              "usage": {
                "prompt_tokens": 15,
                "completion_tokens": 10,
                "total_tokens": 25
              }
            }
            """;

        mockRestClientChain(openaiResponse);

        AssistantResponse response = adapter.generateResponse(List.of(LlmMessage.user("What is my balance?")), 0.2);

        assertThat(response).isNotNull();
        assertThat(response.answer()).isEqualTo("Your balance is $50.");
        assertThat(response.confidence()).isEqualTo(Confidence.HIGH);
        assertThat(response.promptTokens()).isEqualTo(15);
        assertThat(response.completionTokens()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should parse structured response when content is wrapped in markdown code fence")
    void shouldParseMarkdownWrappedJsonResponse() {
        String openaiResponse = """
            {
              "id": "chatcmpl-456",
              "model": "gpt-4o-mini",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "```json\\n{\\"answer\\": \\"Ticket #100 is resolved.\\", \\"confidence\\": \\"MEDIUM\\"}\\n```"
                  }
                }
              ],
              "usage": {
                "prompt_tokens": 20,
                "completion_tokens": 12,
                "total_tokens": 32
              }
            }
            """;

        mockRestClientChain(openaiResponse);

        AssistantResponse response = adapter.generateResponse(List.of(LlmMessage.user("Status of ticket 100?")), 0.2);

        assertThat(response).isNotNull();
        assertThat(response.answer()).isEqualTo("Ticket #100 is resolved.");
        assertThat(response.confidence()).isEqualTo(Confidence.MEDIUM);
    }

    @Test
    @DisplayName("Should throw LlmSchemaValidationException when LLM response is not valid JSON")
    void shouldThrowSchemaValidationExceptionWhenNotJson() {
        String openaiResponse = """
            {
              "id": "chatcmpl-789",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "I am not able to answer in JSON format."
                  }
                }
              ]
            }
            """;

        mockRestClientChain(openaiResponse);

        assertThatThrownBy(() -> adapter.generateResponse(List.of(LlmMessage.user("Hello")), null))
            .isInstanceOf(LlmSchemaValidationException.class)
            .hasMessageContaining("LLM output is not valid JSON");
    }

    @Test
    @DisplayName("Should throw LlmSchemaValidationException when required 'answer' field is missing")
    void shouldThrowSchemaValidationExceptionWhenAnswerMissing() {
        String openaiResponse = """
            {
              "id": "chatcmpl-789",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "{\\"confidence\\": \\"LOW\\"}"
                  }
                }
              ]
            }
            """;

        mockRestClientChain(openaiResponse);

        assertThatThrownBy(() -> adapter.generateResponse(List.of(LlmMessage.user("Hello")), null))
            .isInstanceOf(LlmSchemaValidationException.class)
            .hasMessageContaining("missing required 'answer' field");
    }

    @Test
    @DisplayName("Should throw LlmTimeoutException on network timeout")
    void shouldThrowTimeoutExceptionOnNetworkError() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenThrow(new ResourceAccessException("Connection timed out"));

        assertThatThrownBy(() -> adapter.generateResponse(List.of(LlmMessage.user("Hello")), null))
            .isInstanceOf(LlmTimeoutException.class)
            .hasMessageContaining("timed out");
    }

    @Test
    @DisplayName("Should throw LlmProviderException on 500 server error")
    void shouldThrowProviderExceptionOn5xx() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenThrow(
            new RestClientResponseException("Internal Error", HttpStatusCode.valueOf(500), "Internal Error",
                HttpHeaders.EMPTY, "{\"error\": \"service overloaded\"}".getBytes(), null));

        assertThatThrownBy(() -> adapter.generateResponse(List.of(LlmMessage.user("Hello")), null))
            .isInstanceOf(LlmProviderException.class)
            .hasMessageContaining("LLM provider error");
    }
}
