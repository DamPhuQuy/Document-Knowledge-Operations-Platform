package com.platform.app.iam.application.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder
public record DepartmentCreatedEvent(
    UUID departmentId,
    String code,
    String name,
    UUID operatorUserId,
    Instant timestamp
) {}
