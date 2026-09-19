package com.platform.app.document.infrastructure.adapters.secondary.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.platform.app.document.domain.model.DocumentDepartmentAccess;
import com.platform.app.document.domain.model.DocumentRoleAccess;
import com.platform.app.document.domain.model.DocumentUserAccess;
import com.platform.app.document.domain.model.PermissionLevel;
import com.platform.app.document.infrastructure.adapters.secondary.persistence.entity.DocumentUserAccessJpaEntity;
import com.platform.app.document.infrastructure.adapters.secondary.persistence.repository.SpringDataDocumentDepartmentAccessRepository;
import com.platform.app.document.infrastructure.adapters.secondary.persistence.repository.SpringDataDocumentRoleAccessRepository;
import com.platform.app.document.infrastructure.adapters.secondary.persistence.repository.SpringDataDocumentUserAccessRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentAclRepositoryAdapterTest {

    @Mock
    private SpringDataDocumentUserAccessRepository userAccessRepository;

    @Mock
    private SpringDataDocumentDepartmentAccessRepository departmentAccessRepository;

    @Mock
    private SpringDataDocumentRoleAccessRepository roleAccessRepository;

    private DocumentAclRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DocumentAclRepositoryAdapter(
            userAccessRepository,
            departmentAccessRepository,
            roleAccessRepository
        );
    }

    @Test
    @DisplayName(
        "Should replace permissions by deleting existing records and persisting new grants"
    )
    void shouldReplacePermissionsSuccessfully() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        List<DocumentUserAccess> userGrants = List.of(
            DocumentUserAccess.builder()
                .documentId(docId)
                .userId(userId)
                .permissionLevel(PermissionLevel.EDIT)
                .build()
        );
        List<DocumentDepartmentAccess> deptGrants = List.of(
            DocumentDepartmentAccess.builder()
                .documentId(docId)
                .departmentId(deptId)
                .permissionLevel(PermissionLevel.VIEW)
                .build()
        );
        List<DocumentRoleAccess> roleGrants = List.of(
            DocumentRoleAccess.builder()
                .documentId(docId)
                .roleId(roleId)
                .permissionLevel(PermissionLevel.ADMIN)
                .build()
        );

        adapter.replacePermissions(docId, userGrants, deptGrants, roleGrants);

        verify(userAccessRepository).deleteByDocumentId(docId);
        verify(departmentAccessRepository).deleteByDocumentId(docId);
        verify(roleAccessRepository).deleteByDocumentId(docId);

        verify(userAccessRepository).saveAll(anyList());
        verify(departmentAccessRepository).saveAll(anyList());
        verify(roleAccessRepository).saveAll(anyList());
    }

    @Test
    @DisplayName(
        "Should replace permissions with empty lists without calling saveAll"
    )
    void shouldReplacePermissionsWithEmptyLists() {
        UUID docId = UUID.randomUUID();

        adapter.replacePermissions(
            docId,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );

        verify(userAccessRepository).deleteByDocumentId(docId);
        verify(departmentAccessRepository).deleteByDocumentId(docId);
        verify(roleAccessRepository).deleteByDocumentId(docId);

        verify(userAccessRepository, never()).saveAll(anyList());
        verify(departmentAccessRepository, never()).saveAll(anyList());
        verify(roleAccessRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should query user grants and map to domain objects")
    void shouldFindUserGrants() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        DocumentUserAccessJpaEntity entity =
            DocumentUserAccessJpaEntity.builder()
                .id(UUID.randomUUID())
                .documentId(docId)
                .userId(userId)
                .permissionLevel(PermissionLevel.EDIT)
                .createdAt(Instant.now())
                .build();

        when(userAccessRepository.findByDocumentId(docId)).thenReturn(
            List.of(entity)
        );

        List<DocumentUserAccess> result = adapter.findUserGrants(docId);
        assertEquals(1, result.size());
        assertEquals(userId, result.get(0).getUserId());
        assertEquals(PermissionLevel.EDIT, result.get(0).getPermissionLevel());
    }

    @Test
    @DisplayName("Should check user permission hierarchy correctly")
    void shouldCheckUserPermissionHierarchy() {
        UUID docId = UUID.randomUUID();
        UUID userWithAdmin = UUID.randomUUID();
        UUID userWithEdit = UUID.randomUUID();
        UUID userWithView = UUID.randomUUID();

        when(userAccessRepository.findByDocumentId(docId)).thenReturn(
            List.of(
                DocumentUserAccessJpaEntity.builder()
                    .documentId(docId)
                    .userId(userWithAdmin)
                    .permissionLevel(PermissionLevel.ADMIN)
                    .build(),
                DocumentUserAccessJpaEntity.builder()
                    .documentId(docId)
                    .userId(userWithEdit)
                    .permissionLevel(PermissionLevel.EDIT)
                    .build(),
                DocumentUserAccessJpaEntity.builder()
                    .documentId(docId)
                    .userId(userWithView)
                    .permissionLevel(PermissionLevel.VIEW)
                    .build()
            )
        );

        // Admin has VIEW, EDIT, ADMIN
        assertTrue(
            adapter.hasUserPermission(
                docId,
                userWithAdmin,
                PermissionLevel.VIEW
            )
        );
        assertTrue(
            adapter.hasUserPermission(
                docId,
                userWithAdmin,
                PermissionLevel.EDIT
            )
        );
        assertTrue(
            adapter.hasUserPermission(
                docId,
                userWithAdmin,
                PermissionLevel.ADMIN
            )
        );

        // Edit has VIEW, EDIT, but not ADMIN
        assertTrue(
            adapter.hasUserPermission(docId, userWithEdit, PermissionLevel.VIEW)
        );
        assertTrue(
            adapter.hasUserPermission(docId, userWithEdit, PermissionLevel.EDIT)
        );
        assertFalse(
            adapter.hasUserPermission(
                docId,
                userWithEdit,
                PermissionLevel.ADMIN
            )
        );

        // View has VIEW, but not EDIT or ADMIN
        assertTrue(
            adapter.hasUserPermission(docId, userWithView, PermissionLevel.VIEW)
        );
        assertFalse(
            adapter.hasUserPermission(docId, userWithView, PermissionLevel.EDIT)
        );
        assertFalse(
            adapter.hasUserPermission(
                docId,
                userWithView,
                PermissionLevel.ADMIN
            )
        );

        // Non-existent user
        assertFalse(
            adapter.hasUserPermission(
                docId,
                UUID.randomUUID(),
                PermissionLevel.VIEW
            )
        );
    }
}
