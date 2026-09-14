package com.platform.app.iam.application.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder
public record UserDepartmentResponseDto(
    UUID userId,
    String email,
    String fullName,
    UUID departmentId,
    String departmentCode,
    String departmentName,
    boolean internal,
    Instant updatedAt
) {}
