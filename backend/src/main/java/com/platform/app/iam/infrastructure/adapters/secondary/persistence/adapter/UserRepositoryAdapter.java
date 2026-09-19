package com.platform.app.iam.infrastructure.adapters.secondary.persistence.adapter;

import com.platform.app.iam.application.ports.outbound.UserRepositoryPort;
import com.platform.app.iam.domain.model.Permission;
import com.platform.app.iam.domain.model.Role;
import com.platform.app.iam.domain.model.User;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.PermissionJpaEntity;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.RoleJpaEntity;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.UserJpaEntity;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.repository.SpringDataRoleRepository;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.repository.SpringDataUserRepository;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository springDataUserRepository;
    private final SpringDataRoleRepository springDataRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return springDataUserRepository
            .findByEmailIgnoreCaseWithRolesAndPermissions(
                email.trim().toLowerCase()
            )
            .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return springDataUserRepository
            .findByIdWithRolesAndPermissions(id)
            .map(this::toDomain);
    }

    @Override
    @Transactional
    public User save(User user) {
        Objects.requireNonNull(user, "user must not be null");

        UserJpaEntity entity = springDataUserRepository
            .findById(user.getId())
            .orElseGet(() ->
                UserJpaEntity.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .passwordHash(user.getPasswordHash())
                    .fullName(user.getFullName())
                    .departmentId(user.getDepartmentId())
                    .enabled(user.isEnabled())
                    .isInternal(user.isInternal())
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    .roles(new HashSet<>())
                    .build()
            );

        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setFullName(user.getFullName());
        entity.setDepartmentId(user.getDepartmentId());
        entity.setEnabled(user.isEnabled());
        entity.setInternal(user.isInternal());
        entity.setUpdatedAt(user.getUpdatedAt());

        Set<UUID> roleIds = user.getRoleIds();
        if (roleIds != null && !roleIds.isEmpty()) {
            Set<RoleJpaEntity> roleEntities =
                springDataRoleRepository.findByIdInWithPermissions(roleIds);
            entity.setRoles(new HashSet<>(roleEntities));
        } else {
            entity.getRoles().clear();
        }

        UserJpaEntity savedEntity = springDataUserRepository.save(entity);
        return toDomain(savedEntity);
    }

    private User toDomain(UserJpaEntity entity) {
        Set<Role> roles = entity
            .getRoles()
            .stream()
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
        Set<Permission> permissions = entity
            .getPermissions()
            .stream()
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
