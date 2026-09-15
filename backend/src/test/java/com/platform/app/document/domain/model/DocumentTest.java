package com.platform.app.document.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.platform.app.document.domain.exception.DocumentValidationException;
import com.platform.app.document.domain.exception.PayloadTooLargeException;

class DocumentTest {

  private static final String VALID_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

  @Test
  @DisplayName("Should successfully create Document with valid attributes and default values")
  void shouldCreateDocumentSuccessfully() {
    UUID userId = UUID.randomUUID();
    UUID deptId = UUID.randomUUID();

    Document doc = Document.builder()
        .originalFileName("report.pdf")
        .title("Annual Report")
        .description("2026 Financial Report")
        .fileType("PDF")
        .mimeType("application/pdf")
        .fileSizeBytes(1024L)
        .checksumSha256(VALID_SHA256)
        .storageBucket("doc-knowledge-storage")
        .storageKey("documents/123/v1/report.pdf")
        .departmentId(deptId)
        .uploadedByUserId(userId)
        .build();

    assertNotNull(doc.getId());
    assertEquals("Annual Report", doc.getTitle());
    assertEquals("report.pdf", doc.getOriginalFileName());
    assertEquals("PDF", doc.getFileType());
    assertEquals(1024L, doc.getFileSizeBytes());
    assertEquals(VALID_SHA256, doc.getChecksumSha256());
    assertEquals(AccessLevel.INTERNAL, doc.getAccessLevel()); // Invariant B2
    assertEquals(ProcessingStatus.UPLOADED, doc.getProcessingStatus());
    assertEquals(1, doc.getCurrentVersion());
    assertTrue(doc.isS3Synced());
    assertNotNull(doc.getCreatedAt());
  }

  @Test
  @DisplayName("Should reject blank or null title")
  void shouldRejectBlankTitle() {
    UUID userId = UUID.randomUUID();

    assertThrows(DocumentValidationException.class, () ->
        Document.builder()
            .originalFileName("test.pdf")
            .title("   ")
            .fileType("PDF")
            .fileSizeBytes(500L)
            .checksumSha256(VALID_SHA256)
            .storageBucket("bucket")
            .storageKey("key")
            .uploadedByUserId(userId)
            .build()
    );
  }

  @Test
  @DisplayName("Should reject non-positive file size")
  void shouldRejectNonPositiveFileSize() {
    UUID userId = UUID.randomUUID();

    assertThrows(DocumentValidationException.class, () ->
        Document.builder()
            .originalFileName("test.pdf")
            .title("Test Document")
            .fileType("PDF")
            .fileSizeBytes(0L)
            .checksumSha256(VALID_SHA256)
            .storageBucket("bucket")
            .storageKey("key")
            .uploadedByUserId(userId)
            .build()
    );
  }

  @Test
  @DisplayName("Should reject file size exceeding 50MB with PayloadTooLargeException")
  void shouldRejectOversizedFile() {
    UUID userId = UUID.randomUUID();
    long oversized = 50L * 1024 * 1024 + 1; // 50MB + 1 byte

    assertThrows(PayloadTooLargeException.class, () ->
        Document.builder()
            .originalFileName("test.pdf")
            .title("Big Document")
            .fileType("PDF")
            .fileSizeBytes(oversized)
            .checksumSha256(VALID_SHA256)
            .storageBucket("bucket")
            .storageKey("key")
            .uploadedByUserId(userId)
            .build()
    );
  }

  @Test
  @DisplayName("Should reject invalid checksum SHA-256 length")
  void shouldRejectInvalidChecksum() {
    UUID userId = UUID.randomUUID();

    assertThrows(DocumentValidationException.class, () ->
        Document.builder()
            .originalFileName("test.pdf")
            .title("Test Document")
            .fileType("PDF")
            .fileSizeBytes(100L)
            .checksumSha256("short_hash")
            .storageBucket("bucket")
            .storageKey("key")
            .uploadedByUserId(userId)
            .build()
    );
  }

  @Test
  @DisplayName("Should update processing status correctly")
  void shouldUpdateProcessingStatus() {
    UUID userId = UUID.randomUUID();

    Document doc = Document.builder()
        .originalFileName("report.pdf")
        .title("Annual Report")
        .fileType("PDF")
        .fileSizeBytes(1024L)
        .checksumSha256(VALID_SHA256)
        .storageBucket("bucket")
        .storageKey("key")
        .uploadedByUserId(userId)
        .build();

    doc.updateProcessingStatus(ProcessingStatus.PARSING);
    assertEquals(ProcessingStatus.PARSING, doc.getProcessingStatus());
  }
}
