package com.platform.app.iam.domain.model;

import java.util.Objects;
import java.util.UUID;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
public class Permission {
  private final UUID id;

  @ToString.Include
  @EqualsAndHashCode.Include
  private final String code;

  @ToString.Include
  private final String name;

  @ToString.Include
  private final String module;
  private final String description;

  public Permission(UUID id, String code, String name, String module, String description) {
    this.id = Objects.requireNonNull(id, "Permission id must not be null");
    this.code = Objects.requireNonNull(code, "Permission code must not be null").toUpperCase();
    this.name = Objects.requireNonNull(name, "Permission name must not be null");
    this.module = module;
    this.description = description;
  }
}
