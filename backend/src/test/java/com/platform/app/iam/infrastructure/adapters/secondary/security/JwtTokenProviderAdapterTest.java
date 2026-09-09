package com.platform.app.iam.infrastructure.adapters.secondary.security;

import static org.junit.jupiter.api.Assertions.*;

import com.platform.app.iam.domain.model.DepartmentId;
import com.platform.app.iam.domain.model.Permission;
import com.platform.app.iam.domain.model.Role;
import com.platform.app.iam.domain.model.RoleId;
import com.platform.app.iam.domain.model.User;
import com.platform.app.iam.domain.model.UserId;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderAdapterTest {

  private static final String SECRET_256 =
      "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

  @Test
  @DisplayName("Should generate valid JWT with all required claims")
  void shouldGenerateValidJwtWithClaims() {
    JwtTokenProviderAdapter adapter = new JwtTokenProviderAdapter(SECRET_256, 3600000L);

    UserId userId = UserId.generate();
    RoleId roleId = RoleId.generate();
    DepartmentId deptId = DepartmentId.from(UUID.randomUUID());
    Permission p1 = new Permission(UUID.randomUUID(), "IAM_READ", "Read IAM", "IAM", "Read permission");
    Role role = new Role(roleId, "ADMIN", "Administrator", "Full access", Set.of(p1));

    User user =
        new User(
            userId,
            "admin@platform.com",
            "hash",
            "Admin User",
            deptId,
            true,
            true,
            Set.of(role),
            Instant.now(),
            Instant.now());

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
    JwtTokenProviderAdapter adapter = new JwtTokenProviderAdapter(SECRET_256, 3600000L);
    String token1 = adapter.generateRefreshTokenString();
    String token2 = adapter.generateRefreshTokenString();

    assertNotNull(token1);
    assertNotNull(token2);
    assertNotEquals(token1, token2);
    assertTrue(token1.length() >= 64);
  }
}
