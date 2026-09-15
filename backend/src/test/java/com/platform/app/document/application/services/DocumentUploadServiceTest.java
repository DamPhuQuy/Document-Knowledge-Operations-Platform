package com.platform.app.document.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.platform.app.document.application.dto.DocumentResponseDto;
import com.platform.app.document.application.dto.UploadDocumentCommand;
import com.platform.app.document.application.event.DocumentUploadedEvent;
import com.platform.app.document.application.ports.outbound.DocumentRepositoryPort;
import com.platform.app.document.application.ports.outbound.DocumentVersionRepositoryPort;
import com.platform.app.document.application.ports.outbound.ObjectStoragePort;
import com.platform.app.document.domain.exception.PayloadTooLargeException;
import com.platform.app.document.domain.exception.StorageException;
import com.platform.app.document.domain.exception.UnsupportedMediaTypeException;
import com.platform.app.document.domain.model.AccessLevel;
import com.platform.app.document.domain.model.Document;
import com.platform.app.document.domain.model.DocumentVersion;
import com.platform.app.document.domain.model.ProcessingStatus;
import com.platform.app.document.infrastructure.adapters.secondary.storage.config.S3StorageProperties;

@ExtendWith(MockitoExtension.class)
class DocumentUploadServiceTest {

  @Mock
  private ObjectStoragePort objectStoragePort;

  @Mock
  private DocumentRepositoryPort documentRepositoryPort;

  @Mock
  private DocumentVersionRepositoryPort documentVersionRepositoryPort;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  private S3StorageProperties storageProperties;
  private DocumentUploadService service;

  @BeforeEach
  void setUp() {
    storageProperties = new S3StorageProperties();
    storageProperties.setBucketName("test-bucket");

    service = new DocumentUploadService(
        objectStoragePort,
        documentRepositoryPort,
        documentVersionRepositoryPort,
        storageProperties,
        eventPublisher
    );
  }

  @Test
  @DisplayName("Should upload document successfully, stream-compute SHA-256 and publish audit event")
  void shouldUploadDocumentSuccessfully() throws Exception {
    byte[] fileBytes = "Hello Document Knowledge Platform".getBytes(StandardCharsets.UTF_8);
    String expectedSha256 = HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(fileBytes)
    );

    UUID userId = UUID.randomUUID();
    UUID deptId = UUID.randomUUID();

    UploadDocumentCommand command = UploadDocumentCommand.builder()
        .originalFileName("report.pdf")
        .contentType("application/pdf")
        .fileSize(fileBytes.length)
        .inputStream(new ByteArrayInputStream(fileBytes))
        .title("Annual Report")
        .description("Company Annual Report 2026")
        .accessLevel(AccessLevel.INTERNAL)
        .departmentId(deptId)
        .userId(userId)
        .build();

    // Mock S3 upload reading the stream
    doAnswer(invocation -> {
      InputStream is = invocation.getArgument(1);
      byte[] buf = new byte[1024];
      while (is.read(buf) != -1) {
        // drain stream to update DigestInputStream
      }
      return null;
    }).when(objectStoragePort).upload(anyString(), any(InputStream.class), anyLong(), anyString());

