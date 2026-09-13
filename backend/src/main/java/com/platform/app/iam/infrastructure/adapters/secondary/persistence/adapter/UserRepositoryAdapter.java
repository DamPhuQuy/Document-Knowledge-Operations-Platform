package com.platform.app.iam.infrastructure.adapters.secondary.persistence.adapter;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.platform.app.iam.application.ports.outbound.UserRepositoryPort;
import com.platform.app.iam.domain.model.Permission;
import com.platform.app.iam.domain.model.Role;
import com.platform.app.iam.domain.model.User;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.PermissionJpaEntity;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.RoleJpaEntity;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.UserJpaEntity;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.repository.SpringDataUserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

  private final SpringDataUserRepository springDataUserRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<User> findByEmail(String email) {
    if (email == null) {
      return Optional.empty();
    }
    return springDataUserRepository
        .findByEmailIgnoreCaseWithRolesAndPermissions(email.trim().toLowerCase())
        .map(this::toDomain);
  }

  private User toDomain(UserJpaEntity entity) {
    Set<Role> roles =
        entity.getRoles().stream()
            .map(this::toDomainRole)
            .collect(Collectors.toSet());

    return User.builder()
        .id(entity.getId())
        .email(entity.getEmail())
        .passwordHash(entity.getPasswordHash())
        .fullName(entity.getFullName())
        .departmentId(entity.getDepartmentId())
        .enabled(entity.isEnabled())
        .internal(entity.isInternal())
        .roles(roles)
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }

  private Role toDomainRole(RoleJpaEntity entity) {
    Set<Permission> permissions =
        entity.getPermissions().stream()
            .map(this::toDomainPermission)
            .collect(Collectors.toSet());

    return Role.builder()
        .id(entity.getId())
        .code(entity.getCode())
        .name(entity.getName())
        .description(entity.getDescription())
        .permissions(permissions)
        .build();
  }

  private Permission toDomainPermission(PermissionJpaEntity entity) {
    return Permission.builder()
        .id(entity.getId())
        .code(entity.getCode())
        .name(entity.getName())
        .module(entity.getModule())
        .description(entity.getDescription())
        .build();
  }
}
