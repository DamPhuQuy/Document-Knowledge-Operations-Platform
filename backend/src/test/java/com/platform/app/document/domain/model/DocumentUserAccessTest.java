package com.platform.app.document.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DocumentUserAccessTest {

    @Test
    @DisplayName("Should construct DocumentUserAccess with builder and generated id when id is null")
    void shouldConstructWithGeneratedId() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        DocumentUserAccess access = DocumentUserAccess.builder()
            .documentId(docId)
            .userId(userId)
            .permissionLevel(PermissionLevel.VIEW)
            .build();

        assertThat(access.getId()).isNotNull();
        assertThat(access.getDocumentId()).isEqualTo(docId);
        assertThat(access.getUserId()).isEqualTo(userId);
        assertThat(access.getPermissionLevel()).isEqualTo(PermissionLevel.VIEW);
        assertThat(access.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should construct with explicit values and check equality")
    void shouldConstructWithExplicitValuesAndCheckEquality() {
        UUID id = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        DocumentUserAccess a1 = DocumentUserAccess.builder()
            .id(id)
            .documentId(docId)
            .userId(userId)
            .permissionLevel(PermissionLevel.EDIT)
            .createdAt(now)
            .build();

        DocumentUserAccess a2 = DocumentUserAccess.builder()
            .id(id)
            .documentId(docId)
            .userId(userId)
            .permissionLevel(PermissionLevel.EDIT)
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
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> DocumentUserAccess.builder()
            .documentId(null)
            .userId(userId)
            .permissionLevel(PermissionLevel.VIEW)
            .build()
        ).isInstanceOf(NullPointerException.class).hasMessageContaining("Document ID must not be null");

        assertThatThrownBy(() -> DocumentUserAccess.builder()
            .documentId(docId)
            .userId(null)
            .permissionLevel(PermissionLevel.VIEW)
            .build()
        ).isInstanceOf(NullPointerException.class).hasMessageContaining("User ID must not be null");

        assertThatThrownBy(() -> DocumentUserAccess.builder()
            .documentId(docId)
            .userId(userId)
            .permissionLevel(null)
            .build()
        ).isInstanceOf(NullPointerException.class).hasMessageContaining("Permission level must not be null");
    }
}
