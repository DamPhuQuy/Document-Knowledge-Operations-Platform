package com.platform.app.ai.application.service;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.platform.app.ai.application.dto.request.ChatRequest;
import com.platform.app.ai.application.dto.response.ChatResponse;
import com.platform.app.ai.application.port.out.LlmClientPort;
import com.platform.app.ai.domain.exception.LlmProviderException;
import com.platform.app.ai.domain.exception.LlmSchemaValidationException;
import com.platform.app.ai.domain.exception.LlmTimeoutException;
import com.platform.app.ai.domain.model.AssistantResponse;
import com.platform.app.ai.domain.model.Confidence;
import com.platform.app.ai.domain.model.LlmMessage;
import com.platform.app.ai.domain.model.LlmRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private LlmClientPort llmClientPort;

    @InjectMocks
    private ChatService chatService;

    private ChatRequest validRequest;
    private AssistantResponse mockResponse;

    @BeforeEach
    void setUp() {
        validRequest = ChatRequest.builder()
            .message("How can I reset my password?")
            .temperature(0.5)
            .build();

        mockResponse = AssistantResponse.builder()
            .answer("You can reset your password from the login screen by clicking 'Forgot Password'.")
            .confidence(Confidence.HIGH)
            .promptTokens(45)
            .completionTokens(20)
            .build();
    }

    @Test
    @DisplayName("Should successfully send message and receive structured response")
    void shouldSuccessfullySendMessage() {
        when(llmClientPort.generateResponse(any(), eq(0.5))).thenReturn(mockResponse);

        ChatResponse result = chatService.sendMessage(validRequest);

        assertThat(result).isNotNull();
        assertThat(result.answer()).isEqualTo(mockResponse.answer());
        assertThat(result.confidence()).isEqualTo(Confidence.HIGH);
        assertThat(result.promptTokens()).isEqualTo(45);
        assertThat(result.completionTokens()).isEqualTo(20);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LlmMessage>> messagesCaptor = ArgumentCaptor.forClass(List.class);
        verify(llmClientPort).generateResponse(messagesCaptor.capture(), eq(0.5));

        List<LlmMessage> capturedMessages = messagesCaptor.getValue();
        assertThat(capturedMessages).hasSize(2);
        assertThat(capturedMessages.get(0).role()).isEqualTo(LlmRole.SYSTEM);
        assertThat(capturedMessages.get(0).content()).contains("JSON");
        assertThat(capturedMessages.get(1).role()).isEqualTo(LlmRole.USER);
        assertThat(capturedMessages.get(1).content()).isEqualTo("How can I reset my password?");
    }

    @Test
    @DisplayName("Should propagate LlmTimeoutException when LLM port times out")
    void shouldPropagateTimeoutException() {
        when(llmClientPort.generateResponse(any(), any()))
            .thenThrow(new LlmTimeoutException("Request timed out after 10s"));

        assertThatThrownBy(() -> chatService.sendMessage(validRequest))
            .isInstanceOf(LlmTimeoutException.class)
            .hasMessageContaining("Request timed out");
    }

    @Test
    @DisplayName("Should propagate LlmProviderException when LLM upstream returns error")
    void shouldPropagateProviderException() {
        when(llmClientPort.generateResponse(any(), any()))
            .thenThrow(new LlmProviderException("500 Internal Server Error from LLM provider"));

        assertThatThrownBy(() -> chatService.sendMessage(validRequest))
            .isInstanceOf(LlmProviderException.class)
            .hasMessageContaining("500 Internal Server Error");
    }

    @Test
    @DisplayName("Should propagate LlmSchemaValidationException when LLM response is malformed")
    void shouldPropagateSchemaValidationException() {
        when(llmClientPort.generateResponse(any(), any()))
            .thenThrow(new LlmSchemaValidationException("Failed to parse JSON response"));

        assertThatThrownBy(() -> chatService.sendMessage(validRequest))
            .isInstanceOf(LlmSchemaValidationException.class)
            .hasMessageContaining("Failed to parse JSON");
    }
}
