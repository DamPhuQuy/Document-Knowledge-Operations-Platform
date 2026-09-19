package com.platform.app.iam.application.dto;

import java.util.Set;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UserRolesResponseDto(
    UUID userId,
    String email,
    String fullName,
    UUID departmentId,
    Set<RoleDto> roles,
    Set<String> permissions
) {
    @Builder
    public record RoleDto(
        UUID id,
        String code,
        String name,
        String description
    ) {}
}
