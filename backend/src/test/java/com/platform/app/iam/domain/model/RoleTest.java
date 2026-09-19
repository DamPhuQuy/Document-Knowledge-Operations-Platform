package com.platform.app.iam.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoleTest {

    @Test
    @DisplayName("Should construct Role with uppercase code and defensive copy of permissions")
    void shouldConstructRoleWithUppercaseCodeAndDefensiveCopy() {
        UUID id = UUID.randomUUID();
        UUID permId = UUID.randomUUID();

        Permission permission = Permission.builder()
            .id(permId)
            .code("iam:read")
            .name("Read IAM")
            .module("IAM")
            .description("Desc")
            .build();

        Role role = Role.builder()
            .id(id)
            .code("role_admin")
            .name("Administrator")
            .description("Full Admin Access")
            .permissions(Set.of(permission))
            .build();

        assertThat(role.getId()).isEqualTo(id);
        assertThat(role.getCode()).isEqualTo("ROLE_ADMIN");
        assertThat(role.getName()).isEqualTo("Administrator");
        assertThat(role.getDescription()).isEqualTo("Full Admin Access");
        assertThat(role.getPermissions()).containsExactly(permission);
        assertThat(role.getPermissionCodes()).containsExactly("IAM:READ");

        // Verify unmodifiable set
        Set<Permission> perms = role.getPermissions();
        assertThatThrownBy(() -> perms.add(permission))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should handle null permissions as empty set")
    void shouldHandleNullPermissionsAsEmptySet() {
        Role role = Role.builder()
            .id(UUID.randomUUID())
            .code("STAFF")
            .name("Staff")
            .permissions(null)
            .build();

        assertThat(role.getPermissions()).isEmpty();
        assertThat(role.getPermissionCodes()).isEmpty();
    }

    @Test
    @DisplayName("Should verify equals, hashCode, and toString")
    void shouldVerifyEqualsHashCodeAndToString() {
        UUID id = UUID.randomUUID();
        Role r1 = Role.builder().id(id).code("ADMIN").name("Admin").build();
        Role r2 = Role.builder().id(id).code("ADMIN").name("Admin").build();

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        assertThat(r1.toString()).contains("ADMIN");
    }

    @Test
    @DisplayName("Should throw NullPointerException when required fields are null")
    void shouldThrowWhenRequiredFieldsNull() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> Role.builder().id(null).code("ADMIN").name("Admin").build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Role id must not be null");

        assertThatThrownBy(() -> Role.builder().id(id).code(null).name("Admin").build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Role code must not be null");

        assertThatThrownBy(() -> Role.builder().id(id).code("ADMIN").name(null).build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Role name must not be null");
    }
}
