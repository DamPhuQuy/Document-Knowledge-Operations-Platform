package com.platform.app.document.infrastructure.adapters.secondary.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.platform.app.document.application.ports.outbound.DocumentRepositoryPort;
import com.platform.app.document.domain.model.Document;
import com.platform.app.document.infrastructure.adapters.secondary.persistence.entity.DocumentJpaEntity;
import com.platform.app.document.infrastructure.adapters.secondary.persistence.repository.SpringDataDocumentRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DocumentRepositoryAdapter implements DocumentRepositoryPort {

  private final SpringDataDocumentRepository repository;

  @Override
  public Document save(Document document) {
    DocumentJpaEntity entity = toEntity(document);
    DocumentJpaEntity saved = repository.save(entity);
    return toDomain(saved);
  }

  @Override
  public Optional<Document> findById(UUID id) {
    return repository.findByIdAndDeletedAtIsNull(id).map(this::toDomain);
  }

  public DocumentJpaEntity toEntity(Document domain) {
    if (domain == null) {
      return null;
    }
    return DocumentJpaEntity.builder()
        .id(domain.getId())
        .originalFileName(domain.getOriginalFileName())
        .title(domain.getTitle())
        .description(domain.getDescription())
        .fileType(domain.getFileType())
        .mimeType(domain.getMimeType())
        .fileSizeBytes(domain.getFileSizeBytes())
        .checksumSha256(domain.getChecksumSha256())
        .storageBucket(domain.getStorageBucket())
        .storageKey(domain.getStorageKey())
        .isS3Synced(domain.isS3Synced())
        .processingStatus(domain.getProcessingStatus())
        .currentVersion(domain.getCurrentVersion())
        .departmentId(domain.getDepartmentId())
        .uploadedByUserId(domain.getUploadedByUserId())
        .accessLevel(domain.getAccessLevel())
        .metadata(domain.getMetadata())
        .createdAt(domain.getCreatedAt())
        .updatedAt(domain.getUpdatedAt())
        .deletedAt(domain.getDeletedAt())
        .build();
  }

  public Document toDomain(DocumentJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return Document.builder()
        .id(entity.getId())
        .originalFileName(entity.getOriginalFileName())
        .title(entity.getTitle())
        .description(entity.getDescription())
        .fileType(entity.getFileType())
        .mimeType(entity.getMimeType())
        .fileSizeBytes(entity.getFileSizeBytes())
        .checksumSha256(entity.getChecksumSha256())
        .storageBucket(entity.getStorageBucket())
        .storageKey(entity.getStorageKey())
        .isS3Synced(entity.isS3Synced())
        .processingStatus(entity.getProcessingStatus())
        .currentVersion(entity.getCurrentVersion())
        .departmentId(entity.getDepartmentId())
        .uploadedByUserId(entity.getUploadedByUserId())
        .accessLevel(entity.getAccessLevel())
        .metadata(entity.getMetadata())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .deletedAt(entity.getDeletedAt())
        .build();
  }
}
