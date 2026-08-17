package com.platform.app.ai.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenAiMessageDto(
    String role,
    String content
) {
    public static OpenAiMessageDto of(String role, String content) {
        return new OpenAiMessageDto(role, content);
    }
}
