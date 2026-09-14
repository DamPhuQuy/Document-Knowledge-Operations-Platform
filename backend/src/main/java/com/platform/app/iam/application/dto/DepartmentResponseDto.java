package com.platform.app.iam.application.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder
public record DepartmentResponseDto(
    UUID id,
    String code,
    String name,
    String description,
    Instant createdAt,
    Instant updatedAt
) {}
