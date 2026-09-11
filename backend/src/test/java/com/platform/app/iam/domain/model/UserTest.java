package com.platform.app.iam.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.platform.app.shared.domain.AuditMetadata;
import com.platform.app.shared.domain.UserFlags;

class UserTest {

  @Test
  @DisplayName("Should correctly assemble user with roles and permissions")
  void shouldAssembleUserWithRolesAndPermissions() {
    UserId userId = UserId.generate();
    RoleId roleId = RoleId.generate();
    DepartmentId deptId = DepartmentId.from(UUID.randomUUID());

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
            .flags(UserFlags.of(true, true))
            .roles(Set.of(role))
            .auditMetadata(AuditMetadata.now())
            .build();

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
    User user =
        User.builder()
            .id(UserId.generate())
            .email("inactive@platform.com")
            .passwordHash("hash")
            .fullName("Inactive User")
            .flags(UserFlags.of(false, false))
            .roles(Set.of())
            .build();

    assertFalse(user.isEnabled());
  }
}
