package com.platform.app.iam.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

  @Test
  @DisplayName("Should detect active vs expired and revoked token state")
  void shouldDetectTokenValidity() {
    UUID userId = UUID.randomUUID();
    Instant now = Instant.now();
    Instant future = now.plus(30, ChronoUnit.DAYS);
    Instant past = now.minus(1, ChronoUnit.DAYS);

    RefreshToken validToken = RefreshToken.create(userId, "valid-token-str", future);
    assertTrue(validToken.isValid(now));
    assertFalse(validToken.isExpired(now));

    validToken.revoke();
    assertTrue(validToken.isRevoked());
    assertFalse(validToken.isValid(now));

    RefreshToken expiredToken = RefreshToken.create(userId, "expired-token-str", past);
    assertTrue(expiredToken.isExpired(now));
    assertFalse(expiredToken.isValid(now));
  }

  @Test
  @DisplayName("Should correctly construct refresh token using builder")
  void shouldConstructUsingBuilder() {
    UUID userId = UUID.randomUUID();
    UUID tokenId = UUID.randomUUID();
    Instant expiry = Instant.now().plus(7, ChronoUnit.DAYS);
    Instant created = Instant.now();

    RefreshToken token =
        RefreshToken.builder()
            .id(tokenId)
            .userId(userId)
            .token("builder-token")
            .expiryDate(expiry)
            .revoked(false)
            .createdAt(created)
            .build();

    assertEquals(tokenId, token.getId());
    assertEquals(userId, token.getUserId());
    assertEquals("builder-token", token.getToken());
    assertEquals(expiry, token.getExpiryDate());
    assertFalse(token.isRevoked());
    assertEquals(created, token.getCreatedAt());
  }
}
