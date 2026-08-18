package com.platform.app.ai.infrastructure.client;

import com.platform.app.ai.domain.model.AiResponse;
import com.platform.app.ai.domain.model.Confidence;
import com.platform.app.ai.domain.model.LlmMessage;
import com.platform.app.ai.domain.model.LlmRole;
import com.platform.app.shared.config.properties.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestClientAiAdapterTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private AppProperties appProperties;
    private RestClientAiAdapter adapter;

    @BeforeEach
    void setUp() {
        // Build mock AppProperties
        appProperties = new AppProperties();
        AppProperties.Ai ai = new AppProperties.Ai();
        AppProperties.Ai.Llm llm = new AppProperties.Ai.Llm();
        llm.setUrl("http://localhost:8000");
        llm.setGeneratePath("/api/v1/ai/generate");
        ai.setLlm(llm);
        appProperties.setAi(ai);

        adapter = new RestClientAiAdapter(restClient, appProperties);
    }

    @Test
    @DisplayName("Should successfully call Python AI Service REST API and return response")
    void shouldSuccessfullyCallRestClient() {
        AiResponse expectedResponse = new AiResponse(
                "Password reset link sent.",
                Confidence.HIGH,
                12,
                5
        );

        // Stub fluent RestClient API
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(AiResponse.class)).thenReturn(expectedResponse);

        AiResponse result = adapter.generateResponse(List.of(
                new LlmMessage(LlmRole.SYSTEM, "System prompt"),
                new LlmMessage(LlmRole.USER, "Reset my password")
        ), 0.5);

        assertThat(result).isNotNull();
        assertThat(result.answer()).isEqualTo("Password reset link sent.");
        assertThat(result.confidence()).isEqualTo(Confidence.HIGH);
        assertThat(result.promptTokens()).isEqualTo(12);
        assertThat(result.completionTokens()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should throw RuntimeException when RestClient call fails")
    void shouldThrowRuntimeExceptionOnFailure() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> adapter.generateResponse(List.of(new LlmMessage(LlmRole.USER, "Hello")), 0.0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("AI service communication failure");
    }
}
