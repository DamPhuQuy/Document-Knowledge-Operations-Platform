package com.platform.app.ai.infrastructure.client.dto;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenAiChatRequest(
    String model,
    List<OpenAiMessageDto> messages,
    Double temperature,
    @JsonProperty("response_format")
    Map<String, String> responseFormat
) {}
