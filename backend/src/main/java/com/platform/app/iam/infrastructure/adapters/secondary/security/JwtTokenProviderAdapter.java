package com.platform.app.iam.infrastructure.adapters.secondary.security;

import com.platform.app.iam.application.ports.outbound.TokenProviderPort;
import com.platform.app.iam.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProviderAdapter implements TokenProviderPort {

  private final SecretKey key;
  private final long expirationMs;
  private final SecureRandom secureRandom;

  public JwtTokenProviderAdapter(
      @Value("${app.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
          String secret,
      @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
    Objects.requireNonNull(secret, "JWT secret must not be null");
    // Ensure key satisfies minimum 256 bits (NF2)
    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length < 32) {
      throw new IllegalArgumentException("JWT secret must be at least 256 bits (32 bytes)");
    }
    this.key = Keys.hmacShaKeyFor(keyBytes);
    this.expirationMs = expirationMs;
    this.secureRandom = new SecureRandom();
  }

  @Override
  public String generateAccessToken(User user) {
    Objects.requireNonNull(user, "User must not be null");
    Instant now = Instant.now();
    Instant expiry = now.plusMillis(expirationMs);

    return Jwts.builder()
        .subject(user.getId().value().toString())
        .claim("userId", user.getId().value().toString())
        .claim("email", user.getEmail())
        .claim(
            "departmentId",
            user.getDepartmentId() != null ? user.getDepartmentId().value().toString() : null)
        .claim(
            "roleIds",
            user.getRoleIds().stream().map(UUID::toString).sorted().toList())
        .claim("isInternal", user.isInternal())
        .claim("permissions", user.getAllPermissionCodes().stream().sorted().toList())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiry))
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
    return expirationMs / 1000;
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
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }
}
