package com.platform.app.document.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.platform.app.document.application.dto.ConfigureDocumentAclCommand;
import com.platform.app.document.application.dto.DepartmentGrantDto;
import com.platform.app.document.application.dto.DocumentPermissionsResponseDto;
import com.platform.app.document.application.dto.RoleGrantDto;
import com.platform.app.document.application.dto.UserGrantDto;
import com.platform.app.document.application.event.DocumentAclUpdatedEvent;
import com.platform.app.document.application.ports.outbound.DocumentAclRepositoryPort;
import com.platform.app.document.application.ports.outbound.DocumentRepositoryPort;
import com.platform.app.document.domain.exception.DocumentAccessDeniedException;
import com.platform.app.document.domain.exception.DocumentNotFoundException;
import com.platform.app.document.domain.exception.DocumentValidationException;
import com.platform.app.document.domain.model.AccessLevel;
import com.platform.app.document.domain.model.Document;
import com.platform.app.document.domain.model.DocumentUserAccess;
import com.platform.app.document.domain.model.PermissionLevel;

@ExtendWith(MockitoExtension.class)
class DocumentAclServiceTest {

  @Mock
  private DocumentRepositoryPort documentRepositoryPort;

  @Mock
  private DocumentAclRepositoryPort documentAclRepositoryPort;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  private DocumentAclService service;

  @BeforeEach
  void setUp() {
    service = new DocumentAclService(documentRepositoryPort, documentAclRepositoryPort, eventPublisher);
  }

  private Document createSampleDocument(UUID docId, UUID ownerId, AccessLevel accessLevel) {
    return Document.builder()
        .id(docId)
        .originalFileName("spec.pdf")
        .contentType("application/pdf")
        .fileSizeBytes(2048L)
        .checksumSha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
        .storageKey("documents/123/spec.pdf")
        .uploadedByUserId(ownerId)
        .accessLevel(accessLevel)
        .build();
  }

