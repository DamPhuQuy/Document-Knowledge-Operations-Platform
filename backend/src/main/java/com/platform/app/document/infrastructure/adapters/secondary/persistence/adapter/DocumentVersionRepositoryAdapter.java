package com.platform.app.document.infrastructure.adapters.secondary.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.platform.app.document.application.ports.outbound.DocumentVersionRepositoryPort;
import com.platform.app.document.domain.model.DocumentVersion;
import com.platform.app.document.infrastructure.adapters.secondary.persistence.entity.DocumentVersionJpaEntity;
import com.platform.app.document.infrastructure.adapters.secondary.persistence.repository.SpringDataDocumentVersionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DocumentVersionRepositoryAdapter implements DocumentVersionRepositoryPort {

  private final SpringDataDocumentVersionRepository repository;

  @Override
  public DocumentVersion save(DocumentVersion version) {
    DocumentVersionJpaEntity entity = toEntity(version);
    DocumentVersionJpaEntity saved = repository.save(entity);
    return toDomain(saved);
  }

  @Override
  public List<DocumentVersion> findByDocumentId(UUID documentId) {
    return repository.findByDocumentIdOrderByVersionNumberDesc(documentId)
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public Optional<DocumentVersion> findByDocumentIdAndVersionNumber(UUID documentId, int versionNumber) {
    return repository.findByDocumentIdAndVersionNumber(documentId, versionNumber)
        .map(this::toDomain);
  }

  public DocumentVersionJpaEntity toEntity(DocumentVersion domain) {
    if (domain == null) {
      return null;
    }
    return DocumentVersionJpaEntity.builder()
        .id(domain.getId())
        .documentId(domain.getDocumentId())
        .versionNumber(domain.getVersionNumber())
        .storageBucket(domain.getStorageBucket())
        .storageKey(domain.getStorageKey())
        .fileSizeBytes(domain.getFileSizeBytes())
        .checksumSha256(domain.getChecksumSha256())
        .isS3Synced(domain.isS3Synced())
        .changeSummary(domain.getChangeSummary())
        .uploadedByUserId(domain.getUploadedByUserId())
        .createdAt(domain.getCreatedAt())
        .build();
  }

  public DocumentVersion toDomain(DocumentVersionJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return DocumentVersion.builder()
        .id(entity.getId())
        .documentId(entity.getDocumentId())
        .versionNumber(entity.getVersionNumber())
        .storageBucket(entity.getStorageBucket())
        .storageKey(entity.getStorageKey())
        .fileSizeBytes(entity.getFileSizeBytes())
        .checksumSha256(entity.getChecksumSha256())
        .isS3Synced(entity.isS3Synced())
        .changeSummary(entity.getChangeSummary())
        .uploadedByUserId(entity.getUploadedByUserId())
        .createdAt(entity.getCreatedAt())
        .build();
  }
}