    when(documentRepositoryPort.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(documentVersionRepositoryPort.save(any(DocumentVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));

    DocumentResponseDto response = service.uploadDocument(command);

    assertNotNull(response);
    assertEquals("Annual Report", response.getTitle());
    assertEquals("report.pdf", response.getOriginalFileName());
    assertEquals("PDF", response.getFileType());
    assertEquals(expectedSha256, response.getChecksumSha256());
    assertEquals(AccessLevel.INTERNAL, response.getAccessLevel());
    assertEquals(ProcessingStatus.UPLOADED, response.getProcessingStatus());
    assertEquals(1, response.getCurrentVersion());

    verify(objectStoragePort).upload(anyString(), any(InputStream.class), eq((long) fileBytes.length), eq("application/pdf"));
    verify(documentRepositoryPort).save(any(Document.class));
    verify(documentVersionRepositoryPort).save(any(DocumentVersion.class));

    ArgumentCaptor<DocumentUploadedEvent> eventCaptor = ArgumentCaptor.forClass(DocumentUploadedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    DocumentUploadedEvent event = eventCaptor.getValue();
    assertEquals(expectedSha256, event.getChecksumSha256());
    assertEquals(1, event.getVersionNumber());
    assertEquals(userId, event.getUploadedByUserId());
  }

  @Test
  @DisplayName("Should reject unsupported file extensions (.exe, .sh)")
  void shouldRejectUnsupportedFileExtension() {
    byte[] fileBytes = "test binary".getBytes();
    UploadDocumentCommand command = UploadDocumentCommand.builder()
        .originalFileName("malicious.exe")
        .contentType("application/x-msdownload")
        .fileSize(fileBytes.length)
        .inputStream(new ByteArrayInputStream(fileBytes))
        .userId(UUID.randomUUID())
        .build();

    assertThrows(UnsupportedMediaTypeException.class, () -> service.uploadDocument(command));
    verify(objectStoragePort, never()).upload(anyString(), any(), anyLong(), anyString());
  }

  @Test
  @DisplayName("Should reject files exceeding 50MB limit")
  void shouldRejectOversizedFile() {
    byte[] fileBytes = "test".getBytes();
    long oversized = 50L * 1024 * 1024 + 1; // 50MB + 1 byte

    UploadDocumentCommand command = UploadDocumentCommand.builder()
        .originalFileName("huge.pdf")
        .contentType("application/pdf")
        .fileSize(oversized)
        .inputStream(new ByteArrayInputStream(fileBytes))
        .userId(UUID.randomUUID())
        .build();

    assertThrows(PayloadTooLargeException.class, () -> service.uploadDocument(command));
    verify(objectStoragePort, never()).upload(anyString(), any(), anyLong(), anyString());
  }

  @Test
  @DisplayName("Should trigger S3 compensation delete when database persistence fails")
  void shouldTriggerCompensationOnDbFailure() {
    byte[] fileBytes = "data".getBytes();
    UploadDocumentCommand command = UploadDocumentCommand.builder()
        .originalFileName("doc.docx")
        .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        .fileSize(fileBytes.length)
        .inputStream(new ByteArrayInputStream(fileBytes))
        .userId(UUID.randomUUID())
        .build();

    doAnswer(invocation -> {
      InputStream is = invocation.getArgument(1);
      is.readAllBytes();
      return null;
    }).when(objectStoragePort).upload(anyString(), any(InputStream.class), anyLong(), anyString());

    when(documentRepositoryPort.save(any(Document.class)))
        .thenThrow(new RuntimeException("Database connection terminated unexpectedly"));

    assertThrows(RuntimeException.class, () -> service.uploadDocument(command));

    // Verify S3 compensation delete was called
    verify(objectStoragePort).delete(anyString());
  }

  @Test
  @DisplayName("Should propagate StorageException when S3 upload fails without touching database")
  void shouldPropagateStorageExceptionWhenS3UploadFails() {
    byte[] fileBytes = "data".getBytes();
    UploadDocumentCommand command = UploadDocumentCommand.builder()
        .originalFileName("doc.txt")
        .contentType("text/plain")
        .fileSize(fileBytes.length)
        .inputStream(new ByteArrayInputStream(fileBytes))
        .userId(UUID.randomUUID())
        .build();

    doThrow(new StorageException("S3 network failure"))
        .when(objectStoragePort).upload(anyString(), any(InputStream.class), anyLong(), anyString());

    assertThrows(StorageException.class, () -> service.uploadDocument(command));
    verify(documentRepositoryPort, never()).save(any());
    verify(documentVersionRepositoryPort, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any());
  }
}
