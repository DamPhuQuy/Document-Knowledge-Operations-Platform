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
}
