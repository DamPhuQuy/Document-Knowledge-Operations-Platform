package com.platform.app.ai.application.port.out;

import java.util.List;

import com.platform.app.ai.domain.model.AssistantResponse;
import com.platform.app.ai.domain.model.LlmMessage;

public interface LlmClientPort {
    AssistantResponse generateResponse(List<LlmMessage> messages, Double temperature);
}
