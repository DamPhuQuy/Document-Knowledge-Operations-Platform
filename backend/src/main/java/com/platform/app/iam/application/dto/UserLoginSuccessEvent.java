package com.platform.app.iam.application.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UserLoginSuccessEvent(
    UUID userId,
    String email,
    String clientIp,
    String userAgent,
    Instant timestamp
) {}
