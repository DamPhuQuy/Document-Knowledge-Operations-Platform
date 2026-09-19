package com.platform.app.iam.application.dto;

import lombok.Builder;

@Builder
public record UserRegisteredOtpEvent(
    String email,
    String otp,
    String fullName
) {}
