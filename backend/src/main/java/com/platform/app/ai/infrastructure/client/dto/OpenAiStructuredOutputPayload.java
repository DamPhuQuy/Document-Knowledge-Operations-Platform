package com.platform.app.ai.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.platform.app.ai.domain.model.Confidence;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiStructuredOutputPayload(
    String answer,
    Confidence confidence
) {}