  @Test
  @DisplayName("Should successfully configure ACL by document owner")
  void shouldConfigureAclByOwnerSuccessfully() {
    UUID docId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    UUID targetDeptId = UUID.randomUUID();
    UUID targetRoleId = UUID.randomUUID();

    Document doc = createSampleDocument(docId, ownerId, AccessLevel.INTERNAL);
    when(documentRepositoryPort.findById(docId)).thenReturn(Optional.of(doc));
    when(documentRepositoryPort.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

    ConfigureDocumentAclCommand command = ConfigureDocumentAclCommand.builder()
        .documentId(docId)
        .currentUserId(ownerId)
        .isAdmin(false)
        .hasManagePermissions(false)
        .accessLevel(AccessLevel.CONFIDENTIAL)
        .userGrants(List.of(UserGrantDto.builder().userId(targetUserId).permissionLevel(PermissionLevel.EDIT).build()))
        .departmentGrants(List.of(DepartmentGrantDto.builder().departmentId(targetDeptId).permissionLevel(PermissionLevel.VIEW).build()))
        .roleGrants(List.of(RoleGrantDto.builder().roleId(targetRoleId).permissionLevel(PermissionLevel.ADMIN).build()))
        .build();

    DocumentPermissionsResponseDto response = service.configureAcl(command);

    assertNotNull(response);
    assertEquals(docId, response.getDocumentId());
    assertEquals(AccessLevel.CONFIDENTIAL, response.getAccessLevel());
    assertEquals(1, response.getUserGrants().size());
    assertEquals(1, response.getDepartmentGrants().size());
    assertEquals(1, response.getRoleGrants().size());

    // Verify document was saved with new access level
    verify(documentRepositoryPort).save(doc);
    assertEquals(AccessLevel.CONFIDENTIAL, doc.getAccessLevel());

    // Verify ACL repository was called to replace permissions
    verify(documentAclRepositoryPort).replacePermissions(eq(docId), any(), any(), any());

    // Verify domain event was published
    ArgumentCaptor<DocumentAclUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(DocumentAclUpdatedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    DocumentAclUpdatedEvent publishedEvent = eventCaptor.getValue();
    assertEquals(docId, publishedEvent.getDocumentId());
    assertEquals(ownerId, publishedEvent.getUpdatedByUserId());
    assertEquals(AccessLevel.CONFIDENTIAL, publishedEvent.getAccessLevel());
    assertEquals(1, publishedEvent.getUserGrantsCount());
    assertEquals(1, publishedEvent.getDepartmentGrantsCount());
    assertEquals(1, publishedEvent.getRoleGrantsCount());
  }

  @Test
  @DisplayName("Should successfully configure ACL by admin user")
  void shouldConfigureAclByAdminSuccessfully() {
    UUID docId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID adminId = UUID.randomUUID();

    Document doc = createSampleDocument(docId, ownerId, AccessLevel.INTERNAL);
    when(documentRepositoryPort.findById(docId)).thenReturn(Optional.of(doc));
    when(documentRepositoryPort.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

    ConfigureDocumentAclCommand command = ConfigureDocumentAclCommand.builder()
        .documentId(docId)
        .currentUserId(adminId)
        .isAdmin(true)
        .hasManagePermissions(false)
        .accessLevel(AccessLevel.PUBLIC)
        .userGrants(Collections.emptyList())
        .departmentGrants(Collections.emptyList())
        .roleGrants(Collections.emptyList())
        .build();

    DocumentPermissionsResponseDto response = service.configureAcl(command);

    assertNotNull(response);
    assertEquals(AccessLevel.PUBLIC, response.getAccessLevel());
    verify(documentAclRepositoryPort).replacePermissions(eq(docId), any(), any(), any());
    verify(eventPublisher).publishEvent(any(DocumentAclUpdatedEvent.class));
  }

  @Test
  @DisplayName("Should successfully configure ACL by user with manage:permissions authority")
  void shouldConfigureAclByPrivilegedUserSuccessfully() {
    UUID docId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID privilegedUserId = UUID.randomUUID();

    Document doc = createSampleDocument(docId, ownerId, AccessLevel.INTERNAL);
    when(documentRepositoryPort.findById(docId)).thenReturn(Optional.of(doc));
    when(documentRepositoryPort.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

    ConfigureDocumentAclCommand command = ConfigureDocumentAclCommand.builder()
        .documentId(docId)
        .currentUserId(privilegedUserId)
        .isAdmin(false)
        .hasManagePermissions(true)
        .accessLevel(AccessLevel.RESTRICTED)
        .build();

    DocumentPermissionsResponseDto response = service.configureAcl(command);

    assertNotNull(response);
    assertEquals(AccessLevel.RESTRICTED, response.getAccessLevel());
  }

  @Test
  @DisplayName("Should reject unauthorized user attempting to configure ACL with 403 AccessDenied")
  void shouldRejectUnauthorizedUserFromConfiguringAcl() {
    UUID docId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID unauthorizedUserId = UUID.randomUUID();

    Document doc = createSampleDocument(docId, ownerId, AccessLevel.INTERNAL);
    when(documentRepositoryPort.findById(docId)).thenReturn(Optional.of(doc));

    ConfigureDocumentAclCommand command = ConfigureDocumentAclCommand.builder()
        .documentId(docId)
        .currentUserId(unauthorizedUserId)
        .isAdmin(false)
        .hasManagePermissions(false)
        .accessLevel(AccessLevel.CONFIDENTIAL)
        .build();

    assertThrows(DocumentAccessDeniedException.class, () -> service.configureAcl(command));

    verify(documentRepositoryPort, never()).save(any());
    verify(documentAclRepositoryPort, never()).replacePermissions(any(), any(), any(), any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("Should throw 404 DocumentNotFoundException when configuring ACL on non-existent document")
  void shouldThrowNotFoundWhenConfiguringNonExistentDocument() {
    UUID docId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(documentRepositoryPort.findById(docId)).thenReturn(Optional.empty());

    ConfigureDocumentAclCommand command = ConfigureDocumentAclCommand.builder()
        .documentId(docId)
        .currentUserId(userId)
        .isAdmin(true)
        .accessLevel(AccessLevel.PUBLIC)
        .build();

    assertThrows(DocumentNotFoundException.class, () -> service.configureAcl(command));
  }

  @Test
  @DisplayName("Should throw DocumentValidationException when accessLevel is null")
  void shouldThrowValidationExceptionWhenAccessLevelIsNull() {
    UUID docId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    ConfigureDocumentAclCommand command = ConfigureDocumentAclCommand.builder()
        .documentId(docId)
        .currentUserId(userId)
        .isAdmin(true)
        .accessLevel(null)
        .build();

    assertThrows(DocumentValidationException.class, () -> service.configureAcl(command));
  }

  @Test
  @DisplayName("Should get permissions successfully for document owner")
  void shouldGetPermissionsForOwner() {
    UUID docId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();

    Document doc = createSampleDocument(docId, ownerId, AccessLevel.CONFIDENTIAL);
    when(documentRepositoryPort.findById(docId)).thenReturn(Optional.of(doc));
    when(documentAclRepositoryPort.findUserGrants(docId)).thenReturn(List.of(
        DocumentUserAccess.builder().documentId(docId).userId(targetUserId).permissionLevel(PermissionLevel.VIEW).build()
    ));
    when(documentAclRepositoryPort.findDepartmentGrants(docId)).thenReturn(Collections.emptyList());
    when(documentAclRepositoryPort.findRoleGrants(docId)).thenReturn(Collections.emptyList());

    DocumentPermissionsResponseDto permissions = service.getPermissions(docId, ownerId, false, false);

    assertNotNull(permissions);
    assertEquals(docId, permissions.getDocumentId());
    assertEquals(AccessLevel.CONFIDENTIAL, permissions.getAccessLevel());
    assertEquals(1, permissions.getUserGrants().size());
  }

  @Test
  @DisplayName("Should reject non-privileged user viewing confidential document without view grant")
  void shouldRejectNonPrivilegedUserViewingConfidentialAcl() {
    UUID docId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID viewerId = UUID.randomUUID();

    Document doc = createSampleDocument(docId, ownerId, AccessLevel.CONFIDENTIAL);
    when(documentRepositoryPort.findById(docId)).thenReturn(Optional.of(doc));
    when(documentAclRepositoryPort.hasUserPermission(docId, viewerId, PermissionLevel.VIEW)).thenReturn(false);

    assertThrows(DocumentAccessDeniedException.class, () -> service.getPermissions(docId, viewerId, false, false));
  }
}
