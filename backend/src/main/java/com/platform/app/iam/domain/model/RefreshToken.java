package com.platform.app.iam.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
public class RefreshToken {
  @EqualsAndHashCode.Include
  private final UUID id;
  private final UUID userId;
  private final String token;
  private final Instant expiryDate;
  private boolean revoked;
  private final Instant createdAt;

  public RefreshToken(
      UUID id,
      UUID userId,
      String token,
      Instant expiryDate,
      boolean revoked,
      Instant createdAt) {
    this.id = Objects.requireNonNull(id, "RefreshToken id must not be null");
    this.userId = Objects.requireNonNull(userId, "RefreshToken userId must not be null");
    this.token = Objects.requireNonNull(token, "RefreshToken token must not be null");
    this.expiryDate = Objects.requireNonNull(expiryDate, "RefreshToken expiryDate must not be null");
    this.revoked = revoked;
    this.createdAt = createdAt != null ? createdAt : Instant.now();
  }

  public static RefreshToken create(UUID userId, String token, Instant expiryDate) {
    return new RefreshToken(
        UUID.randomUUID(), userId, token, expiryDate, false, Instant.now());
  }

  public boolean isExpired(Instant now) {
    return expiryDate.isBefore(now);
  }

  public boolean isValid(Instant now) {
    return !revoked && !isExpired(now);
  }

  public void revoke() {
    this.revoked = true;
  }
}
