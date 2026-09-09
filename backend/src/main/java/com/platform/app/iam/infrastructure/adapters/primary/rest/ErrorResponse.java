package com.platform.app.iam.infrastructure.adapters.primary.rest;

import java.time.Instant;

public record ErrorResponse(
    int status,
    String error,
    String message,
    Instant timestamp,
    String path) {}
