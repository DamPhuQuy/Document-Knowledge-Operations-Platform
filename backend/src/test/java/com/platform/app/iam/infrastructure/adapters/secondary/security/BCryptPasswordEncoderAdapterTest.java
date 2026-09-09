package com.platform.app.iam.infrastructure.adapters.secondary.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BCryptPasswordEncoderAdapterTest {

  @Test
  @DisplayName("Should encode password and verify matches with BCrypt strength >= 12")
  void shouldEncodeAndVerify() {
    BCryptPasswordEncoderAdapter adapter = new BCryptPasswordEncoderAdapter();
    String raw = "SecurePass#2026";
    String hash = adapter.encode(raw);

    assertNotNull(hash);
    assertTrue(hash.startsWith("$2a$12$") || hash.startsWith("$2b$12$"));
    assertTrue(adapter.matches(raw, hash));
    assertFalse(adapter.matches("WrongPassword", hash));
    assertFalse(adapter.matches(null, hash));
  }
}
