package com.platform.app.document.infrastructure.adapters.secondary.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.platform.app.document.domain.model.AccessLevel;
import com.platform.app.document.domain.model.Document;
import com.platform.app.document.domain.model.DocumentVersion;
import com.platform.app.document.domain.model.ProcessingStatus;
import com.platform.app.document.infrastructure.adapters.secondary.persistence.entity.DocumentJpaEntity;
import com.platform.app.document.infrastructure.adapters.secondary.persistence.entity.DocumentVersionJpaEntity;
import com.platform.app.document.infrastructure.adapters.secondary.persistence.repository.SpringDataDocumentRepository;
import com.platform.app.document.infrastructure.adapters.secondary.persistence.repository.SpringDataDocumentVersionRepository;

@ExtendWith(MockitoExtension.class)
class DocumentRepositoryAdapterTest {

  private static final String SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

  @Mock
  private SpringDataDocumentRepository documentRepository;

  @Mock
  private SpringDataDocumentVersionRepository versionRepository;

  private DocumentRepositoryAdapter documentAdapter;
  private DocumentVersionRepositoryAdapter versionAdapter;

  @BeforeEach
  void setUp() {
    documentAdapter = new DocumentRepositoryAdapter(documentRepository);
    versionAdapter = new DocumentVersionRepositoryAdapter(versionRepository);
  }

  @Test
  @DisplayName("Should save and map Document between domain and JPA entity")
  void shouldSaveAndMapDocumentCorrectly() {
    UUID docId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID deptId = UUID.randomUUID();
    Instant now = Instant.now();

    Document domain = Document.builder()
        .id(docId)
        .originalFileName("report.pdf")
        .title("Annual Report")
        .description("Description")
        .fileType("PDF")
        .mimeType("application/pdf")
        .fileSizeBytes(2048L)
        .checksumSha256(SHA256)
        .storageBucket("doc-knowledge-storage")
        .storageKey("documents/" + docId + "/v1/report.pdf")
        .departmentId(deptId)
        .uploadedByUserId(userId)
        .accessLevel(AccessLevel.INTERNAL)
        .processingStatus(ProcessingStatus.UPLOADED)
        .currentVersion(1)
        .isS3Synced(true)
        .metadata("{}")
        .createdAt(now)
        .updatedAt(now)
        .build();

    DocumentJpaEntity entity = documentAdapter.toEntity(domain);
    when(documentRepository.save(any(DocumentJpaEntity.class))).thenReturn(entity);

    Document saved = documentAdapter.save(domain);

    assertNotNull(saved);
    assertEquals(docId, saved.getId());
    assertEquals("Annual Report", saved.getTitle());
    assertEquals("report.pdf", saved.getOriginalFileName());
    assertEquals("PDF", saved.getFileType());
    assertEquals("doc-knowledge-storage", saved.getStorageBucket());
    assertEquals("documents/" + docId + "/v1/report.pdf", saved.getStorageKey());
    assertEquals(AccessLevel.INTERNAL, saved.getAccessLevel());
    assertEquals(ProcessingStatus.UPLOADED, saved.getProcessingStatus());
    verify(documentRepository).save(any(DocumentJpaEntity.class));
  }

  @Test
  @DisplayName("Should find document by id and map to domain")
  void shouldFindDocumentByIdCorrectly() {
    UUID docId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    DocumentJpaEntity entity = DocumentJpaEntity.builder()
        .id(docId)
        .originalFileName("report.pdf")
        .title("Report")
        .fileType("PDF")
        .mimeType("application/pdf")
        .fileSizeBytes(100L)
        .checksumSha256(SHA256)
        .storageBucket("bucket")
        .storageKey("key")
        .isS3Synced(true)
        .processingStatus(ProcessingStatus.UPLOADED)
        .currentVersion(1)
        .uploadedByUserId(userId)
        .accessLevel(AccessLevel.INTERNAL)
        .metadata("{}")
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();

    when(documentRepository.findByIdAndDeletedAtIsNull(docId)).thenReturn(Optional.of(entity));

    Optional<Document> result = documentAdapter.findById(docId);
    assertTrue(result.isPresent());
    assertEquals(docId, result.get().getId());
  }

  @Test
  @DisplayName("Should save and map DocumentVersion between domain and JPA entity")
  void shouldSaveAndMapDocumentVersionCorrectly() {
    UUID versionId = UUID.randomUUID();
    UUID docId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Instant now = Instant.now();

    DocumentVersion domain = DocumentVersion.builder()
        .id(versionId)
        .documentId(docId)
        .versionNumber(1)
        .storageBucket("bucket")
        .storageKey("documents/" + docId + "/v1/report.pdf")
        .fileSizeBytes(1000L)
        .checksumSha256(SHA256)
        .isS3Synced(true)
        .changeSummary("Initial version")
        .uploadedByUserId(userId)
        .createdAt(now)
        .build();

    DocumentVersionJpaEntity entity = versionAdapter.toEntity(domain);
    when(versionRepository.save(any(DocumentVersionJpaEntity.class))).thenReturn(entity);

    DocumentVersion saved = versionAdapter.save(domain);

    assertNotNull(saved);
    assertEquals(versionId, saved.getId());
    assertEquals(docId, saved.getDocumentId());
    assertEquals(1, saved.getVersionNumber());
    assertEquals("Initial version", saved.getChangeSummary());
    assertEquals(SHA256, saved.getChecksumSha256());
    verify(versionRepository).save(any(DocumentVersionJpaEntity.class));
  }
}
