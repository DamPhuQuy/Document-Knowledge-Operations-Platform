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
  private String title;

  @ToString.Include
  private String originalFileName;

  @ToString.Include
  private String contentType;

  private long fileSizeBytes;

  private String checksumSha256; // avoid duplicating the file content, used for integrity check

  private String storageKey;

  @Builder.Default
  private int currentVersion = 1;

  @Builder.Default
  private DocumentStatus status = DocumentStatus.UPLOADED;

  private final UUID uploadedByUserId;

  private UUID departmentId;

  @Builder.Default
  private AccessLevel accessLevel = AccessLevel.INTERNAL;

  private final Instant createdAt;

  private Instant updatedAt;

  public Document(
      UUID id,
      String title,
      String originalFileName,
      String contentType,
      long fileSizeBytes,
      String checksumSha256,
      String storageKey,
      int currentVersion,
      DocumentStatus status,
      UUID uploadedByUserId,
      UUID departmentId,
      AccessLevel accessLevel,
      Instant createdAt,
      Instant updatedAt) {

    this.id = id != null ? id : UUID.randomUUID();

    if (originalFileName == null || originalFileName.trim().isEmpty()) {
      throw new DocumentValidationException("Original file name must not be blank");
    }
    this.originalFileName = originalFileName.trim();

    if (title != null && !title.trim().isEmpty()) {
      this.title = title.trim();
    } else {
      this.title = this.originalFileName;
    }

    if (contentType == null || contentType.trim().isEmpty()) {
      throw new DocumentValidationException("Content type must not be blank");
    }
    this.contentType = contentType.trim().toLowerCase();

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

    if (storageKey == null || storageKey.trim().isEmpty()) {
      throw new DocumentValidationException("Storage key must not be blank");
    }
    this.storageKey = storageKey.trim();

    if (currentVersion < 1) {
      throw new DocumentValidationException("Current version must be at least 1");
    }
    this.currentVersion = currentVersion;

    this.uploadedByUserId = Objects.requireNonNull(uploadedByUserId, "Uploaded by user ID must not be null");
    this.departmentId = departmentId;
    this.accessLevel = accessLevel != null ? accessLevel : AccessLevel.INTERNAL;
    this.status = status != null ? status : DocumentStatus.UPLOADED;
    this.createdAt = createdAt != null ? createdAt : Instant.now();
    this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
  }

  public void applyNewVersion(
      int newVersion,
      String storageKey,
      String checksumSha256,
      long fileSizeBytes,
      String contentType,
      String originalFileName) {

    if (newVersion <= this.currentVersion) {
      throw new DocumentValidationException("New version number must be strictly greater than current version " + this.currentVersion);
    }
    if (storageKey == null || storageKey.trim().isEmpty()) {
      throw new DocumentValidationException("Storage key must not be blank");
    }
    if (checksumSha256 == null || checksumSha256.trim().length() != 64) {
      throw new DocumentValidationException("Checksum SHA-256 must be a 64-character hex string");
    }
    if (fileSizeBytes <= 0) {
      throw new DocumentValidationException("File size must be strictly positive");
    }
    if (fileSizeBytes > MAX_FILE_SIZE_BYTES) {
      throw new PayloadTooLargeException("File size exceeds maximum allowed 50MB limit");
    }

    this.currentVersion = newVersion;
    this.storageKey = storageKey.trim();
    this.checksumSha256 = checksumSha256.trim().toLowerCase();
    this.fileSizeBytes = fileSizeBytes;
    if (contentType != null && !contentType.trim().isEmpty()) {
      this.contentType = contentType.trim().toLowerCase();
    }
    if (originalFileName != null && !originalFileName.trim().isEmpty()) {
      this.originalFileName = originalFileName.trim();
    }
    this.status = DocumentStatus.UPLOADED;
    this.updatedAt = Instant.now();
  }

  public void markProcessing() {
    this.status = DocumentStatus.PROCESSING;
    this.updatedAt = Instant.now();
  }

  public void markReady() {
    this.status = DocumentStatus.READY;
    this.updatedAt = Instant.now();
  }

  public void markFailed() {
    this.status = DocumentStatus.FAILED;
    this.updatedAt = Instant.now();
  }

  public void updateStatus(DocumentStatus newStatus) {
    this.status = Objects.requireNonNull(newStatus, "Document status must not be null");
    this.updatedAt = Instant.now();
  }
}
