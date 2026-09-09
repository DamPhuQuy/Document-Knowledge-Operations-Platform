package com.platform.app.iam.domain.model;

import java.util.Objects;
import java.util.UUID;

public record DepartmentId(UUID value) {
  public DepartmentId {
    Objects.requireNonNull(value, "Department ID value must not be null");
  }

  public static DepartmentId from(UUID uuid) {
    return uuid == null ? null : new DepartmentId(uuid);
  }

  public static DepartmentId from(String uuidString) {
    if (uuidString == null || uuidString.isBlank()) {
      return null;
    }
    return new DepartmentId(UUID.fromString(uuidString));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
