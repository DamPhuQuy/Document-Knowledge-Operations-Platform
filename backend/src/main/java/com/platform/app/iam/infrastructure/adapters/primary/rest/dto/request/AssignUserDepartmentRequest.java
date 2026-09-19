package com.platform.app.iam.infrastructure.adapters.primary.rest.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignUserDepartmentRequest(
    UUID departmentId,

    @NotNull(message = "isInternal flag is required") Boolean isInternal
) {}
