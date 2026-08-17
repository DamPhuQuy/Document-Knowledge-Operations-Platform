package com.platform.app.ai.application.port.in;

import com.platform.app.ai.application.dto.request.ChatRequest;
import com.platform.app.ai.application.dto.response.ChatResponse;

public interface ChatUseCase {
    ChatResponse sendMessage(ChatRequest request);
}
