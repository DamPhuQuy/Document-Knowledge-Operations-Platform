package com.platform.app.iam.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  @DisplayName("Should correctly assemble user with roles and permissions")
  void shouldAssembleUserWithRolesAndPermissions() {
    UUID userId = UUID.randomUUID();
    UUID roleId = UUID.randomUUID();
    UUID deptId = UUID.randomUUID();

    Permission p1 =
        Permission.builder()
            .id(UUID.randomUUID())
            .code("DOC_READ")
            .name("Read Documents")
            .module("DOC")
            .description("Can read documents")
            .build();
    Permission p2 =
        Permission.builder()
            .id(UUID.randomUUID())
            .code("DOC_WRITE")
            .name("Write Documents")
            .module("DOC")
            .description("Can write documents")
            .build();
    Role role =
        Role.builder()
            .id(roleId)
            .code("DOCUMENT_MANAGER")
            .name("Document Manager")
            .description("Manages documents")
            .permissions(Set.of(p1, p2))
            .build();

    User user =
        User.builder()
            .id(userId)
            .email("User@Platform.COM")
            .passwordHash("$2a$12$e88...hashed")
            .fullName("Test User")
            .departmentId(deptId)
            .enabled(true)
            .internal(true)
            .roles(Set.of(role))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    assertEquals("user@platform.com", user.getEmail());
    assertTrue(user.isEnabled());
    assertTrue(user.isInternal());
    assertEquals(1, user.getRoleIds().size());
    assertTrue(user.getRoleIds().contains(roleId));
    assertEquals(Set.of("DOC_READ", "DOC_WRITE"), user.getAllPermissionCodes());
  }

  @Test
  @DisplayName("Should detect disabled user state")
  void shouldDetectDisabledUser() {
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .email("inactive@platform.com")
            .passwordHash("hash")
            .fullName("Inactive User")
            .enabled(false)
            .internal(false)
            .roles(Set.of())
            .build();

    assertFalse(user.isEnabled());
  }

  @Test
  @DisplayName("Should successfully assign new roles to user")
  void shouldAssignNewRoles() {
    UUID userId = UUID.randomUUID();
    Role role1 = Role.builder().id(UUID.randomUUID()).code("ROLE_STAFF").name("Staff").build();
    Role role2 = Role.builder().id(UUID.randomUUID()).code("ROLE_MANAGER").name("Manager").build();

    User user =
        User.builder()
            .id(userId)
            .email("staff@platform.com")
            .passwordHash("hash")
            .fullName("Staff User")
            .enabled(true)
            .internal(true)
            .roles(Set.of(role1))
            .build();

    user.assignRoles(Set.of(role1, role2), UUID.randomUUID());

    assertEquals(2, user.getRoles().size());
    assertTrue(user.getRoleCodes().contains("ROLE_STAFF"));
    assertTrue(user.getRoleCodes().contains("ROLE_MANAGER"));
  }

  @Test
  @DisplayName("Should reject assigning empty roles set")
  void shouldRejectEmptyRoles() {
    UUID userId = UUID.randomUUID();
    Role role1 = Role.builder().id(UUID.randomUUID()).code("ROLE_STAFF").name("Staff").build();
    User user =
        User.builder()
            .id(userId)
            .email("staff@platform.com")
            .passwordHash("hash")
            .fullName("Staff User")
            .enabled(true)
            .internal(true)
            .roles(Set.of(role1))
            .build();

    org.junit.jupiter.api.Assertions.assertThrows(
        com.platform.app.iam.domain.exception.EmptyRolesException.class,
        () -> user.assignRoles(Set.of(), UUID.randomUUID()));

    org.junit.jupiter.api.Assertions.assertThrows(
        com.platform.app.iam.domain.exception.EmptyRolesException.class,
        () -> user.assignRoles(null, UUID.randomUUID()));
  }

  @Test
  @DisplayName("Should reject admin revoking ROLE_ADMIN from self")
  void shouldRejectAdminSelfRoleRevocation() {
    UUID adminId = UUID.randomUUID();
    Role adminRole = Role.builder().id(UUID.randomUUID()).code("ROLE_ADMIN").name("Admin").build();
    Role staffRole = Role.builder().id(UUID.randomUUID()).code("ROLE_STAFF").name("Staff").build();

    User adminUser =
        User.builder()
            .id(adminId)
            .email("admin@platform.com")
            .passwordHash("hash")
            .fullName("Admin User")
            .enabled(true)
            .internal(true)
            .roles(Set.of(adminRole))
            .build();

    // Admin attempting to replace own role with only ROLE_STAFF
    org.junit.jupiter.api.Assertions.assertThrows(
        com.platform.app.iam.domain.exception.SelfRoleRevocationException.class,
        () -> adminUser.assignRoles(Set.of(staffRole), adminId));
  }

  @Test
  @DisplayName("Should allow admin updating self when retaining ROLE_ADMIN")
  void shouldAllowAdminUpdatingSelfRetainingAdmin() {
    UUID adminId = UUID.randomUUID();
    Role adminRole = Role.builder().id(UUID.randomUUID()).code("ROLE_ADMIN").name("Admin").build();
    Role managerRole =
        Role.builder().id(UUID.randomUUID()).code("ROLE_MANAGER").name("Manager").build();

    User adminUser =
        User.builder()
            .id(adminId)
            .email("admin@platform.com")
            .passwordHash("hash")
            .fullName("Admin User")
            .enabled(true)
            .internal(true)
            .roles(Set.of(adminRole))
            .build();

    adminUser.assignRoles(Set.of(adminRole, managerRole), adminId);

    assertEquals(2, adminUser.getRoles().size());
    assertTrue(adminUser.getRoleCodes().contains("ROLE_ADMIN"));
    assertTrue(adminUser.getRoleCodes().contains("ROLE_MANAGER"));
  }
}
