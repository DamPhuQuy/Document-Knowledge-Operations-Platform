package com.platform.app.iam.infrastructure.adapters.primary.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AssignUserDepartmentRequest(
    UUID departmentId,

    @NotNull(message = "isInternal flag is required")
    Boolean isInternal
) {}
