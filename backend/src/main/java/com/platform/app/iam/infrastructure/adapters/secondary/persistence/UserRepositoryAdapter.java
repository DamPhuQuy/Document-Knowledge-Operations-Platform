package com.platform.app.iam.infrastructure.adapters.secondary.persistence;

import com.platform.app.iam.application.ports.outbound.UserRepositoryPort;
import com.platform.app.iam.domain.model.DepartmentId;
import com.platform.app.iam.domain.model.Permission;
import com.platform.app.iam.domain.model.Role;
import com.platform.app.iam.domain.model.RoleId;
import com.platform.app.iam.domain.model.User;
import com.platform.app.iam.domain.model.UserId;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

  private final SpringDataUserRepository springDataUserRepository;

  public UserRepositoryAdapter(SpringDataUserRepository springDataUserRepository) {
    this.springDataUserRepository =
        Objects.requireNonNull(springDataUserRepository, "springDataUserRepository must not be null");
  }

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

    return new User(
        UserId.from(entity.getId()),
        entity.getEmail(),
        entity.getPasswordHash(),
        entity.getFullName(),
        DepartmentId.from(entity.getDepartmentId()),
        entity.isEnabled(),
        entity.isInternal(),
        roles,
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  private Role toDomainRole(RoleJpaEntity entity) {
    Set<Permission> permissions =
        entity.getPermissions().stream()
            .map(this::toDomainPermission)
            .collect(Collectors.toSet());

    return new Role(
        RoleId.from(entity.getId()),
        entity.getCode(),
        entity.getName(),
        entity.getDescription(),
        permissions);
  }

  private Permission toDomainPermission(PermissionJpaEntity entity) {
    return new Permission(
        entity.getId(),
        entity.getCode(),
        entity.getName(),
        entity.getModule(),
        entity.getDescription());
  }
}
