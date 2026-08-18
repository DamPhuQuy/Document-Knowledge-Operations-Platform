package com.platform.app.ai.infrastructure.client;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.platform.app.ai.application.port.out.LlmClientPort;
import com.platform.app.ai.domain.exception.LlmProviderException;
import com.platform.app.ai.domain.exception.LlmTimeoutException;
import com.platform.app.ai.domain.model.AssistantResponse;
import com.platform.app.ai.domain.model.Confidence;
import com.platform.app.ai.domain.model.LlmMessage;
import com.platform.app.ai.infrastructure.config.LlmProperties;
import com.platform.app.ai.grpc.AiServiceGrpc;
import com.platform.app.ai.grpc.ChatRequest;
import com.platform.app.ai.grpc.ChatResponse;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GrpcAiClientAdapter implements LlmClientPort {

    private final AiServiceGrpc.AiServiceBlockingStub aiServiceBlockingStub;
    private final LlmProperties properties;

    @Override
    public AssistantResponse generateResponse(List<LlmMessage> messages, Double temperature) {
        log.info("Sending AI chat request via gRPC to Python AI service");

        // Build gRPC request
        ChatRequest.Builder requestBuilder = ChatRequest.newBuilder();
        
        if (temperature != null) {
            requestBuilder.setTemperature(temperature);
        }

        // Map messages
        for (LlmMessage msg : messages) {
            com.platform.app.ai.grpc.LlmRole grpcRole;
            switch (msg.role()) {
                case SYSTEM -> grpcRole = com.platform.app.ai.grpc.LlmRole.SYSTEM;
                case ASSISTANT -> grpcRole = com.platform.app.ai.grpc.LlmRole.ASSISTANT;
                default -> grpcRole = com.platform.app.ai.grpc.LlmRole.USER;
            }

            com.platform.app.ai.grpc.LlmMessage grpcMsg = com.platform.app.ai.grpc.LlmMessage.newBuilder()
                .setRole(grpcRole)
                .setContent(msg.content())
                .build();
            
            requestBuilder.addMessages(grpcMsg);
        }

        ChatResponse response;
        try {
            // Apply deadline based on properties timeout configuration
            AiServiceGrpc.AiServiceBlockingStub stubWithDeadline = aiServiceBlockingStub
                .withDeadlineAfter(properties.timeout().toMillis(), TimeUnit.MILLISECONDS);

            response = stubWithDeadline.generateResponse(requestBuilder.build());
        } catch (StatusRuntimeException ex) {
            Status.Code code = ex.getStatus().getCode();
            log.warn("gRPC call failed with status: {}, description: {}", code, ex.getStatus().getDescription());
            
            if (code == Status.Code.DEADLINE_EXCEEDED || code == Status.Code.UNAVAILABLE) {
                throw new LlmTimeoutException("Python AI service timeout or unavailable: " + ex.getMessage(), ex);
            }
            throw new LlmProviderException("Python AI service returned gRPC error: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error calling Python AI gRPC service: {}", ex.getMessage(), ex);
            throw new LlmProviderException("Unexpected gRPC error: " + ex.getMessage(), ex);
        }

        // Map gRPC response to Domain model
        Confidence confidence;
        switch (response.getConfidence()) {
            case LOW -> confidence = Confidence.LOW;
            case HIGH -> confidence = Confidence.HIGH;
            default -> confidence = Confidence.MEDIUM;
        }

        return AssistantResponse.builder()
            .answer(response.getAnswer())
            .confidence(confidence)
            .promptTokens(response.getPromptTokens())
            .completionTokens(response.getCompletionTokens())
            .build();
    }
}
