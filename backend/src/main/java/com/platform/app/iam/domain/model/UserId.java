package com.platform.app.iam.domain.model;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {
  public UserId {
    Objects.requireNonNull(value, "User ID value must not be null");
  }

  public static UserId generate() {
    return new UserId(UUID.randomUUID());
  }

  public static UserId from(UUID uuid) {
    return new UserId(uuid);
  }

  public static UserId from(String uuidString) {
    Objects.requireNonNull(uuidString, "UUID string must not be null");
    return new UserId(UUID.fromString(uuidString));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
