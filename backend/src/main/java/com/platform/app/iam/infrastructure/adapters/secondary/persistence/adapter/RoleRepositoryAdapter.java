package com.platform.app.iam.infrastructure.adapters.secondary.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.platform.app.iam.application.ports.outbound.RoleRepositoryPort;
import com.platform.app.iam.domain.model.Permission;
import com.platform.app.iam.domain.model.Role;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.PermissionJpaEntity;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.RoleJpaEntity;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.repository.SpringDataRoleRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepositoryPort {

  private final SpringDataRoleRepository springDataRoleRepository;

  @Override
  @Transactional(readOnly = true)
  public Set<Role> findByIds(Set<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return Set.of();
    }
    return springDataRoleRepository.findByIdInWithPermissions(ids).stream()
        .map(this::toDomain)
        .collect(Collectors.toSet());
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Role> findByCode(String code) {
    if (code == null) {
      return Optional.empty();
    }
    return springDataRoleRepository
        .findByCodeIgnoreCaseWithPermissions(code.trim())
        .map(this::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Role> findAll() {
    return springDataRoleRepository.findAllWithPermissions().stream()
        .map(this::toDomain)
        .toList();
  }

  private Role toDomain(RoleJpaEntity entity) {
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
