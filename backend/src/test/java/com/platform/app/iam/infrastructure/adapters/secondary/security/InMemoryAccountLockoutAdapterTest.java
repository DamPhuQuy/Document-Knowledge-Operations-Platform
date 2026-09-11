package com.platform.app.iam.infrastructure.adapters.secondary.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.platform.app.iam.infrastructure.adapters.secondary.security.adapter.InMemoryAccountLockoutAdapter;

class InMemoryAccountLockoutAdapterTest {

  @Test
  @DisplayName("Should lock account after 5 failed attempts and unlock after 15 minutes")
  void shouldLockAndUnlockCorrectly() {
    MutableClock clock = new MutableClock(Instant.parse("2026-09-09T10:00:00Z"));
    InMemoryAccountLockoutAdapter adapter = new InMemoryAccountLockoutAdapter(clock);
    String email = "target@platform.com";

    // 1 to 4 failed attempts: not locked
    for (int i = 1; i <= 4; i++) {
      adapter.recordFailure(email);
      assertFalse(adapter.isLocked(email), "Should not be locked at attempt " + i);
    }

    // 5th failed attempt: locked
    adapter.recordFailure(email);
    assertTrue(adapter.isLocked(email), "Should be locked at 5th attempt");

    // Advance 14 minutes: still locked
    clock.advance(Duration.ofMinutes(14));
    assertTrue(adapter.isLocked(email), "Should remain locked after 14 minutes");

    // Advance past 15 minutes: unlocked
    clock.advance(Duration.ofMinutes(2)); // total 16 minutes
    assertFalse(adapter.isLocked(email), "Should unlock after 15 minutes");
  }

  @Test
  @DisplayName("Should reset lockout counter when resetAttempts is called")
  void shouldResetCounter() {
    InMemoryAccountLockoutAdapter adapter = new InMemoryAccountLockoutAdapter();
    String email = "reset@platform.com";

    for (int i = 0; i < 5; i++) {
      adapter.recordFailure(email);
    }
    assertTrue(adapter.isLocked(email));

    adapter.resetAttempts(email);
    assertFalse(adapter.isLocked(email));
  }

  private static class MutableClock extends Clock {
    private Instant currentInstant;

    MutableClock(Instant initial) {
      this.currentInstant = initial;
    }

    void advance(Duration duration) {
      this.currentInstant = this.currentInstant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return currentInstant;
    }
  }
}
