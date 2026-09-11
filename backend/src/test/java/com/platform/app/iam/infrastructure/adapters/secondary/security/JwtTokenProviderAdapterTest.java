package com.platform.app.iam.infrastructure.adapters.secondary.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.platform.app.iam.domain.model.DepartmentId;
import com.platform.app.iam.domain.model.Permission;
import com.platform.app.iam.domain.model.Role;
import com.platform.app.iam.domain.model.RoleId;
import com.platform.app.iam.domain.model.User;
import com.platform.app.iam.domain.model.UserId;
import com.platform.app.iam.infrastructure.adapters.secondary.security.adapter.JwtTokenProviderAdapter;
import com.platform.app.iam.infrastructure.adapters.secondary.security.config.JwtProperties;

import io.jsonwebtoken.Claims;

class JwtTokenProviderAdapterTest {

  private static final String SECRET_256 =
      "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
  private static final JwtProperties PROPERTIES =
      JwtProperties.builder()
          .secret(SECRET_256)
          .expirationMs(3600000L)
          .refreshExpirationMs(604800000L)
          .build();

  @Test
  @DisplayName("Should generate valid JWT with all required claims")
  void shouldGenerateValidJwtWithClaims() {
    JwtTokenProviderAdapter adapter = new JwtTokenProviderAdapter(PROPERTIES);

    UserId userId = UserId.generate();
    RoleId roleId = RoleId.generate();
    DepartmentId deptId = DepartmentId.from(UUID.randomUUID());
    Permission p1 =
        Permission.builder()
            .id(UUID.randomUUID())
            .code("IAM_READ")
            .name("Read IAM")
            .module("IAM")
            .description("Read permission")
            .build();
    Role role =
        Role.builder()
            .id(roleId)
            .code("ADMIN")
            .name("Administrator")
            .description("Full access")
            .permissions(Set.of(p1))
            .build();

    User user =
        User.builder()
            .id(userId)
            .email("admin@platform.com")
            .passwordHash("hash")
            .fullName("Admin User")
            .departmentId(deptId)
            .flags(com.platform.app.shared.domain.UserFlags.of(true, true))
            .roles(Set.of(role))
            .auditMetadata(com.platform.app.shared.domain.AuditMetadata.now())
            .build();

    String token = adapter.generateAccessToken(user);
    assertNotNull(token);
    assertTrue(adapter.validateToken(token));

    Claims claims = adapter.parseClaims(token);
    assertEquals(userId.value().toString(), claims.getSubject());
    assertEquals(userId.value().toString(), claims.get("userId", String.class));
    assertEquals("admin@platform.com", claims.get("email", String.class));
    assertEquals(deptId.value().toString(), claims.get("departmentId", String.class));
    assertEquals(true, claims.get("isInternal", Boolean.class));

    @SuppressWarnings("unchecked")
    List<String> roleIds = claims.get("roleIds", List.class);
    assertEquals(List.of(roleId.value().toString()), roleIds);

    @SuppressWarnings("unchecked")
    List<String> permissions = claims.get("permissions", List.class);
    assertEquals(List.of("IAM_READ"), permissions);
  }

  @Test
  @DisplayName("Should generate opaque refresh token of 64 bytes entropy")
  void shouldGenerateOpaqueRefreshToken() {
    JwtTokenProviderAdapter adapter = new JwtTokenProviderAdapter(PROPERTIES);
    String token1 = adapter.generateRefreshTokenString();
    String token2 = adapter.generateRefreshTokenString();

    assertNotEquals(token1, token2);
    assertTrue(token1.length() >= 64);
  }

  @Test
  @DisplayName("Should reject expired token")
  void shouldRejectExpiredToken() {
    JwtProperties expiredProperties =
        JwtProperties.builder()
            .secret(SECRET_256)
            .expirationMs(-1000L)
            .refreshExpirationMs(604800000L)
            .build();
    JwtTokenProviderAdapter adapter = new JwtTokenProviderAdapter(expiredProperties);

    User user =
        User.builder()
            .id(UserId.generate())
            .email("expired@platform.com")
            .passwordHash("hash")
            .fullName("Expired User")
            .flags(com.platform.app.shared.domain.UserFlags.of(true, true))
            .roles(Set.of())
            .auditMetadata(com.platform.app.shared.domain.AuditMetadata.now())
            .build();

    String token = adapter.generateAccessToken(user);
    assertFalse(adapter.validateToken(token));
  }
}
