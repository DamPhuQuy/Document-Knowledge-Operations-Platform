package com.platform.app.ai.infrastructure.client.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiChatResponse(
    String id,
    String model,
    List<OpenAiChoiceDto> choices,
    OpenAiUsageDto usage
) {}
