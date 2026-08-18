package com.platform.app.ai.infrastructure.client;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.platform.app.ai.domain.exception.LlmProviderException;
import com.platform.app.ai.domain.exception.LlmTimeoutException;
import com.platform.app.ai.domain.model.AssistantResponse;
import com.platform.app.ai.domain.model.Confidence;
import com.platform.app.ai.domain.model.LlmMessage;
import com.platform.app.ai.domain.model.LlmRole;
import com.platform.app.ai.infrastructure.config.LlmProperties;
import com.platform.app.ai.grpc.AiServiceGrpc;
import com.platform.app.ai.grpc.ChatRequest;
import com.platform.app.ai.grpc.ChatResponse;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrpcAiClientAdapterTest {

    @Mock
    private AiServiceGrpc.AiServiceBlockingStub blockingStub;

    private LlmProperties properties;
    private GrpcAiClientAdapter adapter;

    @BeforeEach
    void setUp() {
        properties = new LlmProperties(
            "localhost",
            50051,
            Duration.ofSeconds(2)
        );
        adapter = new GrpcAiClientAdapter(blockingStub, properties);
    }

    @Test
    @DisplayName("Should successfully call Python AI gRPC server and return response")
    void shouldSuccessfullyCallGrpcServer() {
        ChatResponse grpcResponse = ChatResponse.newBuilder()
            .setAnswer("Password reset link sent.")
            .setConfidence(com.platform.app.ai.grpc.Confidence.HIGH)
            .setPromptTokens(12)
            .setCompletionTokens(5)
            .build();

        // Stub stubWithDeadline and generateResponse calls
        when(blockingStub.withDeadlineAfter(anyLong(), any())).thenReturn(blockingStub);
        when(blockingStub.generateResponse(any(ChatRequest.class))).thenReturn(grpcResponse);

        AssistantResponse result = adapter.generateResponse(List.of(
            LlmMessage.system("System prompt"),
            LlmMessage.user("Reset my password")
        ), 0.5);

        assertThat(result).isNotNull();
        assertThat(result.answer()).isEqualTo("Password reset link sent.");
        assertThat(result.confidence()).isEqualTo(Confidence.HIGH);
        assertThat(result.promptTokens()).isEqualTo(12);
        assertThat(result.completionTokens()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should throw LlmTimeoutException when gRPC deadline is exceeded")
    void shouldThrowTimeoutExceptionOnDeadlineExceeded() {
        when(blockingStub.withDeadlineAfter(anyLong(), any())).thenReturn(blockingStub);
        when(blockingStub.generateResponse(any()))
            .thenThrow(new StatusRuntimeException(Status.DEADLINE_EXCEEDED.withDescription("Deadline exceeded")));

        assertThatThrownBy(() -> adapter.generateResponse(List.of(LlmMessage.user("Hello")), null))
            .isInstanceOf(LlmTimeoutException.class)
            .hasMessageContaining("timeout or unavailable");
    }

    @Test
    @DisplayName("Should throw LlmProviderException on generic gRPC error")
    void shouldThrowProviderExceptionOnGrpcError() {
        when(blockingStub.withDeadlineAfter(anyLong(), any())).thenReturn(blockingStub);
        when(blockingStub.generateResponse(any()))
            .thenThrow(new StatusRuntimeException(Status.INTERNAL.withDescription("Internal server error")));

        assertThatThrownBy(() -> adapter.generateResponse(List.of(LlmMessage.user("Hello")), null))
            .isInstanceOf(LlmProviderException.class)
            .hasMessageContaining("returned gRPC error");
    }
}
