package com.platform.app.document.infrastructure.adapters.secondary.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.platform.app.document.domain.model.DocumentVersion;
import com.platform.app.document.infrastructure.adapters.secondary.persistence.entity.DocumentVersionJpaEntity;
import com.platform.app.document.infrastructure.adapters.secondary.persistence.repository.SpringDataDocumentVersionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentVersionRepositoryAdapterTest {

    @Mock
    private SpringDataDocumentVersionRepository repository;

    private DocumentVersionRepositoryAdapter adapter;

    private static final String SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    @BeforeEach
    void setUp() {
        adapter = new DocumentVersionRepositoryAdapter(repository);
    }

    @Test
    @DisplayName("Should save and map entity to domain")
    void shouldSaveAndMapToDomain() {
        UUID docId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        DocumentVersion version = DocumentVersion.builder()
            .id(versionId)
            .documentId(docId)
            .versionNumber(1)
            .storageKey("storage/key")
            .fileSizeBytes(100L)
            .checksumSha256(SHA256)
            .changeSummary("Initial")
            .uploadedByUserId(userId)
            .createdAt(Instant.now())
            .build();

        DocumentVersionJpaEntity entity = adapter.toEntity(version);
        when(repository.save(any(DocumentVersionJpaEntity.class))).thenReturn(entity);

        DocumentVersion saved = adapter.save(version);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(versionId);
        assertThat(saved.getVersionNumber()).isEqualTo(1);
        verify(repository).save(any(DocumentVersionJpaEntity.class));
    }

    @Test
    @DisplayName("Should find versions by document id ordered by version desc")
    void shouldFindVersionsByDocumentId() {
        UUID docId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        DocumentVersionJpaEntity entity = DocumentVersionJpaEntity.builder()
            .id(versionId)
            .documentId(docId)
            .versionNumber(2)
            .storageKey("key-2")
            .fileSizeBytes(500L)
            .checksumSha256(SHA256)
            .changeSummary("Updated")
            .uploadedByUserId(userId)
            .createdAt(Instant.now())
            .build();

        when(repository.findByDocumentIdOrderByVersionNumberDesc(docId)).thenReturn(List.of(entity));

        List<DocumentVersion> versions = adapter.findByDocumentId(docId);
        assertThat(versions).hasSize(1);
        assertThat(versions.get(0).getVersionNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should find specific version by document id and version number")
    void shouldFindSpecificVersion() {
        UUID docId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        DocumentVersionJpaEntity entity = DocumentVersionJpaEntity.builder()
            .id(versionId)
            .documentId(docId)
            .versionNumber(1)
            .storageKey("key-1")
            .fileSizeBytes(200L)
            .checksumSha256(SHA256)
            .uploadedByUserId(UUID.randomUUID())
            .createdAt(Instant.now())
            .build();

        when(repository.findByDocumentIdAndVersionNumber(docId, 1)).thenReturn(Optional.of(entity));

        Optional<DocumentVersion> result = adapter.findByDocumentIdAndVersionNumber(docId, 1);
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(versionId);
    }

    @Test
    @DisplayName("Should return null when mapping null entity or domain")
    void shouldReturnNullWhenMappingNull() {
        assertThat(adapter.toEntity(null)).isNull();
        assertThat(adapter.toDomain(null)).isNull();
    }
}
