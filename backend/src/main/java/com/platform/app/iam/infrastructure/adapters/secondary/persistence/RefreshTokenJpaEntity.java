package com.platform.app.iam.infrastructure.adapters.secondary.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenJpaEntity {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(nullable = false, unique = true, length = 512)
  private String token;

  @Column(name = "expiry_date", nullable = false)
  private Instant expiryDate;

  @Column(nullable = false)
  private boolean revoked;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public RefreshTokenJpaEntity() {}

  public RefreshTokenJpaEntity(
      UUID id, UUID userId, String token, Instant expiryDate, boolean revoked, Instant createdAt) {
    this.id = id;
    this.userId = userId;
    this.token = token;
    this.expiryDate = expiryDate;
    this.revoked = revoked;
    this.createdAt = createdAt != null ? createdAt : Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public Instant getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(Instant expiryDate) {
    this.expiryDate = expiryDate;
  }

  public boolean isRevoked() {
    return revoked;
  }

  public void setRevoked(boolean revoked) {
    this.revoked = revoked;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RefreshTokenJpaEntity that = (RefreshTokenJpaEntity) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
