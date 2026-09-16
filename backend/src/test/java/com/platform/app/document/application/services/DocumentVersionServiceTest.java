package com.platform.app.document.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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

import com.platform.app.document.application.dto.DocumentVersionResponseDto;
import com.platform.app.document.application.dto.UploadDocumentVersionCommand;
import com.platform.app.document.application.event.DocumentVersionCreatedEvent;
import com.platform.app.document.application.ports.inbound.StoreVersionMetadataUseCase;
import com.platform.app.document.application.ports.outbound.DocumentRepositoryPort;
import com.platform.app.document.application.ports.outbound.ObjectStoragePort;
import com.platform.app.document.domain.exception.DocumentAccessDeniedException;
import com.platform.app.document.domain.exception.DocumentNotFoundException;
import com.platform.app.document.domain.exception.PayloadTooLargeException;
import com.platform.app.document.domain.exception.UnsupportedMediaTypeException;
import com.platform.app.document.domain.model.AccessLevel;
import com.platform.app.document.domain.model.Document;
import com.platform.app.document.domain.model.DocumentStatus;
import com.platform.app.document.domain.model.DocumentVersion;

@ExtendWith(MockitoExtension.class)
class DocumentVersionServiceTest {

  private static final String SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

  @Mock
  private DocumentRepositoryPort documentRepositoryPort;

  @Mock
  private ObjectStoragePort objectStoragePort;

  @Mock
  private StoreVersionMetadataUseCase storeVersionMetadataUseCase;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  private DocumentVersionService versionService;

  @BeforeEach
  void setUp() {
    versionService = new DocumentVersionService(
        documentRepositoryPort,
        objectStoragePort,
        storeVersionMetadataUseCase,
        eventPublisher);
  }

