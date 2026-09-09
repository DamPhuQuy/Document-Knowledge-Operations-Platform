package com.platform.app.iam.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class RefreshToken {
  private final UUID id;
  private final UserId userId;
  private final String token;
  private final Instant expiryDate;
  private boolean revoked;
  private final Instant createdAt;

  public RefreshToken(
      UUID id,
      UserId userId,
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

  public static RefreshToken create(UserId userId, String token, Instant expiryDate) {
    return new RefreshToken(
        UUID.randomUUID(), userId, token, expiryDate, false, Instant.now());
  }

  public UUID getId() {
    return id;
  }

  public UserId getUserId() {
    return userId;
  }

  public String getToken() {
    return token;
  }

  public Instant getExpiryDate() {
    return expiryDate;
  }

  public boolean isRevoked() {
    return revoked;
  }

  public Instant getCreatedAt() {
    return createdAt;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RefreshToken that = (RefreshToken) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
