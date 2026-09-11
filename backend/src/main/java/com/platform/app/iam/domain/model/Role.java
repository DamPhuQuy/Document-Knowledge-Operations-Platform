package com.platform.app.iam.domain.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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
public class Role {
  @ToString.Include
  @EqualsAndHashCode.Include
  private final RoleId id;

  @ToString.Include
  private final String code;

  @ToString.Include
  private final String name;

  private final String description;

  @Getter(AccessLevel.NONE)
  private final Set<Permission> permissions;

  public Role(RoleId id, String code, String name, String description, Set<Permission> permissions) {
    this.id = Objects.requireNonNull(id, "Role id must not be null");
    this.code = Objects.requireNonNull(code, "Role code must not be null").toUpperCase();
    this.name = Objects.requireNonNull(name, "Role name must not be null");
    this.description = description;
    this.permissions = permissions != null ? new HashSet<>(permissions) : new HashSet<>();
  }

  public Set<Permission> getPermissions() {
    return Collections.unmodifiableSet(permissions);
  }

  public Set<String> getPermissionCodes() {
    return permissions.stream().map(Permission::getCode).collect(Collectors.toSet());
  }
}
