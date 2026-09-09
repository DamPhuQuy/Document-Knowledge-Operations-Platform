package com.platform.app.iam.application.dto;

import java.time.Instant;
import java.util.UUID;

public record UserLoginSuccessEvent(
    UUID userId,
    String email,
    String clientIp,
    String userAgent,
    Instant timestamp) {}
