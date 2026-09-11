package com.platform.app.iam.infrastructure.adapters.secondary.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@ConfigurationProperties(prefix = "app.jwt")
@Validated
@Builder
public record JwtProperties(
    @NotBlank(message = "JWT secret must not be blank")
    @Size(min = 32, message = "JWT secret must be at least 32 characters/bytes (256 bits)")
    String secret,

    @Positive(message = "Access token expiration ms must be positive")
    long expirationMs,

    @Positive(message = "Refresh token expiration ms must be positive")
    long refreshExpirationMs) {}
