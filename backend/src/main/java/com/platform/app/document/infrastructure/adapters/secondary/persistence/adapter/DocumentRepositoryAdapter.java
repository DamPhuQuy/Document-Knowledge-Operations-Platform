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

  @Override
  public boolean softDelete(UUID id, java.time.Instant deletedAt) {
    return repository.softDeleteById(id, deletedAt) > 0;
  }

  public DocumentJpaEntity toEntity(Document domain) {
    if (domain == null) {
      return null;
    }
    return DocumentJpaEntity.builder()
        .id(domain.getId())
        .title(domain.getTitle())
        .originalFileName(domain.getOriginalFileName())
        .contentType(domain.getContentType())
        .fileSizeBytes(domain.getFileSizeBytes())
        .checksumSha256(domain.getChecksumSha256())
        .storageKey(domain.getStorageKey())
        .currentVersion(domain.getCurrentVersion())
        .status(domain.getStatus())
        .uploadedByUserId(domain.getUploadedByUserId())
        .departmentId(domain.getDepartmentId())
        .accessLevel(domain.getAccessLevel())
        .createdAt(domain.getCreatedAt())
        .updatedAt(domain.getUpdatedAt())
        .build();
  }

  public Document toDomain(DocumentJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return Document.builder()
        .id(entity.getId())
        .title(entity.getTitle())
        .originalFileName(entity.getOriginalFileName())
        .contentType(entity.getContentType())
        .fileSizeBytes(entity.getFileSizeBytes())
        .checksumSha256(entity.getChecksumSha256())
        .storageKey(entity.getStorageKey())
        .currentVersion(entity.getCurrentVersion())
        .status(entity.getStatus())
        .uploadedByUserId(entity.getUploadedByUserId())
        .departmentId(entity.getDepartmentId())
        .accessLevel(entity.getAccessLevel())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }
}
