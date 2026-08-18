package com.platform.app.ai.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiResponse(
    String answer,
    Confidence confidence,

    @JsonProperty("prompt_tokens")
    int promptTokens,

    @JsonProperty("completion_tokens")
    int completionTokens
) {}
