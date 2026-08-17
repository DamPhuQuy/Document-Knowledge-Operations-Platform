package com.platform.app.ai.application.service;

import java.util.List;
import org.springframework.stereotype.Service;

import com.platform.app.ai.application.dto.request.ChatRequest;
import com.platform.app.ai.application.dto.response.ChatResponse;
import com.platform.app.ai.application.port.in.ChatUseCase;
import com.platform.app.ai.application.port.out.LlmClientPort;
import com.platform.app.ai.domain.model.AssistantResponse;
import com.platform.app.ai.domain.model.LlmMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService implements ChatUseCase {

    private final LlmClientPort llmClientPort;

    public static final String SYSTEM_PROMPT = """
        You are an AI customer support assistant.
        Analyze the user's inquiry and provide a clear, helpful response.
        You MUST respond ONLY with a valid JSON object matching this schema:
        {
          "answer": "Your detailed response text here",
          "confidence": "LOW" | "MEDIUM" | "HIGH"
        }
        Do not wrap in markdown tags like ```json. Return pure JSON only.
        """;

    @Override
    public ChatResponse sendMessage(ChatRequest request) {
        log.info("Processing AI chat request (message length: {})", request.message().length());

        List<LlmMessage> messages = List.of(
            LlmMessage.system(SYSTEM_PROMPT),
            LlmMessage.user(request.message())
        );

        AssistantResponse assistantResponse = llmClientPort.generateResponse(messages, request.temperature());

        return ChatResponse.builder()
            .answer(assistantResponse.answer())
            .confidence(assistantResponse.confidence())
            .promptTokens(assistantResponse.promptTokens())
            .completionTokens(assistantResponse.completionTokens())
            .build();
    }
}
