package com.platform.app.shared.util;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public final class UuidUtils {

  private static final Pattern UUID_PATTERN =
      Pattern.compile(
          "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

  private UuidUtils() {}

  public static boolean isValid(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    return UUID_PATTERN.matcher(value.trim()).matches();
  }

  public static UUID parseOrNull(String value) {
    if (value == null
        || value.isBlank()
        || "null".equalsIgnoreCase(value.trim())
        || "undefined".equalsIgnoreCase(value.trim())) {
      return null;
    }
    return UUID.fromString(value.trim());
  }

  public static Optional<UUID> tryParse(String value) {
    try {
      return Optional.ofNullable(parseOrNull(value));
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }
  }
}
