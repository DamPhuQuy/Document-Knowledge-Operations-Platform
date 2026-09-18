package com.platform.app.document.application.services;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.platform.app.document.application.dto.UploadDocumentCommand;
import com.platform.app.document.application.ports.inbound.StoreMetadataUseCase;
import com.platform.app.document.application.ports.inbound.StoreVersionMetadataUseCase;
import com.platform.app.document.application.ports.outbound.DocumentRepositoryPort;
import com.platform.app.document.application.ports.outbound.DocumentVersionRepositoryPort;
import com.platform.app.document.domain.model.AccessLevel;
import com.platform.app.document.domain.model.Document;
import com.platform.app.document.domain.model.DocumentStatus;
import com.platform.app.document.domain.model.DocumentVersion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentMetadataService implements StoreMetadataUseCase, StoreVersionMetadataUseCase {

  private final DocumentRepositoryPort documentRepositoryPort;
  private final DocumentVersionRepositoryPort documentVersionRepositoryPort;

  @Transactional
  @Override
  public Document persistMetadata(
      UUID docId,
      UploadDocumentCommand command,
      String sanitizedFileName,
      String storageKey,
      String checksumSha256,
      String contentType) {

    String title = (command.getTitle() != null && !command.getTitle().trim().isEmpty())
        ? command.getTitle().trim()
        : sanitizedFileName;

    AccessLevel accessLevel = command.getAccessLevel() != null ? command.getAccessLevel() : AccessLevel.INTERNAL;

    Document document = Document.builder()
        .id(docId)
        .title(title)
        .originalFileName(command.getOriginalFileName())
        .contentType(contentType)
        .fileSizeBytes(command.getFileSize())
        .checksumSha256(checksumSha256)
        .storageKey(storageKey)
        .currentVersion(1)
        .status(DocumentStatus.UPLOADED)
        .uploadedByUserId(command.getUserId())
        .departmentId(command.getDepartmentId())
        .accessLevel(accessLevel)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();

    return documentRepositoryPort.save(document);
  }

  @Transactional
  @Override
  public DocumentVersion persistVersionMetadata(
      Document document,
      DocumentVersion version,
      int nextVersion,
      String storageKey,
      String checksumSha256,
      long fileSize,
      String contentType,
      String originalFileName) {

    DocumentVersion savedVersion = documentVersionRepositoryPort.save(version);
    document.applyNewVersion(nextVersion, storageKey, checksumSha256, fileSize, contentType, originalFileName);
    documentRepositoryPort.save(document);

    return savedVersion;
  }
}
