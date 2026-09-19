package com.platform.app.document.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DocumentRoleAccessTest {

    @Test
    @DisplayName("Should construct DocumentRoleAccess with builder and generated id when id is null")
    void shouldConstructWithGeneratedId() {
        UUID docId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        DocumentRoleAccess access = DocumentRoleAccess.builder()
            .documentId(docId)
            .roleId(roleId)
            .permissionLevel(PermissionLevel.EDIT)
            .build();

        assertThat(access.getId()).isNotNull();
        assertThat(access.getDocumentId()).isEqualTo(docId);
        assertThat(access.getRoleId()).isEqualTo(roleId);
        assertThat(access.getPermissionLevel()).isEqualTo(PermissionLevel.EDIT);
        assertThat(access.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should construct with explicit values and check equality")
    void shouldConstructWithExplicitValuesAndCheckEquality() {
        UUID id = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        Instant now = Instant.now();

        DocumentRoleAccess a1 = DocumentRoleAccess.builder()
            .id(id)
            .documentId(docId)
            .roleId(roleId)
            .permissionLevel(PermissionLevel.VIEW)
            .createdAt(now)
            .build();

        DocumentRoleAccess a2 = DocumentRoleAccess.builder()
            .id(id)
            .documentId(docId)
            .roleId(roleId)
            .permissionLevel(PermissionLevel.VIEW)
            .createdAt(now)
            .build();

        assertThat(a1).isEqualTo(a2);
        assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
        assertThat(a1.toString()).contains(id.toString());
    }

    @Test
    @DisplayName("Should throw NullPointerException when required fields are null")
    void shouldThrowWhenRequiredFieldsAreNull() {
        UUID docId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        assertThatThrownBy(() -> DocumentRoleAccess.builder()
            .documentId(null)
            .roleId(roleId)
            .permissionLevel(PermissionLevel.VIEW)
            .build()
        ).isInstanceOf(NullPointerException.class).hasMessageContaining("Document ID must not be null");

        assertThatThrownBy(() -> DocumentRoleAccess.builder()
            .documentId(docId)
            .roleId(null)
            .permissionLevel(PermissionLevel.VIEW)
            .build()
        ).isInstanceOf(NullPointerException.class).hasMessageContaining("Role ID must not be null");

        assertThatThrownBy(() -> DocumentRoleAccess.builder()
            .documentId(docId)
            .roleId(roleId)
            .permissionLevel(null)
            .build()
        ).isInstanceOf(NullPointerException.class).hasMessageContaining("Permission level must not be null");
    }
}
