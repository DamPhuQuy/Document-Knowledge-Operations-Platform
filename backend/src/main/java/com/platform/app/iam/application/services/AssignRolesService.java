package com.platform.app.iam.application.services;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.platform.app.iam.application.dto.UserRolesResponseDto;
import com.platform.app.iam.application.dto.UserRolesUpdatedEvent;
import com.platform.app.iam.application.ports.inbound.AssignRolesCommand;
import com.platform.app.iam.application.ports.inbound.AssignRolesUseCase;
import com.platform.app.iam.application.ports.inbound.GetUserRolesUseCase;
import com.platform.app.iam.application.ports.outbound.RoleRepositoryPort;
import com.platform.app.iam.application.ports.outbound.UserRepositoryPort;
import com.platform.app.iam.domain.model.Role;
import com.platform.app.iam.domain.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignRolesService implements AssignRolesUseCase, GetUserRolesUseCase {

  private final UserRepositoryPort userRepositoryPort;
  private final RoleRepositoryPort roleRepositoryPort;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional(readOnly = true)
  public UserRolesResponseDto getUserRoles(UUID userId) {
    Objects.requireNonNull(userId, "userId must not be null");
    User user =
        userRepositoryPort
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

    return toResponseDto(user);
  }

  @Override
  @Transactional
  public UserRolesResponseDto assignRoles(AssignRolesCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    Objects.requireNonNull(command.targetUserId(), "targetUserId must not be null");

    User user =
        userRepositoryPort
            .findById(command.targetUserId())
            .orElseThrow(
                () -> new IllegalArgumentException("User not found: " + command.targetUserId()));

    Set<String> oldRoleCodes = user.getRoleCodes();

    Set<UUID> targetRoleIds = command.roleIds() != null ? command.roleIds() : Set.of();
    Set<Role> resolvedRoles = roleRepositoryPort.findByIds(targetRoleIds);

    if (resolvedRoles.size() != targetRoleIds.size()) {
      throw new IllegalArgumentException("One or more specified role IDs do not exist");
    }

    user.assignRoles(resolvedRoles, command.operatorUserId());

    User savedUser = userRepositoryPort.save(user);
    log.info(
        "Assigned {} roles to user [{}] by operator [{}] (roles: {} -> {})",
        resolvedRoles.size(),
        savedUser.getId(),
        command.operatorUserId(),
        oldRoleCodes,
        savedUser.getRoleCodes());

    eventPublisher.publishEvent(
        new UserRolesUpdatedEvent(
            savedUser.getId(),
            oldRoleCodes,
            savedUser.getRoleCodes(),
            command.operatorUserId(),
            Instant.now()));

    return toResponseDto(savedUser);
  }

  private UserRolesResponseDto toResponseDto(User user) {
    Set<UserRolesResponseDto.RoleDto> roleDtos =
        user.getRoles().stream()
            .map(
                role ->
                    UserRolesResponseDto.RoleDto.builder()
                        .id(role.getId())
                        .code(role.getCode())
                        .name(role.getName())
                        .description(role.getDescription())
                        .build())
            .collect(Collectors.toSet());

    return UserRolesResponseDto.builder()
        .userId(user.getId())
        .email(user.getEmail())
        .fullName(user.getFullName())
        .departmentId(user.getDepartmentId())
        .roles(roleDtos)
        .permissions(user.getAllPermissionCodes())
        .build();
  }
}
