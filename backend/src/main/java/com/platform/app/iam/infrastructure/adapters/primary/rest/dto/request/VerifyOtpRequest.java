package com.platform.app.iam.infrastructure.adapters.primary.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record VerifyOtpRequest(
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "OTP must not be blank")
    String otp
) {}
