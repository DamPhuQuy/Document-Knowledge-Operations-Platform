package com.platform.app.iam.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import com.platform.app.iam.domain.exception.InvalidDepartmentCodeException;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
public class Department {

  private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9_]+$");

  @ToString.Include
  @EqualsAndHashCode.Include
  private final UUID id;

  @ToString.Include
  private String code;

  @ToString.Include
  private String name;

  private String description;

  private final Instant createdAt;
  private Instant updatedAt;

  public Department(UUID id, String code, String name, String description, Instant createdAt, Instant updatedAt) {
    this.id = Objects.requireNonNull(id, "Department id must not be null");
    this.code = validateCode(code);
    this.name = validateName(name);
    this.description = description;
    this.createdAt = createdAt != null ? createdAt : Instant.now();
    this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
  }

  public void updateDetails(String newCode, String newName, String newDescription) {
    this.code = validateCode(newCode);
    this.name = validateName(newName);
    this.description = newDescription;
    this.updatedAt = Instant.now();
  }

  public static String validateCode(String code) {
    if (code == null || code.isBlank()) {
      throw new InvalidDepartmentCodeException("Department code must not be null or blank");
    }
    String trimmed = code.trim();
    if (!CODE_PATTERN.matcher(trimmed).matches()) {
      throw new InvalidDepartmentCodeException(
          "Department code must be uppercase alphanumeric (e.g., HR, FIN, IT, LEGAL)");
    }
    return trimmed;
  }

  private static String validateName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Department name must not be null or blank");
    }
    return name.trim();
  }
}
