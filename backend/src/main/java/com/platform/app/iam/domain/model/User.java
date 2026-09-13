package com.platform.app.iam.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
public class User {
  @ToString.Include
  @EqualsAndHashCode.Include
  private final UUID id;

  @ToString.Include
  private final String email;
  private final String passwordHash;
  private final String fullName;
  private final UUID departmentId;
  private final boolean enabled;
  private final boolean internal;

  @Getter(AccessLevel.NONE)
  private final Set<Role> roles;
  private final Instant createdAt;
  private final Instant updatedAt;

  public User(
      UUID id,
      String email,
      String passwordHash,
      String fullName,
      UUID departmentId,
      boolean enabled,
      boolean internal,
      Set<Role> roles,
      Instant createdAt,
      Instant updatedAt) {
    this.id = Objects.requireNonNull(id, "User id must not be null");
    this.email = Objects.requireNonNull(email, "User email must not be null").toLowerCase();
    this.passwordHash = Objects.requireNonNull(passwordHash, "User passwordHash must not be null");
    this.fullName = Objects.requireNonNull(fullName, "User fullName must not be null");
    this.departmentId = departmentId;
    this.enabled = enabled;
    this.internal = internal;
    this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
    this.createdAt = createdAt != null ? createdAt : Instant.now();
    this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
  }

  public Set<Role> getRoles() {
    return Collections.unmodifiableSet(roles);
  }

  public Set<UUID> getRoleIds() {
    return roles.stream().map(Role::getId).collect(Collectors.toSet());
  }

  public Set<String> getRoleCodes() {
    return roles.stream().map(Role::getCode).collect(Collectors.toSet());
  }

  public Set<String> getAllPermissionCodes() {
    return roles.stream()
        .flatMap(role -> role.getPermissionCodes().stream())
        .collect(Collectors.toSet());
  }
}
