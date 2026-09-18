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

import com.platform.app.document.application.dto.DocumentResponseDto;
import com.platform.app.document.application.dto.UploadDocumentCommand;
import com.platform.app.document.application.event.DocumentUploadedEvent;
import com.platform.app.document.application.ports.inbound.StoreMetadataUseCase;
import com.platform.app.document.application.ports.inbound.UploadDocumentUseCase;
import com.platform.app.document.application.ports.outbound.ObjectStoragePort;
import com.platform.app.document.domain.exception.DocumentValidationException;
import com.platform.app.document.domain.exception.PayloadTooLargeException;
import com.platform.app.document.domain.exception.UnsupportedMediaTypeException;
import com.platform.app.document.domain.model.Document;
import com.platform.app.shared.util.IdGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentUploadService implements UploadDocumentUseCase {

  private static final Set<String> ALLOWED_EXTENSIONS = Set.of("PDF", "DOCX", "TXT", "XLSX");

  private final ObjectStoragePort objectStoragePort;
  private final StoreMetadataUseCase storeMetadataUseCase;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  public DocumentResponseDto uploadDocument(UploadDocumentCommand command) {
    Objects.requireNonNull(command, "Upload command must not be null");
    Objects.requireNonNull(command.getInputStream(), "Input stream must not be null");
    Objects.requireNonNull(command.getUserId(), "User ID must not be null");

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

    UUID docId = IdGenerator.nextId();
    String sanitizedFileName = sanitizeFileName(originalFileName);
    String storageKey = "documents/" + docId + "/" + sanitizedFileName;
    String contentType = command.getContentType() != null ? command.getContentType() : "application/octet-stream";

    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available in JVM", e);
    }

    // Single-pass streaming: wrap stream with DigestInputStream
    DigestInputStream digestStream = new DigestInputStream(command.getInputStream(), digest);

    log.info("Starting streaming S3 upload for documentId={}, key={}, size={}", docId, storageKey, command.getFileSize());
    objectStoragePort.upload(storageKey, digestStream, command.getFileSize(), contentType);

    // Extract calculated checksum after full stream read
    String checksumSha256 = HexFormat.of().formatHex(digest.digest());
    log.info("Computed SHA-256 for documentId={}: {}", docId, checksumSha256);

    // Coordinate DB transaction with S3 compensation on failure
    Document savedDocument;
    try {
      savedDocument = storeMetadataUseCase.persistMetadata(docId, command, sanitizedFileName, storageKey, checksumSha256, contentType);
    } catch (Exception e) {
      log.error("Failed to persist document metadata for docId={}. Executing S3 compensation delete for key: {}",
          docId, storageKey, e);
      try {
        objectStoragePort.delete(storageKey);
      } catch (Exception delEx) {
        log.error("S3 compensation delete failed for key: {}", storageKey, delEx);
      }
      throw e;
    }

    // publish event
    eventPublisher.publishEvent(DocumentUploadedEvent.builder()
        .documentId(savedDocument.getId())
        .title(savedDocument.getTitle())
        .originalFileName(savedDocument.getOriginalFileName())
        .contentType(savedDocument.getContentType())
        .fileSizeBytes(savedDocument.getFileSizeBytes())
        .checksumSha256(checksumSha256)
        .storageKey(storageKey)
        .departmentId(savedDocument.getDepartmentId())
        .uploadedByUserId(savedDocument.getUploadedByUserId())
        .timestamp(Instant.now())
        .build());

    return DocumentResponseDto.fromDomain(savedDocument);
  }

  private String getFileExtension(String fileName) {
    if (fileName == null) {
      return "file";
    }

    int lastDotIndex = fileName.lastIndexOf(".");

    if (lastDotIndex == -1) {
      return null;
    }

    return fileName.substring(lastDotIndex + 1);
  }

  private String sanitizeFileName(String fileName) {
    if (fileName == null) {
      return "file";
    }
    // Strip path traversals and illegal characters
    String clean = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    return clean.isEmpty() ? "file" : clean;
  }
}
