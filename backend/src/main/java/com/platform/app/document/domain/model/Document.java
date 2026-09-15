package com.platform.app.document.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.platform.app.document.domain.exception.DocumentValidationException;
import com.platform.app.document.domain.exception.PayloadTooLargeException;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
public class Document {

  public static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024; // 50MB

  @ToString.Include
  @EqualsAndHashCode.Include
  private final UUID id;

  @ToString.Include
  private final String originalFileName;

  @ToString.Include
  private String title;

  private String description;

  @ToString.Include
  private final String fileType;

  private final String mimeType;

  private final long fileSizeBytes;

  private final String checksumSha256;

  private final String storageBucket;

  private final String storageKey;

  @Builder.Default
  private boolean isS3Synced = true;

  @Builder.Default
  private ProcessingStatus processingStatus = ProcessingStatus.UPLOADED;

  @Builder.Default
  private int currentVersion = 1;

  private UUID departmentId;

  private final UUID uploadedByUserId;

  @Builder.Default
  private AccessLevel accessLevel = AccessLevel.INTERNAL;

  @Builder.Default
  private String metadata = "{}";

  private final Instant createdAt;

  private Instant updatedAt;

  private Instant deletedAt;

  public Document(
      UUID id,
      String originalFileName,
      String title,
      String description,
      String fileType,
      String mimeType,
      long fileSizeBytes,
      String checksumSha256,
      String storageBucket,
      String storageKey,
      boolean isS3Synced,
      ProcessingStatus processingStatus,
      int currentVersion,
      UUID departmentId,
      UUID uploadedByUserId,
      AccessLevel accessLevel,
      String metadata,
      Instant createdAt,
      Instant updatedAt,
      Instant deletedAt) {

    this.id = id != null ? id : UUID.randomUUID();

    if (title == null || title.trim().isEmpty()) {
      throw new DocumentValidationException("Document title must not be blank");
    }
    this.title = title.trim();

    if (originalFileName == null || originalFileName.trim().isEmpty()) {
      throw new DocumentValidationException("Original file name must not be blank");
    }
    this.originalFileName = originalFileName.trim();

    if (fileSizeBytes <= 0) {
      throw new DocumentValidationException("File size must be strictly positive");
    }
    if (fileSizeBytes > MAX_FILE_SIZE_BYTES) {
      throw new PayloadTooLargeException("File size exceeds maximum allowed 50MB limit");
    }
    this.fileSizeBytes = fileSizeBytes;

    if (checksumSha256 == null || checksumSha256.trim().length() != 64) {
      throw new DocumentValidationException("Checksum SHA-256 must be a 64-character hex string");
    }
    this.checksumSha256 = checksumSha256.trim().toLowerCase();

    if (storageBucket == null || storageBucket.trim().isEmpty()) {
      throw new DocumentValidationException("Storage bucket must not be blank");
    }
    this.storageBucket = storageBucket.trim();

    if (storageKey == null || storageKey.trim().isEmpty()) {
      throw new DocumentValidationException("Storage key must not be blank");
    }
    this.storageKey = storageKey.trim();

    this.uploadedByUserId = Objects.requireNonNull(uploadedByUserId, "Uploaded by user ID must not be null");

    this.description = description;
    this.fileType = fileType != null ? fileType.trim().toUpperCase() : "UNKNOWN";
    this.mimeType = mimeType != null ? mimeType.trim() : "application/octet-stream";
    this.isS3Synced = isS3Synced;
    this.processingStatus = processingStatus != null ? processingStatus : ProcessingStatus.UPLOADED;
    this.currentVersion = currentVersion > 0 ? currentVersion : 1;
    this.departmentId = departmentId;
    this.accessLevel = accessLevel != null ? accessLevel : AccessLevel.INTERNAL;
    this.metadata = (metadata != null && !metadata.trim().isEmpty()) ? metadata.trim() : "{}";
    this.createdAt = createdAt != null ? createdAt : Instant.now();
    this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    this.deletedAt = deletedAt;
  }

  public void updateProcessingStatus(ProcessingStatus status) {
    this.processingStatus = Objects.requireNonNull(status, "Processing status must not be null");
    this.updatedAt = Instant.now();
  }

  public void markDeleted() {
    this.deletedAt = Instant.now();
    this.updatedAt = this.deletedAt;
  }
}
