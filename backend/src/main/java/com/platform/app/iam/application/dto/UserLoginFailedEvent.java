package com.platform.app.iam.application.dto;

import java.time.Instant;
import lombok.Builder;

@Builder
public record UserLoginFailedEvent(
    String email,
    String clientIp,
    String userAgent,
    String reason,
    Instant timestamp) {}
