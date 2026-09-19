package com.platform.app.document.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DocumentAccessModelTest {

    @Test
    @DisplayName("Should create DocumentUserAccess with valid attributes")
    void shouldCreateDocumentUserAccess() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        DocumentUserAccess access = DocumentUserAccess.builder()
            .documentId(docId)
            .userId(userId)
            .permissionLevel(PermissionLevel.EDIT)
            .build();

        assertNotNull(access.getId());
        assertEquals(docId, access.getDocumentId());
        assertEquals(userId, access.getUserId());
        assertEquals(PermissionLevel.EDIT, access.getPermissionLevel());
        assertNotNull(access.getCreatedAt());
    }

    @Test
    @DisplayName(
        "Should reject DocumentUserAccess with null required attributes"
    )
    void shouldRejectDocumentUserAccessWithNullAttributes() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        assertThrows(NullPointerException.class, () ->
            new DocumentUserAccess(
                null,
                null,
                userId,
                PermissionLevel.VIEW,
                null
            )
        );
        assertThrows(NullPointerException.class, () ->
            new DocumentUserAccess(
                null,
                docId,
                null,
                PermissionLevel.VIEW,
                null
            )
        );
        assertThrows(NullPointerException.class, () ->
            new DocumentUserAccess(null, docId, userId, null, null)
        );
    }

    @Test
    @DisplayName("Should create DocumentDepartmentAccess with valid attributes")
    void shouldCreateDocumentDepartmentAccess() {
        UUID docId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        DocumentDepartmentAccess access = DocumentDepartmentAccess.builder()
            .documentId(docId)
            .departmentId(deptId)
            .permissionLevel(PermissionLevel.VIEW)
            .build();

        assertNotNull(access.getId());
        assertEquals(docId, access.getDocumentId());
        assertEquals(deptId, access.getDepartmentId());
        assertEquals(PermissionLevel.VIEW, access.getPermissionLevel());
        assertNotNull(access.getCreatedAt());
    }

    @Test
    @DisplayName("Should create DocumentRoleAccess with valid attributes")
    void shouldCreateDocumentRoleAccess() {
        UUID docId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        DocumentRoleAccess access = DocumentRoleAccess.builder()
            .documentId(docId)
            .roleId(roleId)
            .permissionLevel(PermissionLevel.ADMIN)
            .build();

        assertNotNull(access.getId());
        assertEquals(docId, access.getDocumentId());
        assertEquals(roleId, access.getRoleId());
        assertEquals(PermissionLevel.ADMIN, access.getPermissionLevel());
        assertNotNull(access.getCreatedAt());
    }

    @Test
    @DisplayName("Should update access level on Document entity")
    void shouldUpdateDocumentAccessLevel() {
        UUID userId = UUID.randomUUID();
        Document doc = Document.builder()
            .originalFileName("report.pdf")
            .contentType("application/pdf")
            .fileSizeBytes(100L)
            .checksumSha256(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
            )
            .storageKey("documents/1/report.pdf")
            .uploadedByUserId(userId)
            .accessLevel(AccessLevel.INTERNAL)
            .build();

        assertEquals(AccessLevel.INTERNAL, doc.getAccessLevel());

        doc.updateAccessLevel(AccessLevel.CONFIDENTIAL);
        assertEquals(AccessLevel.CONFIDENTIAL, doc.getAccessLevel());

        assertThrows(NullPointerException.class, () ->
            doc.updateAccessLevel(null)
        );
    }
}
