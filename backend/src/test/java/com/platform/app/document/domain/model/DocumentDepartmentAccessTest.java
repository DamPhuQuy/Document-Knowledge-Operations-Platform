package com.platform.app.document.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DocumentDepartmentAccessTest {

    @Test
    @DisplayName("Should construct DocumentDepartmentAccess with builder and generated id when id is null")
    void shouldConstructWithGeneratedId() {
        UUID docId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        DocumentDepartmentAccess access = DocumentDepartmentAccess.builder()
            .documentId(docId)
            .departmentId(deptId)
            .permissionLevel(PermissionLevel.ADMIN)
            .build();

        assertThat(access.getId()).isNotNull();
        assertThat(access.getDocumentId()).isEqualTo(docId);
        assertThat(access.getDepartmentId()).isEqualTo(deptId);
        assertThat(access.getPermissionLevel()).isEqualTo(PermissionLevel.ADMIN);
        assertThat(access.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should construct with explicit values and check equality")
    void shouldConstructWithExplicitValuesAndCheckEquality() {
        UUID id = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();
        Instant now = Instant.now();

        DocumentDepartmentAccess a1 = DocumentDepartmentAccess.builder()
            .id(id)
            .documentId(docId)
            .departmentId(deptId)
            .permissionLevel(PermissionLevel.VIEW)
            .createdAt(now)
            .build();

        DocumentDepartmentAccess a2 = DocumentDepartmentAccess.builder()
            .id(id)
            .documentId(docId)
            .departmentId(deptId)
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
        UUID deptId = UUID.randomUUID();

        assertThatThrownBy(() -> DocumentDepartmentAccess.builder()
            .documentId(null)
            .departmentId(deptId)
            .permissionLevel(PermissionLevel.VIEW)
            .build()
        ).isInstanceOf(NullPointerException.class).hasMessageContaining("Document ID must not be null");

        assertThatThrownBy(() -> DocumentDepartmentAccess.builder()
            .documentId(docId)
            .departmentId(null)
            .permissionLevel(PermissionLevel.VIEW)
            .build()
        ).isInstanceOf(NullPointerException.class).hasMessageContaining("Department ID must not be null");

        assertThatThrownBy(() -> DocumentDepartmentAccess.builder()
            .documentId(docId)
            .departmentId(deptId)
            .permissionLevel(null)
            .build()
        ).isInstanceOf(NullPointerException.class).hasMessageContaining("Permission level must not be null");
    }
}
