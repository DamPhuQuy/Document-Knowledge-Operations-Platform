package com.platform.app.iam.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class User {
  private final UserId id;
  private final String email;
  private final String passwordHash;
  private final String fullName;
  private final DepartmentId departmentId;
  private final boolean enabled;
  private final boolean isInternal;
  private final Set<Role> roles;
  private final Instant createdAt;
  private final Instant updatedAt;

  public User(
      UserId id,
      String email,
      String passwordHash,
      String fullName,
      DepartmentId departmentId,
      boolean enabled,
      boolean isInternal,
      Set<Role> roles,
      Instant createdAt,
      Instant updatedAt) {
    this.id = Objects.requireNonNull(id, "User id must not be null");
    this.email = Objects.requireNonNull(email, "User email must not be null").toLowerCase();
    this.passwordHash = Objects.requireNonNull(passwordHash, "User passwordHash must not be null");
    this.fullName = Objects.requireNonNull(fullName, "User fullName must not be null");
    this.departmentId = departmentId;
    this.enabled = enabled;
    this.isInternal = isInternal;
    this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
    this.createdAt = createdAt != null ? createdAt : Instant.now();
    this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
  }

  public UserId getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getFullName() {
    return fullName;
  }

  public DepartmentId getDepartmentId() {
    return departmentId;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isInternal() {
    return isInternal;
  }

  public Set<Role> getRoles() {
    return Collections.unmodifiableSet(roles);
  }

  public Set<UUID> getRoleIds() {
    return roles.stream().map(role -> role.getId().value()).collect(Collectors.toSet());
  }

  public Set<String> getRoleCodes() {
    return roles.stream().map(Role::getCode).collect(Collectors.toSet());
  }

  public Set<String> getAllPermissionCodes() {
    return roles.stream()
        .flatMap(role -> role.getPermissionCodes().stream())
        .collect(Collectors.toSet());
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    User user = (User) o;
    return Objects.equals(id, user.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "User{" + "id=" + id + ", email='" + email + '\'' + ", enabled=" + enabled + '}';
  }
}
