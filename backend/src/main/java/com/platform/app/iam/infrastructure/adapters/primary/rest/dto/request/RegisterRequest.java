package com.platform.app.iam.infrastructure.adapters.primary.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record RegisterRequest(
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password must not be blank")
    String password,

    @NotBlank(message = "First name must not be blank")
    String firstName,

    String lastName
) {}
