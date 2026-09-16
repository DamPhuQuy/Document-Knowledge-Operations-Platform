package com.platform.app.document.application.services;

import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.platform.app.document.application.dto.DocumentVersionResponseDto;
import com.platform.app.document.application.dto.UploadDocumentVersionCommand;
import com.platform.app.document.application.event.DocumentVersionCreatedEvent;
import com.platform.app.document.application.ports.inbound.StoreVersionMetadataUseCase;
import com.platform.app.document.application.ports.inbound.UploadDocumentVersionUseCase;
import com.platform.app.document.application.ports.outbound.DocumentRepositoryPort;
import com.platform.app.document.application.ports.outbound.ObjectStoragePort;
import com.platform.app.document.domain.exception.DocumentAccessDeniedException;
import com.platform.app.document.domain.exception.DocumentNotFoundException;
import com.platform.app.document.domain.exception.DocumentValidationException;
import com.platform.app.document.domain.exception.PayloadTooLargeException;
import com.platform.app.document.domain.exception.UnsupportedMediaTypeException;
import com.platform.app.document.domain.model.Document;
import com.platform.app.document.domain.model.DocumentVersion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentVersionService implements UploadDocumentVersionUseCase {

  private static final Set<String> ALLOWED_EXTENSIONS = Set.of("PDF", "DOCX", "TXT", "XLSX");

  private final DocumentRepositoryPort documentRepositoryPort;
  private final ObjectStoragePort objectStoragePort;
  private final StoreVersionMetadataUseCase storeVersionMetadataUseCase;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  public DocumentVersionResponseDto uploadVersion(UploadDocumentVersionCommand command) {
    Objects.requireNonNull(command, "Upload version command must not be null");
    Objects.requireNonNull(command.getDocumentId(), "Document ID must not be null");
    Objects.requireNonNull(command.getUserId(), "User ID must not be null");
    Objects.requireNonNull(command.getInputStream(), "Input stream must not be null");

    String originalFileName = command.getOriginalFileName();
    if (originalFileName == null || originalFileName.trim().isEmpty()) {
      throw new DocumentValidationException("Original file name must not be blank");
    }

    String extension = getFileExtension(originalFileName);
    if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toUpperCase())) {
      throw new UnsupportedMediaTypeException("Unsupported file type: " + extension + ". Allowed types: pdf, docx, txt, xlsx");
    }

    if (command.getFileSize() <= 0) {
      throw new DocumentValidationException("File size must be strictly positive");
    }
    if (command.getFileSize() > Document.MAX_FILE_SIZE_BYTES) {
      throw new PayloadTooLargeException("File size exceeds maximum allowed 50MB limit");
    }

    Document document = documentRepositoryPort.findById(command.getDocumentId())
        .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + command.getDocumentId()));

    // Verify ownership or ADMIN role
    if (!document.getUploadedByUserId().equals(command.getUserId()) && !command.isAdmin()) {
      log.warn("Access denied for user {} attempting to upload new version for document {} owned by {}",
          command.getUserId(), document.getId(), document.getUploadedByUserId());
      throw new DocumentAccessDeniedException("User does not have permission to upload a new version for document " + command.getDocumentId());
    }

    int nextVersion = document.getCurrentVersion() + 1;
    String sanitizedFileName = sanitizeFileName(originalFileName);
    String storageKey = "documents/" + document.getId() + "/v" + nextVersion + "/" + sanitizedFileName;
    String contentType = command.getContentType() != null ? command.getContentType() : "application/octet-stream";

    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available in JVM", e);
    }

    // Single-pass streaming upload wrapping input stream with DigestInputStream
    DigestInputStream digestStream = new DigestInputStream(command.getInputStream(), digest);

    log.info("Starting streaming S3 upload for documentId={}, version={}, key={}, size={}",
        document.getId(), nextVersion, storageKey, command.getFileSize());
    objectStoragePort.upload(storageKey, digestStream, command.getFileSize(), contentType);

    String checksumSha256 = HexFormat.of().formatHex(digest.digest());
    log.info("Computed SHA-256 for documentId={}, version={}: {}", document.getId(), nextVersion, checksumSha256);

    UUID versionId = UUID.randomUUID();
    DocumentVersion version = DocumentVersion.builder()
        .id(versionId)
        .documentId(document.getId())
        .versionNumber(nextVersion)
        .storageKey(storageKey)
        .fileSizeBytes(command.getFileSize())
        .checksumSha256(checksumSha256)
        .changeSummary(command.getChangeSummary())
        .uploadedByUserId(command.getUserId())
        .createdAt(Instant.now())
        .build();

    // Persist in @Transactional boundary with S3 compensation on failure
    DocumentVersion savedVersion;
    try {
      savedVersion = storeVersionMetadataUseCase.persistVersionMetadata(
          document, version, nextVersion, storageKey, checksumSha256, command.getFileSize(), contentType, sanitizedFileName);
    } catch (Exception e) {
      log.error("Failed to persist document version metadata for docId={}, version={}. Executing S3 compensation delete for key: {}",
          document.getId(), nextVersion, storageKey, e);
      try {
        objectStoragePort.delete(storageKey);
      } catch (Exception delEx) {
        log.error("S3 compensation delete failed for key: {}", storageKey, delEx);
      }
      throw e;
    }

    // Emit domain event for re-indexing and audit logging
    eventPublisher.publishEvent(DocumentVersionCreatedEvent.builder()
        .versionId(savedVersion.getId())
        .documentId(savedVersion.getDocumentId())
        .versionNumber(savedVersion.getVersionNumber())
        .storageKey(savedVersion.getStorageKey())
        .fileSizeBytes(savedVersion.getFileSizeBytes())
        .checksumSha256(savedVersion.getChecksumSha256())
        .changeSummary(savedVersion.getChangeSummary())
        .uploadedByUserId(savedVersion.getUploadedByUserId())
        .timestamp(Instant.now())
        .build());

    return DocumentVersionResponseDto.fromDomain(savedVersion);
  }

  private String getFileExtension(String fileName) {
    if (fileName == null) {
      return "file";
    }
    int lastDot = fileName.lastIndexOf('.');
    if (lastDot == -1 || lastDot == fileName.length() - 1) {
      return "";
    }
    return fileName.substring(lastDot + 1);
  }

  private String sanitizeFileName(String fileName) {
    if (fileName == null) {
      return "file";
    }
    return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
  }
}
