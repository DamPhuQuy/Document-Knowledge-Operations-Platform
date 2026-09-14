package com.platform.app.iam.application.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder
public record UserDepartmentAssignedEvent(
    UUID targetUserId,
    UUID departmentId,
    boolean internal,
    UUID operatorUserId,
    Instant timestamp
) {}
