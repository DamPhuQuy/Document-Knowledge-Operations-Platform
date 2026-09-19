package com.platform.app.iam.infrastructure.adapters.primary.rest.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

public record AssignRolesRequest(
    @NotNull(message = "roleIds must not be null")
    @NotEmpty(message = "roleIds must not be empty")
    Set<UUID> roleIds
) {}
