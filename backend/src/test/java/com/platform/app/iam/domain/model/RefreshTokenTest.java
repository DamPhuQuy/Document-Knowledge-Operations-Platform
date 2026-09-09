package com.platform.app.iam.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

  @Test
  @DisplayName("Should detect active vs expired and revoked token state")
  void shouldDetectTokenValidity() {
    UserId userId = UserId.generate();
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
}
