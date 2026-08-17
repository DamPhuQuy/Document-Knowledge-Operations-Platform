package com.platform.app.ai.domain.model;

import lombok.Builder;

@Builder
public record AssistantResponse(
    String answer,
    Confidence confidence,
    int promptTokens,
    int completionTokens
) {}
