package com.platform.app.iam.infrastructure.adapters.primary.rest.dto.response;

import java.time.Instant;

import lombok.Builder;

@Builder
public record ErrorResponse(
    int status,
    String error,
    String message,
    Instant timestamp,
    String path) {}
