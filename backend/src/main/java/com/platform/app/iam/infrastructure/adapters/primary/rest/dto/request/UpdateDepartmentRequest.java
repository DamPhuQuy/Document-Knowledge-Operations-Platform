package com.platform.app.iam.infrastructure.adapters.primary.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateDepartmentRequest(
    @NotBlank(message = "Department code is required")
    @Pattern(
        regexp = "^[A-Z0-9_]+$",
        message = "Department code must be uppercase alphanumeric (e.g., HR, FIN, IT, LEGAL)"
    )
    String code,

    @NotBlank(message = "Department name is required") String name,

    String description
) {}
