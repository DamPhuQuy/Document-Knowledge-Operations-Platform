package com.platform.app.ai.application.dto.response;

import com.platform.app.ai.domain.model.Confidence;
import lombok.Builder;

@Builder
public record ChatResponse(
    String answer,
    Confidence confidence,
    int promptTokens,
    int completionTokens
) {}
