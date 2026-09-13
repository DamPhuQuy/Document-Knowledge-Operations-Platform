package com.platform.app.iam.infrastructure.adapters.secondary.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class BCryptPasswordEncoderAdapterTest {

  @Test
  @DisplayName("Should encode password and verify matches with BCrypt strength >= 12")
  void shouldEncodeAndVerify() {
    PasswordEncoder encoder = new BCryptPasswordEncoder(12);
    String raw = "SecurePass#2026";
    String hash = encoder.encode(raw);

    assertNotNull(hash);
    assertTrue(hash.startsWith("$2a$12$") || hash.startsWith("$2b$12$"));
    assertTrue(encoder.matches(raw, hash));
    assertFalse(encoder.matches("WrongPassword", hash));
    assertFalse(encoder.matches(null, hash));
  }
}
