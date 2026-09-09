package com.platform.app.iam.domain.model;

import java.util.Objects;
import java.util.UUID;

public class Permission {
  private final UUID id;
  private final String code;
  private final String name;
  private final String module;
  private final String description;

  public Permission(UUID id, String code, String name, String module, String description) {
    this.id = Objects.requireNonNull(id, "Permission id must not be null");
    this.code = Objects.requireNonNull(code, "Permission code must not be null").toUpperCase();
    this.name = Objects.requireNonNull(name, "Permission name must not be null");
    this.module = module;
    this.description = description;
  }

  public UUID getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getModule() {
    return module;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Permission that = (Permission) o;
    return Objects.equals(code, that.code);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code);
  }

  @Override
  public String toString() {
    return "Permission{" + "code='" + code + '\'' + ", name='" + name + '\'' + '}';
  }
}
