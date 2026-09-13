package com.platform.app.iam.infrastructure.adapters.secondary.security.adapter;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.platform.app.iam.application.ports.outbound.TokenProviderPort;
import com.platform.app.iam.domain.model.User;
import com.platform.app.iam.infrastructure.adapters.secondary.security.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProviderAdapter implements TokenProviderPort {

  private final SecretKey key;
  private final JwtProperties properties;
  private final SecureRandom secureRandom;

  public JwtTokenProviderAdapter(JwtProperties properties) {
    Objects.requireNonNull(properties, "JwtProperties must not be null");
    Objects.requireNonNull(properties.secret(), "JWT secret must not be null");
    // Ensure key satisfies minimum 256 bits (NF2)
    byte[] keyBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length < 32) {
      throw new IllegalArgumentException("JWT secret must be at least 256 bits (32 bytes)");
    }
    this.key = Keys.hmacShaKeyFor(keyBytes);
    this.properties = properties;
    this.secureRandom = new SecureRandom();
  }

  @Override
  public String generateAccessToken(User user) {
    Objects.requireNonNull(user, "User must not be null");
    Instant now = Instant.now();
    Instant expiry = now.plusMillis(properties.expirationMs());

    return Jwts.builder()
        .subject(user.getId().toString())
        .claim("userId", user.getId().toString())
        .claim("email", user.getEmail())
        .claim(
            "departmentId",
            user.getDepartmentId() != null ? user.getDepartmentId().toString() : null)
        .claim(
            "roleIds",
            user.getRoleIds().stream().map(UUID::toString).sorted().toList())
        .claim("isInternal", user.isInternal())
        .claim("permissions", user.getAllPermissionCodes().stream().sorted().toList())
        .claim(Claims.ISSUED_AT, now.getEpochSecond())
        .claim(Claims.EXPIRATION, expiry.getEpochSecond())
        .signWith(key)
        .compact();
  }

  @Override
  public String generateRefreshTokenString() {
    byte[] randomBytes = new byte[64];
    secureRandom.nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
  }

  @Override
  public long getAccessTokenExpirationSeconds() {
    return Duration.ofMillis(properties.expirationMs()).toSeconds();
  }

  public Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public boolean validateToken(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException _) {
      return false;
    }
  }
}
