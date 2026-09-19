package com.platform.app.iam.application.dto;

import java.util.Set;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UserProfileDto(
    UUID id,
    String email,
    String fullName,
    UUID departmentId,
    boolean isInternal,
    Set<String> roles,
    Set<String> permissions
) {}
