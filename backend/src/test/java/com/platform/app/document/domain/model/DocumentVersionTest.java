package com.platform.app.document.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.platform.app.document.domain.exception.DocumentValidationException;

class DocumentVersionTest {

  private static final String VALID_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

  @Test
  @DisplayName("Should successfully create DocumentVersion")
  void shouldCreateDocumentVersionSuccessfully() {
    UUID docId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    DocumentVersion version = DocumentVersion.builder()
        .documentId(docId)
        .versionNumber(1)
        .storageBucket("doc-knowledge-storage")
        .storageKey("documents/" + docId + "/v1/file.pdf")
        .fileSizeBytes(2048L)
        .checksumSha256(VALID_SHA256)
        .changeSummary("Initial upload")
        .uploadedByUserId(userId)
        .isS3Synced(true)
        .build();

    assertNotNull(version.getId());
    assertEquals(docId, version.getDocumentId());
    assertEquals(1, version.getVersionNumber());
    assertEquals("doc-knowledge-storage", version.getStorageBucket());
    assertEquals(2048L, version.getFileSizeBytes());
    assertEquals(VALID_SHA256, version.getChecksumSha256());
    assertTrue(version.isS3Synced());
    assertEquals("Initial upload", version.getChangeSummary());
    assertEquals(userId, version.getUploadedByUserId());
    assertNotNull(version.getCreatedAt());
  }

  @Test
  @DisplayName("Should reject invalid version number < 1")
  void shouldRejectInvalidVersionNumber() {
    UUID docId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    assertThrows(DocumentValidationException.class, () ->
        DocumentVersion.builder()
            .documentId(docId)
            .versionNumber(0)
            .storageBucket("bucket")
            .storageKey("key")
            .fileSizeBytes(100L)
            .checksumSha256(VALID_SHA256)
            .uploadedByUserId(userId)
            .build()
    );
  }

  @Test
  @DisplayName("Should reject null or invalid checksum SHA-256")
  void shouldRejectInvalidChecksumSha256() {
    UUID docId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    assertThrows(DocumentValidationException.class, () ->
        DocumentVersion.builder()
            .documentId(docId)
            .versionNumber(1)
            .storageBucket("bucket")
            .storageKey("key")
            .fileSizeBytes(100L)
            .checksumSha256("short")
            .uploadedByUserId(userId)
            .build()
    );
  }
}
