package com.platform.app.iam.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  @DisplayName("Should correctly assemble user with roles and permissions")
  void shouldAssembleUserWithRolesAndPermissions() {
    UserId userId = UserId.generate();
    RoleId roleId = RoleId.generate();
    DepartmentId deptId = DepartmentId.from(UUID.randomUUID());

    Permission p1 = new Permission(UUID.randomUUID(), "DOC_READ", "Read Documents", "DOC", "Can read documents");
    Permission p2 = new Permission(UUID.randomUUID(), "DOC_WRITE", "Write Documents", "DOC", "Can write documents");
    Role role = new Role(roleId, "DOCUMENT_MANAGER", "Document Manager", "Manages documents", Set.of(p1, p2));

    User user = new User(
        userId,
        "User@Platform.COM",
        "$2a$12$e88...hashed",
        "Test User",
        deptId,
        true,
        true,
        Set.of(role),
        Instant.now(),
        Instant.now()
    );

    assertEquals("user@platform.com", user.getEmail());
    assertTrue(user.isEnabled());
    assertTrue(user.isInternal());
    assertEquals(1, user.getRoleIds().size());
    assertTrue(user.getRoleIds().contains(roleId.value()));
    assertEquals(Set.of("DOC_READ", "DOC_WRITE"), user.getAllPermissionCodes());
  }

  @Test
  @DisplayName("Should detect disabled user state")
  void shouldDetectDisabledUser() {
    User user = new User(
        UserId.generate(),
        "inactive@platform.com",
        "hash",
        "Inactive User",
        null,
        false,
        false,
        Set.of(),
        Instant.now(),
        Instant.now()
    );

    assertFalse(user.isEnabled());
  }
}
