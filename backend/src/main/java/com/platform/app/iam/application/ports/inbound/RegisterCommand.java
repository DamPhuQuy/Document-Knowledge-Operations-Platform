package com.platform.app.iam.application.ports.inbound;

import java.util.Objects;
import lombok.Builder;

@Builder
public record RegisterCommand(
    String email,
    String password,
    String firstName,
    String lastName
) {
  public RegisterCommand {
    Objects.requireNonNull(email, "Email must not be null");
    Objects.requireNonNull(password, "Password must not be null");
    Objects.requireNonNull(firstName, "First name must not be null");
  }

  public String getFullName() {
    if (lastName == null || lastName.isBlank()) {
      return firstName.trim();
    }
    return firstName.trim() + " " + lastName.trim();
  }
}
