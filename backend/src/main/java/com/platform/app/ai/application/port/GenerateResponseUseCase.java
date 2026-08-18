package com.platform.app.ai.application.port;

import com.platform.app.ai.domain.model.AiResponse;
import com.platform.app.ai.domain.model.LlmMessage;
import java.util.List;

public interface GenerateResponseUseCase {
    AiResponse generateResponse(List<LlmMessage> messages, double temperature);
}
