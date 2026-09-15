package com.platform.app.document.application.services;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.platform.app.document.application.dto.UploadDocumentCommand;
import com.platform.app.document.application.ports.inbound.StoreMetadataUseCase;
import com.platform.app.document.application.ports.outbound.DocumentRepositoryPort;
import com.platform.app.document.application.ports.outbound.DocumentVersionRepositoryPort;
import com.platform.app.document.domain.model.AccessLevel;
import com.platform.app.document.domain.model.Document;
import com.platform.app.document.domain.model.DocumentVersion;
import com.platform.app.document.domain.model.ProcessingStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentMetadataService implements StoreMetadataUseCase {

    private final DocumentRepositoryPort documentRepositoryPort;
    private final DocumentVersionRepositoryPort documentVersionRepositoryPort;

    @Transactional
    @Override
    public Document persistMetadata(
      UUID docId,
      UploadDocumentCommand command,
      String sanitizedFileName,
      String storageKey,
      String bucket,
      String checksumSha256,
      String fileType) {

        String title = (command.getTitle() != null && !command.getTitle().trim().isEmpty())
            ? command.getTitle().trim()
            : sanitizedFileName;

        AccessLevel accessLevel = command.getAccessLevel() != null ? command.getAccessLevel() : AccessLevel.INTERNAL;

        Document document = Document.builder()
            .id(docId)
            .originalFileName(command.getOriginalFileName())
            .title(title)
            .description(command.getDescription())
            .fileType(fileType)
            .mimeType(command.getContentType() != null ? command.getContentType() : "application/octet-stream")
            .fileSizeBytes(command.getFileSize())
            .checksumSha256(checksumSha256)
            .storageBucket(bucket)
            .storageKey(storageKey)
            .isS3Synced(true)
            .processingStatus(ProcessingStatus.UPLOADED)
            .currentVersion(1)
            .departmentId(command.getDepartmentId())
            .uploadedByUserId(command.getUserId())
            .accessLevel(accessLevel)
            .metadata("{}")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        Document saved = documentRepositoryPort.save(document);

        DocumentVersion version = DocumentVersion.builder()
            .documentId(saved.getId())
            .versionNumber(1)
            .storageBucket(bucket)
            .storageKey(storageKey)
            .fileSizeBytes(command.getFileSize())
            .checksumSha256(checksumSha256)
            .isS3Synced(true)
            .changeSummary("Initial upload")
            .uploadedByUserId(command.getUserId())
            .createdAt(saved.getCreatedAt())
            .build();

        documentVersionRepositoryPort.save(version);
        return saved;
    }
}
