package com.platform.app.ai.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiChoiceDto(
    int index,
    OpenAiMessageDto message,
    String finishReason
) {}