  @Test
  @DisplayName("Should successfully upload new version when user is document owner")
  void shouldUploadNewVersionSuccessfullyWhenOwner() {
    UUID docId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    Document doc = Document.builder()
        .id(docId)
        .title("Annual Report")
        .originalFileName("report_v1.pdf")
        .contentType("application/pdf")
        .fileSizeBytes(1024L)
        .checksumSha256(SHA256)
        .storageKey("documents/" + docId + "/v1/report_v1.pdf")
        .currentVersion(1)
        .status(DocumentStatus.UPLOADED)
        .uploadedByUserId(userId)
        .accessLevel(AccessLevel.INTERNAL)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();

    when(documentRepositoryPort.findById(docId)).thenReturn(Optional.of(doc));

    DocumentVersion savedVersion = DocumentVersion.builder()
        .id(UUID.randomUUID())
        .documentId(docId)
        .versionNumber(2)
        .storageKey("documents/" + docId + "/v2/report_v2.pdf")
        .fileSizeBytes(2048L)
        .checksumSha256(SHA256)
        .changeSummary("Updated financial tables")
        .uploadedByUserId(userId)
        .createdAt(Instant.now())
        .build();

    when(storeVersionMetadataUseCase.persistVersionMetadata(
        eq(doc), any(DocumentVersion.class), eq(2), anyString(), anyString(), eq(2048L), eq("application/pdf"), eq("report_v2.pdf")))
        .thenReturn(savedVersion);

    UploadDocumentVersionCommand command = UploadDocumentVersionCommand.builder()
        .documentId(docId)
        .userId(userId)
        .inputStream(new ByteArrayInputStream("test content v2".getBytes()))
        .originalFileName("report_v2.pdf")
        .contentType("application/pdf")
        .fileSize(2048L)
        .changeSummary("Updated financial tables")
        .isAdmin(false)
        .build();

    DocumentVersionResponseDto response = versionService.uploadVersion(command);

    assertNotNull(response);
    assertEquals(2, response.getVersionNumber());
    assertEquals(docId, response.getDocumentId());
    assertEquals("Updated financial tables", response.getChangeSummary());
    assertEquals("documents/" + docId + "/v2/report_v2.pdf", response.getStorageKey());

    verify(objectStoragePort).upload(eq("documents/" + docId + "/v2/report_v2.pdf"), any(InputStream.class), eq(2048L), eq("application/pdf"));

    ArgumentCaptor<DocumentVersionCreatedEvent> eventCaptor = ArgumentCaptor.forClass(DocumentVersionCreatedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    DocumentVersionCreatedEvent event = eventCaptor.getValue();
    assertEquals(docId, event.getDocumentId());
    assertEquals(2, event.getVersionNumber());
    assertEquals(userId, event.getUploadedByUserId());
  }

  @Test
  @DisplayName("Should successfully upload new version when user is admin even if not owner")
  void shouldUploadNewVersionSuccessfullyWhenAdmin() {
    UUID docId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID adminUserId = UUID.randomUUID();

    Document doc = Document.builder()
        .id(docId)
        .title("Annual Report")
        .originalFileName("report_v1.pdf")
        .contentType("application/pdf")
        .fileSizeBytes(1024L)
        .checksumSha256(SHA256)
        .storageKey("documents/" + docId + "/v1/report_v1.pdf")
        .currentVersion(1)
        .status(DocumentStatus.UPLOADED)
        .uploadedByUserId(ownerId)
        .build();

    when(documentRepositoryPort.findById(docId)).thenReturn(Optional.of(doc));

    DocumentVersion savedVersion = DocumentVersion.builder()
        .id(UUID.randomUUID())
        .documentId(docId)
        .versionNumber(2)
        .storageKey("documents/" + docId + "/v2/report_v2.pdf")
        .fileSizeBytes(2048L)
        .checksumSha256(SHA256)
        .changeSummary("Admin override revision")
        .uploadedByUserId(adminUserId)
        .createdAt(Instant.now())
        .build();

    when(storeVersionMetadataUseCase.persistVersionMetadata(
        eq(doc), any(DocumentVersion.class), eq(2), anyString(), anyString(), eq(2048L), eq("application/pdf"), eq("report_v2.pdf")))
        .thenReturn(savedVersion);

    UploadDocumentVersionCommand command = UploadDocumentVersionCommand.builder()
        .documentId(docId)
        .userId(adminUserId)
        .inputStream(new ByteArrayInputStream("admin content".getBytes()))
        .originalFileName("report_v2.pdf")
        .contentType("application/pdf")
        .fileSize(2048L)
        .changeSummary("Admin override revision")
        .isAdmin(true)
        .build();

    DocumentVersionResponseDto response = versionService.uploadVersion(command);

    assertNotNull(response);
    assertEquals(2, response.getVersionNumber());
  }

  @Test
  @DisplayName("Should throw DocumentAccessDeniedException when user is not owner and not admin")
  void shouldThrowDocumentAccessDeniedExceptionWhenNotOwnerAndNotAdmin() {
    UUID docId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID intruderId = UUID.randomUUID();

    Document doc = Document.builder()
        .id(docId)
        .title("Annual Report")
        .originalFileName("report_v1.pdf")
        .contentType("application/pdf")
        .fileSizeBytes(1024L)
        .checksumSha256(SHA256)
        .storageKey("documents/" + docId + "/v1/report_v1.pdf")
        .currentVersion(1)
        .uploadedByUserId(ownerId)
        .build();

    when(documentRepositoryPort.findById(docId)).thenReturn(Optional.of(doc));

    UploadDocumentVersionCommand command = UploadDocumentVersionCommand.builder()
        .documentId(docId)
        .userId(intruderId)
        .inputStream(new ByteArrayInputStream("unauthorized content".getBytes()))
        .originalFileName("report_v2.pdf")
        .contentType("application/pdf")
        .fileSize(100L)
        .isAdmin(false)
        .build();

    assertThrows(DocumentAccessDeniedException.class, () -> versionService.uploadVersion(command));
    verify(objectStoragePort, never()).upload(anyString(), any(InputStream.class), anyLong(), anyString());
  }

  @Test
  @DisplayName("Should throw DocumentNotFoundException when document does not exist")
  void shouldThrowDocumentNotFoundExceptionWhenMissing() {
    UUID missingDocId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(documentRepositoryPort.findById(missingDocId)).thenReturn(Optional.empty());

    UploadDocumentVersionCommand command = UploadDocumentVersionCommand.builder()
        .documentId(missingDocId)
        .userId(userId)
        .inputStream(new ByteArrayInputStream("test".getBytes()))
        .originalFileName("doc.pdf")
        .fileSize(100L)
        .build();

    assertThrows(DocumentNotFoundException.class, () -> versionService.uploadVersion(command));
  }

  @Test
  @DisplayName("Should throw UnsupportedMediaTypeException for invalid file extension")
  void shouldRejectInvalidExtension() {
    UploadDocumentVersionCommand command = UploadDocumentVersionCommand.builder()
        .documentId(UUID.randomUUID())
        .userId(UUID.randomUUID())
        .inputStream(new ByteArrayInputStream("script".getBytes()))
        .originalFileName("malicious.sh")
        .fileSize(100L)
        .build();

    assertThrows(UnsupportedMediaTypeException.class, () -> versionService.uploadVersion(command));
  }

  @Test
  @DisplayName("Should throw PayloadTooLargeException for files > 50MB")
  void shouldRejectOversizedFile() {
    long oversized = Document.MAX_FILE_SIZE_BYTES + 1;
    UploadDocumentVersionCommand command = UploadDocumentVersionCommand.builder()
        .documentId(UUID.randomUUID())
        .userId(UUID.randomUUID())
        .inputStream(new ByteArrayInputStream(new byte[0]))
        .originalFileName("big.pdf")
        .fileSize(oversized)
        .build();

    assertThrows(PayloadTooLargeException.class, () -> versionService.uploadVersion(command));
  }

  @Test
  @DisplayName("Should execute S3 compensation delete when database persistence fails")
  void shouldExecuteS3CompensationWhenDatabaseFails() {
    UUID docId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    Document doc = Document.builder()
        .id(docId)
        .title("Report")
        .originalFileName("report_v1.pdf")
        .contentType("application/pdf")
        .fileSizeBytes(100L)
        .checksumSha256(SHA256)
        .storageKey("key")
        .currentVersion(1)
        .uploadedByUserId(userId)
        .build();

    when(documentRepositoryPort.findById(docId)).thenReturn(Optional.of(doc));

    when(storeVersionMetadataUseCase.persistVersionMetadata(
        eq(doc), any(DocumentVersion.class), eq(2), anyString(), anyString(), anyLong(), anyString(), anyString()))
        .thenThrow(new RuntimeException("Database connection timeout"));

    UploadDocumentVersionCommand command = UploadDocumentVersionCommand.builder()
        .documentId(docId)
        .userId(userId)
        .inputStream(new ByteArrayInputStream("data".getBytes()))
        .originalFileName("report_v2.pdf")
        .fileSize(100L)
        .build();

    assertThrows(RuntimeException.class, () -> versionService.uploadVersion(command));

    String expectedKey = "documents/" + docId + "/v2/report_v2.pdf";
    verify(objectStoragePort).delete(expectedKey);
    verify(eventPublisher, never()).publishEvent(any());
  }
}
