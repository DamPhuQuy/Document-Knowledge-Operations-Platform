package com.platform.app.iam.infrastructure.adapters.secondary.security.adapter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import com.platform.app.iam.application.ports.outbound.AccountLockoutPort;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class InMemoryAccountLockoutAdapter implements AccountLockoutPort {

  private static final int MAX_FAILED_ATTEMPTS = 5;
  private static final Duration WINDOW_DURATION = Duration.ofMinutes(15);
  private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

  private final Clock clock;
  private final ConcurrentMap<String, AttemptRecord> attemptCache;

  public InMemoryAccountLockoutAdapter() {
    this(Clock.systemUTC());
  }

  public InMemoryAccountLockoutAdapter(Clock clock) {
    this.clock = Objects.requireNonNull(clock, "Clock must not be null");
    this.attemptCache = new ConcurrentHashMap<>();
  }

  @Override
  public boolean isLocked(String email) {
    if (email == null) {
      return false;
    }
    String normalized = email.trim().toLowerCase();
    AttemptRecord record = attemptCache.get(normalized);
    if (record == null) {
      return false;
    }

    Instant now = Instant.now(clock);
    if (record.lockedUntil() != null) {
      if (now.isBefore(record.lockedUntil())) {
        log.warn("Account [{}] is currently locked until {}", normalized, record.lockedUntil());
        return true;
      }
      // Lockout duration expired -> automatically clear
      attemptCache.remove(normalized);
      log.info("Lockout expired for account [{}]; reset to unlocked", normalized);
      return false;
    }
    return false;
  }

  @Override
  public void recordFailure(String email) {
    if (email == null) {
      return;
    }
    String normalized = email.trim().toLowerCase();
    Instant now = Instant.now(clock);

    attemptCache.compute(
        normalized,
        (key, current) -> {
          if (current == null) {
            return new AttemptRecord(1, now, null);
          }

          // If previously locked and lock expired, reset
          if (current.lockedUntil() != null && !now.isBefore(current.lockedUntil())) {
            return new AttemptRecord(1, now, null);
          }

          // If window has passed since first failed attempt, start fresh window
          if (Duration.between(current.firstFailedAt(), now).compareTo(WINDOW_DURATION) > 0) {
            return new AttemptRecord(1, now, null);
          }

          int newCount = current.failedCount() + 1;
          Instant lockedUntil = null;
          if (newCount >= MAX_FAILED_ATTEMPTS) {
            lockedUntil = now.plus(LOCKOUT_DURATION);
          }
          return new AttemptRecord(newCount, current.firstFailedAt(), lockedUntil);
        });

    AttemptRecord updated = attemptCache.get(normalized);
    if (updated != null && updated.lockedUntil() != null) {
      log.warn(
          "Account [{}] reached {} failed login attempts; locked for {} minutes until {}",
          normalized,
          updated.failedCount(),
          LOCKOUT_DURATION.toMinutes(),
          updated.lockedUntil());
    } else if (updated != null) {
      log.debug(
          "Recorded failed login attempt {}/{} for account [{}]",
          updated.failedCount(),
          MAX_FAILED_ATTEMPTS,
          normalized);
    }
  }

  @Override
  public void resetAttempts(String email) {
    if (email != null) {
      String normalized = email.trim().toLowerCase();
      if (attemptCache.remove(normalized) != null) {
        log.debug("Reset failed login attempts for account [{}]", normalized);
      }
    }
  }

  private record AttemptRecord(int failedCount, Instant firstFailedAt, Instant lockedUntil) {}
}
