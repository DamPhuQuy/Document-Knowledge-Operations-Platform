package com.platform.app.ai.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiUsageDto(
    @JsonProperty("prompt_tokens")
    int promptTokens,

    @JsonProperty("completion_tokens")
    int completionTokens,

    @JsonProperty("total_tokens")
    int totalTokens
) {}
