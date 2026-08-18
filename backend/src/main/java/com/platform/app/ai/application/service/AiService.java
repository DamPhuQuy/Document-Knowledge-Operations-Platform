package com.platform.app.ai.application.service;

import com.platform.app.ai.application.port.GenerateResponseUseCase;
import com.platform.app.ai.domain.model.AiResponse;
import com.platform.app.ai.domain.model.LlmMessage;
import com.platform.app.ai.domain.port.AiClientPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService implements GenerateResponseUseCase {

    private final AiClientPort aiClientPort;

    @Override
    public AiResponse generateResponse(List<LlmMessage> messages, double temperature) {
        log.info("Executing AI generate response usecase for {} messages", messages.size());
        return aiClientPort.generateResponse(messages, temperature);
    }
}
