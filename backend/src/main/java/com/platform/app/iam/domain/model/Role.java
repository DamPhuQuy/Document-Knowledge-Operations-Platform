package com.platform.app.iam.domain.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Role {
  private final RoleId id;
  private final String code;
  private final String name;
  private final String description;
  private final Set<Permission> permissions;

  public Role(RoleId id, String code, String name, String description, Set<Permission> permissions) {
    this.id = Objects.requireNonNull(id, "Role id must not be null");
    this.code = Objects.requireNonNull(code, "Role code must not be null").toUpperCase();
    this.name = Objects.requireNonNull(name, "Role name must not be null");
    this.description = description;
    this.permissions = permissions != null ? new HashSet<>(permissions) : new HashSet<>();
  }

  public RoleId getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Set<Permission> getPermissions() {
    return Collections.unmodifiableSet(permissions);
  }

  public Set<String> getPermissionCodes() {
    return permissions.stream().map(Permission::getCode).collect(Collectors.toSet());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Role role = (Role) o;
    return Objects.equals(id, role.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "Role{" + "id=" + id + ", code='" + code + '\'' + '}';
  }
}
