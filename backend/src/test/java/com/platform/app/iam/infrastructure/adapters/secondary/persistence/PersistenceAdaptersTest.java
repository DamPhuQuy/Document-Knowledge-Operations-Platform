package com.platform.app.iam.infrastructure.adapters.secondary.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.platform.app.iam.domain.model.RefreshToken;
import com.platform.app.iam.domain.model.User;
import com.platform.app.iam.domain.model.UserId;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({UserRepositoryAdapter.class, RefreshTokenRepositoryAdapter.class})
class PersistenceAdaptersTest {

  @Autowired private EntityManager entityManager;
  @Autowired private UserRepositoryAdapter userRepositoryAdapter;
  @Autowired private RefreshTokenRepositoryAdapter refreshTokenRepositoryAdapter;

  @Test
  @DisplayName("Should persist and find user with eager roles and permissions")
  void shouldFindUserByEmailWithRolesAndPermissions() {
    UUID permissionId = UUID.randomUUID();
    PermissionJpaEntity perm =
        new PermissionJpaEntity(
            permissionId, "DOC_READ", "Read Documents", "DOC", "Read permission", Instant.now());
    entityManager.persist(perm);

    UUID roleId = UUID.randomUUID();
    RoleJpaEntity role =
        new RoleJpaEntity(
            roleId, "ADMIN", "Administrator", "Admin role", Instant.now(), Set.of(perm));
    entityManager.persist(role);

    UUID userId = UUID.randomUUID();
    UserJpaEntity user =
        new UserJpaEntity(
            userId,
            "test@platform.com",
            "$2a$12$somehashvaluehere",
            "John Doe",
            null,
            true,
            true,
            Instant.now(),
            Instant.now(),
            Set.of(role));
    entityManager.persist(user);
    entityManager.flush();
    entityManager.clear();

    Optional<User> found = userRepositoryAdapter.findByEmail("TEST@PLATFORM.COM");
    assertTrue(found.isPresent());
    User domainUser = found.get();
    assertEquals("test@platform.com", domainUser.getEmail());
    assertTrue(domainUser.isEnabled());
    assertEquals(1, domainUser.getRoles().size());
    assertEquals(Set.of("DOC_READ"), domainUser.getAllPermissionCodes());
  }

  @Test
  @DisplayName("Should save and find refresh token")
  void shouldSaveAndFindRefreshToken() {
    UserId userId = UserId.generate();
    Instant expiry = Instant.now().plus(30, ChronoUnit.DAYS);
    RefreshToken token = RefreshToken.create(userId, "sample-token-string-12345", expiry);

    RefreshToken saved = refreshTokenRepositoryAdapter.save(token);
    assertNotNull(saved);
    assertEquals(token.getId(), saved.getId());

    Optional<RefreshToken> found =
        refreshTokenRepositoryAdapter.findByToken("sample-token-string-12345");
    assertTrue(found.isPresent());
    assertEquals(userId, found.get().getUserId());
    assertFalse(found.get().isRevoked());
  }
}
