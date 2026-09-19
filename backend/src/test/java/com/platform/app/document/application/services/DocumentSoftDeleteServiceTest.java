package com.platform.app.document.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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

import com.platform.app.document.application.dto.SoftDeleteDocumentCommand;
import com.platform.app.document.application.event.DocumentSoftDeletedEvent;
import com.platform.app.document.application.ports.outbound.DocumentRepositoryPort;
import com.platform.app.document.domain.exception.DocumentAccessDeniedException;
import com.platform.app.document.domain.exception.DocumentNotFoundException;
import com.platform.app.document.domain.model.AccessLevel;
import com.platform.app.document.domain.model.Document;
import com.platform.app.document.domain.model.DocumentStatus;

@ExtendWith(MockitoExtension.class)
class DocumentSoftDeleteServiceTest {

  @Mock
  private DocumentRepositoryPort documentRepositoryPort;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  private DocumentSoftDeleteService service;

  private static final String SHA256 = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2";

  @BeforeEach
  void setUp() {
    service = new DocumentSoftDeleteService(documentRepositoryPort, eventPublisher);
  }

  private Document createDocument(UUID docId, UUID ownerId) {
    return Document.builder()
        .id(docId)
        .title("Test Doc")
        .originalFileName("test.pdf")
        .contentType("application/pdf")
        .fileSizeBytes(1024L)
        .checksumSha256(SHA256)
        .storageKey("documents/" + docId + "/v1/test.pdf")
        .uploadedByUserId(ownerId)
        .accessLevel(AccessLevel.INTERNAL)
        .status(DocumentStatus.UPLOADED)
        .build();
  }

  @Test
  @DisplayName("Should soft delete document and emit audit event when caller is the owner")
  void shouldSoftDeleteDocument_whenCallerIsOwner() {
    UUID docId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    Document doc = createDocument(docId, ownerId);

    when(documentRepositoryPort.findById(docId)).thenReturn(Optional.of(doc));
    when(documentRepositoryPort.softDelete(eq(docId), any(Instant.class))).thenReturn(true);

    SoftDeleteDocumentCommand command = SoftDeleteDocumentCommand.builder()
        .documentId(docId)
        .currentUserId(ownerId)
        .isAdmin(false)
        .hasDeletePermission(false)
        .build();

    service.softDeleteDocument(command);

    verify(documentRepositoryPort).softDelete(eq(docId), any(Instant.class));

    ArgumentCaptor<DocumentSoftDeletedEvent> eventCaptor = ArgumentCaptor.forClass(DocumentSoftDeletedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    DocumentSoftDeletedEvent event = eventCaptor.getValue();
    assertNotNull(event);
    assertEquals(docId, event.getDocumentId());
    assertEquals(ownerId, event.getDeletedByUserId());
    assertNotNull(event.getTimestamp());
  }

  @Test
  @DisplayName("Should soft delete document when caller is admin even if not owner")
  void shouldSoftDeleteDocument_whenCallerIsAdmin() {
    UUID docId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID adminId = UUID.randomUUID();
    Document doc = createDocument(docId, ownerId);

    when(documentRepositoryPort.findById(docId)).thenReturn(Optional.of(doc));
    when(documentRepositoryPort.softDelete(eq(docId), any(Instant.class))).thenReturn(true);

    SoftDeleteDocumentCommand command = SoftDeleteDocumentCommand.builder()
        .documentId(docId)
        .currentUserId(adminId)
        .isAdmin(true)
        .hasDeletePermission(false)
        .build();

    service.softDeleteDocument(command);

    verify(documentRepositoryPort).softDelete(eq(docId), any(Instant.class));
    verify(eventPublisher).publishEvent(any(DocumentSoftDeletedEvent.class));
  }

  @Test
  @DisplayName("Should soft delete document when caller has delete:documents permission")
  void shouldSoftDeleteDocument_whenCallerHasDeletePermission() {
    UUID docId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID staffId = UUID.randomUUID();
    Document doc = createDocument(docId, ownerId);

    when(documentRepositoryPort.findById(docId)).thenReturn(Optional.of(doc));
    when(documentRepositoryPort.softDelete(eq(docId), any(Instant.class))).thenReturn(true);

    SoftDeleteDocumentCommand command = SoftDeleteDocumentCommand.builder()
        .documentId(docId)
        .currentUserId(staffId)
        .isAdmin(false)
        .hasDeletePermission(true)
        .build();

    service.softDeleteDocument(command);

    verify(documentRepositoryPort).softDelete(eq(docId), any(Instant.class));
    verify(eventPublisher).publishEvent(any(DocumentSoftDeletedEvent.class));
  }

  @Test
  @DisplayName("Should throw DocumentAccessDeniedException when caller is neither owner nor admin nor has permission")
  void shouldThrowAccessDenied_whenCallerUnauthorized() {
    UUID docId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID strangerId = UUID.randomUUID();
    Document doc = createDocument(docId, ownerId);

    when(documentRepositoryPort.findById(docId)).thenReturn(Optional.of(doc));

    SoftDeleteDocumentCommand command = SoftDeleteDocumentCommand.builder()
        .documentId(docId)
        .currentUserId(strangerId)
        .isAdmin(false)
        .hasDeletePermission(false)
        .build();

    assertThrows(DocumentAccessDeniedException.class, () -> service.softDeleteDocument(command));

    verify(documentRepositoryPort, never()).softDelete(any(), any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("Should throw DocumentNotFoundException when document does not exist or is already deleted")
  void shouldThrowNotFound_whenDocumentDoesNotExist() {
    UUID docId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(documentRepositoryPort.findById(docId)).thenReturn(Optional.empty());

    SoftDeleteDocumentCommand command = SoftDeleteDocumentCommand.builder()
        .documentId(docId)
        .currentUserId(userId)
        .isAdmin(false)
        .hasDeletePermission(false)
        .build();

    assertThrows(DocumentNotFoundException.class, () -> service.softDeleteDocument(command));

    verify(documentRepositoryPort, never()).softDelete(any(), any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("Should throw DocumentNotFoundException when concurrent delete occurs")
  void shouldThrowNotFound_whenConcurrentDeleteOccurs() {
    UUID docId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    Document doc = createDocument(docId, ownerId);

    when(documentRepositoryPort.findById(docId)).thenReturn(Optional.of(doc));
    when(documentRepositoryPort.softDelete(eq(docId), any(Instant.class))).thenReturn(false);

    SoftDeleteDocumentCommand command = SoftDeleteDocumentCommand.builder()
        .documentId(docId)
        .currentUserId(ownerId)
        .isAdmin(false)
        .hasDeletePermission(false)
        .build();

    assertThrows(DocumentNotFoundException.class, () -> service.softDeleteDocument(command));

    verify(eventPublisher, never()).publishEvent(any());
  }
}
