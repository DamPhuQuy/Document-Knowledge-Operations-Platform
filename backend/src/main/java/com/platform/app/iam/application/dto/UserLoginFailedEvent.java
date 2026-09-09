package com.platform.app.iam.application.dto;

import java.time.Instant;

public record UserLoginFailedEvent(
    String email,
    String clientIp,
    String userAgent,
    String reason,
    Instant timestamp) {}
