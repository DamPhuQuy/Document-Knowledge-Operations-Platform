package com.platform.app.iam.domain.model;

import java.util.Objects;
import java.util.UUID;

public record RoleId(UUID value) {
  public RoleId {
    Objects.requireNonNull(value, "Role ID value must not be null");
  }

  public static RoleId generate() {
    return new RoleId(UUID.randomUUID());
  }

  public static RoleId from(UUID uuid) {
    return new RoleId(uuid);
  }

  public static RoleId from(String uuidString) {
    Objects.requireNonNull(uuidString, "UUID string must not be null");
    return new RoleId(UUID.fromString(uuidString));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
