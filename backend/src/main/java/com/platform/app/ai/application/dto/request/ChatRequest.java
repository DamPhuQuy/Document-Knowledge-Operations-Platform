package com.platform.app.ai.application.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ChatRequest(
    @NotBlank(message = "Message must not be blank")
    @Size(max = 4000, message = "Message must not exceed 4000 characters")
    String message,

    @DecimalMin(value = "0.0", message = "Temperature must be at least 0.0")
    @DecimalMax(value = "2.0", message = "Temperature must not exceed 2.0")
    Double temperature
) {}
