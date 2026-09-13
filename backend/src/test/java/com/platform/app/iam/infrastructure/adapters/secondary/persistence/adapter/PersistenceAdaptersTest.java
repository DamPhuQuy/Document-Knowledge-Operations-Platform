package com.platform.app.iam.infrastructure.adapters.secondary.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import com.platform.app.iam.domain.model.RefreshToken;
import com.platform.app.iam.domain.model.User;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.PermissionJpaEntity;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.RoleJpaEntity;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.UserJpaEntity;

import jakarta.persistence.EntityManager;

@DataJpaTest
@Import({
  UserRepositoryAdapter.class,
  RefreshTokenRepositoryAdapter.class,
  RoleRepositoryAdapter.class
})
class PersistenceAdaptersTest {

  @Autowired private EntityManager entityManager;
  @Autowired private UserRepositoryAdapter userRepositoryAdapter;
  @Autowired private RefreshTokenRepositoryAdapter refreshTokenRepositoryAdapter;
  @Autowired private RoleRepositoryAdapter roleRepositoryAdapter;

  @Test
  @DisplayName("Should persist and find user with eager roles and permissions")
  void shouldFindUserByEmailWithRolesAndPermissions() {
    UUID permissionId = UUID.randomUUID();
    PermissionJpaEntity perm =
        PermissionJpaEntity.builder()
            .id(permissionId)
            .code("DOC_READ")
            .name("Read Documents")
            .module("DOC")
            .description("Read permission")
            .createdAt(Instant.now())
            .build();
    entityManager.persist(perm);

    UUID roleId = UUID.randomUUID();
    RoleJpaEntity role =
        RoleJpaEntity.builder()
            .id(roleId)
            .code("ADMIN")
            .name("Administrator")
            .description("Admin role")
            .createdAt(Instant.now())
            .permissions(Set.of(perm))
            .build();
    entityManager.persist(role);

    UUID userId = UUID.randomUUID();
    UserJpaEntity user =
        UserJpaEntity.builder()
            .id(userId)
            .email("test@platform.com")
            .passwordHash("$2a$12$somehashvaluehere")
            .fullName("John Doe")
            .departmentId(null)
            .enabled(true)
            .isInternal(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .roles(Set.of(role))
            .build();
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
    UUID userId = UUID.randomUUID();
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

  @Test
  @DisplayName("Should find user by id and update user roles")
  void shouldFindByIdAndUpdateRoles() {
    UUID role1Id = UUID.randomUUID();
    RoleJpaEntity role1 =
        RoleJpaEntity.builder()
            .id(role1Id)
            .code("STAFF")
            .name("Staff")
            .createdAt(Instant.now())
            .build();
    entityManager.persist(role1);

    UUID role2Id = UUID.randomUUID();
    RoleJpaEntity role2 =
        RoleJpaEntity.builder()
            .id(role2Id)
            .code("MANAGER")
            .name("Manager")
            .createdAt(Instant.now())
            .build();
    entityManager.persist(role2);

    UUID userId = UUID.randomUUID();
    UserJpaEntity user =
        UserJpaEntity.builder()
            .id(userId)
            .email("persist-roles@platform.com")
            .passwordHash("hash")
            .fullName("Persist User")
            .enabled(true)
            .isInternal(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .roles(Set.of(role1))
            .build();
    entityManager.persist(user);
    entityManager.flush();
    entityManager.clear();

    // Verify findById
    Optional<User> found = userRepositoryAdapter.findById(userId);
    assertTrue(found.isPresent());
    assertEquals(1, found.get().getRoles().size());

    // Update user with both role1 and role2
    com.platform.app.iam.domain.model.Role dRole1 =
        com.platform.app.iam.domain.model.Role.builder()
            .id(role1Id)
            .code("STAFF")
            .name("Staff")
            .build();
    com.platform.app.iam.domain.model.Role dRole2 =
        com.platform.app.iam.domain.model.Role.builder()
            .id(role2Id)
            .code("MANAGER")
            .name("Manager")
            .build();

    User domainUser = found.get();
    domainUser.assignRoles(Set.of(dRole1, dRole2), UUID.randomUUID());

    userRepositoryAdapter.save(domainUser);
    entityManager.flush();
    entityManager.clear();

    Optional<User> updated = userRepositoryAdapter.findById(userId);
    assertTrue(updated.isPresent());
    assertEquals(2, updated.get().getRoles().size());
  }

  @Test
  @DisplayName("Should find roles by ids, code, and find all")
  void shouldQueryRolesViaAdapter() {
    UUID permId = UUID.randomUUID();
    PermissionJpaEntity perm =
        PermissionJpaEntity.builder()
            .id(permId)
            .code("USER_MANAGE")
            .name("Manage Users")
            .module("IAM")
            .createdAt(Instant.now())
            .build();
    entityManager.persist(perm);

    UUID roleId = UUID.randomUUID();
    RoleJpaEntity role =
        RoleJpaEntity.builder()
            .id(roleId)
            .code("SYSTEM_ADMIN")
            .name("System Admin")
            .createdAt(Instant.now())
            .permissions(Set.of(perm))
            .build();
    entityManager.persist(role);
    entityManager.flush();
    entityManager.clear();

    Set<com.platform.app.iam.domain.model.Role> byIds =
        roleRepositoryAdapter.findByIds(Set.of(roleId));
    assertEquals(1, byIds.size());
    assertEquals("SYSTEM_ADMIN", byIds.iterator().next().getCode());

    Optional<com.platform.app.iam.domain.model.Role> byCode =
        roleRepositoryAdapter.findByCode("system_admin");
    assertTrue(byCode.isPresent());
    assertEquals(roleId, byCode.get().getId());

    var all = roleRepositoryAdapter.findAll();
    assertFalse(all.isEmpty());
  }
}
