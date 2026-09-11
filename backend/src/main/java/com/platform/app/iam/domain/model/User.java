package com.platform.app.iam.domain.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.platform.app.shared.domain.AuditMetadata;
import com.platform.app.shared.domain.UserFlags;

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
  private final UserId id;

  @ToString.Include
  private final String email;
  private final String passwordHash;
  private final String fullName;
  private final DepartmentId departmentId;
  private final UserFlags flags;

  @Getter(AccessLevel.NONE)
  private final Set<Role> roles;
  private final AuditMetadata auditMetadata;

  private User(
      UserId id,
      String email,
      String passwordHash,
      String fullName,
      DepartmentId departmentId,
      UserFlags flags,
      Set<Role> roles,
      AuditMetadata auditMetadata) {
    this.id = Objects.requireNonNull(id, "User id must not be null");
    this.email = Objects.requireNonNull(email, "User email must not be null").toLowerCase();
    this.passwordHash = Objects.requireNonNull(passwordHash, "User passwordHash must not be null");
    this.fullName = Objects.requireNonNull(fullName, "User fullName must not be null");
    this.departmentId = departmentId;
    this.flags = flags != null ? flags : UserFlags.of(true, false);
    this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
    this.auditMetadata = auditMetadata != null ? auditMetadata : AuditMetadata.now();
  }

  public boolean isEnabled() {
    return flags != null && flags.enabled();
  }

  public boolean isInternal() {
    return flags != null && flags.isInternal();
  }

  public java.time.Instant getCreatedAt() {
    return auditMetadata != null ? auditMetadata.createdAt() : null;
  }

  public java.time.Instant getUpdatedAt() {
    return auditMetadata != null ? auditMetadata.updatedAt() : null;
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
}
