package com.platform.app.document.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        .contentType("application/pdf")
        .fileSizeBytes(1024L)
        .checksumSha256(VALID_SHA256)
        .storageKey("documents/123/report.pdf")
        .departmentId(deptId)
        .uploadedByUserId(userId)
        .build();

    assertNotNull(doc.getId());
    assertEquals("Annual Report", doc.getTitle());
    assertEquals("report.pdf", doc.getOriginalFileName());
    assertEquals("application/pdf", doc.getContentType());
    assertEquals(1024L, doc.getFileSizeBytes());
    assertEquals(VALID_SHA256, doc.getChecksumSha256());
    assertEquals("documents/123/report.pdf", doc.getStorageKey());
    assertEquals(AccessLevel.INTERNAL, doc.getAccessLevel()); // Invariant B2
    assertEquals(DocumentStatus.UPLOADED, doc.getStatus());
    assertEquals(userId, doc.getUploadedByUserId());
    assertEquals(deptId, doc.getDepartmentId());
    assertNotNull(doc.getCreatedAt());
    assertNotNull(doc.getUpdatedAt());
  }

  @Test
  @DisplayName("Should fallback to originalFileName if title is null or blank")
  void shouldFallbackTitleToOriginalFileName() {
    UUID userId = UUID.randomUUID();

    Document doc = Document.builder()
        .originalFileName("test.pdf")
        .title("   ")
        .contentType("application/pdf")
        .fileSizeBytes(500L)
        .checksumSha256(VALID_SHA256)
        .storageKey("key")
        .uploadedByUserId(userId)
        .build();

    assertEquals("test.pdf", doc.getTitle());
  }

  @Test
  @DisplayName("Should reject blank or null originalFileName")
  void shouldRejectBlankOriginalFileName() {
    UUID userId = UUID.randomUUID();

    assertThrows(DocumentValidationException.class, () ->
        Document.builder()
            .originalFileName("   ")
            .title("Test Document")
            .contentType("application/pdf")
            .fileSizeBytes(500L)
            .checksumSha256(VALID_SHA256)
            .storageKey("key")
            .uploadedByUserId(userId)
            .build()
    );
  }

  @Test
  @DisplayName("Should reject blank or null contentType")
  void shouldRejectBlankContentType() {
    UUID userId = UUID.randomUUID();

    assertThrows(DocumentValidationException.class, () ->
        Document.builder()
            .originalFileName("test.pdf")
            .title("Test Document")
            .contentType("   ")
            .fileSizeBytes(500L)
            .checksumSha256(VALID_SHA256)
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
            .contentType("application/pdf")
            .fileSizeBytes(0L)
            .checksumSha256(VALID_SHA256)
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
            .contentType("application/pdf")
            .fileSizeBytes(oversized)
            .checksumSha256(VALID_SHA256)
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
            .contentType("application/pdf")
            .fileSizeBytes(100L)
            .checksumSha256("short_hash")
            .storageKey("key")
            .uploadedByUserId(userId)
            .build()
    );
  }

  @Test
  @DisplayName("Should update status correctly via state-transition methods")
  void shouldUpdateStatusCorrectly() {
    UUID userId = UUID.randomUUID();

    Document doc = Document.builder()
        .originalFileName("report.pdf")
        .contentType("application/pdf")
        .fileSizeBytes(1024L)
        .checksumSha256(VALID_SHA256)
        .storageKey("key")
        .uploadedByUserId(userId)
        .build();

    assertEquals(DocumentStatus.UPLOADED, doc.getStatus());

    doc.markProcessing();
    assertEquals(DocumentStatus.PROCESSING, doc.getStatus());

    doc.markReady();
    assertEquals(DocumentStatus.READY, doc.getStatus());

    doc.markFailed();
    assertEquals(DocumentStatus.FAILED, doc.getStatus());

    doc.updateStatus(DocumentStatus.PROCESSING);
    assertEquals(DocumentStatus.PROCESSING, doc.getStatus());
  }

  @Test
  @DisplayName("Should initialize currentVersion to 1 by default and allow valid revision application")
  void shouldApplyNewVersionSuccessfully() {
    UUID userId = UUID.randomUUID();

    Document doc = Document.builder()
        .originalFileName("report_v1.pdf")
        .contentType("application/pdf")
        .fileSizeBytes(1024L)
        .checksumSha256(VALID_SHA256)
        .storageKey("documents/123/v1/report.pdf")
        .uploadedByUserId(userId)
        .build();

    assertEquals(1, doc.getCurrentVersion());

    String newSha256 = "a" + VALID_SHA256.substring(1);
    doc.applyNewVersion(2, "documents/123/v2/report_v2.pdf", newSha256, 2048L, "application/pdf", "report_v2.pdf");

    assertEquals(2, doc.getCurrentVersion());
    assertEquals("documents/123/v2/report_v2.pdf", doc.getStorageKey());
    assertEquals(newSha256, doc.getChecksumSha256());
    assertEquals(2048L, doc.getFileSizeBytes());
    assertEquals("report_v2.pdf", doc.getOriginalFileName());
    assertEquals(DocumentStatus.UPLOADED, doc.getStatus());
  }

  @Test
  @DisplayName("Should reject applying new version with number not greater than current version")
  void shouldRejectLesserOrEqualVersionNumber() {
    UUID userId = UUID.randomUUID();

    Document doc = Document.builder()
        .originalFileName("report_v1.pdf")
        .contentType("application/pdf")
        .fileSizeBytes(1024L)
        .checksumSha256(VALID_SHA256)
        .storageKey("documents/123/v1/report.pdf")
        .uploadedByUserId(userId)
        .build();

    assertThrows(DocumentValidationException.class, () ->
        doc.applyNewVersion(1, "key", VALID_SHA256, 100L, "application/pdf", "name.pdf")
    );
  }
}
