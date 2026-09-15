package com.platform.app.document.infrastructure.adapters.secondary.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.platform.app.document.domain.model.AccessLevel;
import com.platform.app.document.domain.model.ProcessingStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DocumentJpaEntity {

  @Id
  @EqualsAndHashCode.Include
  private UUID id;

  @Column(name = "original_file_name", nullable = false, length = 255)
  private String originalFileName;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "file_type", nullable = false, length = 50)
  private String fileType;

  @Column(name = "mime_type", nullable = false, length = 100)
  private String mimeType;

  @Column(name = "file_size_bytes", nullable = false)
  private Long fileSizeBytes;

  @Column(name = "checksum_sha256", nullable = false, length = 64)
  private String checksumSha256;

  @Column(name = "storage_bucket", nullable = false, length = 128)
  private String storageBucket;

  @Column(name = "storage_key", nullable = false, length = 512)
  private String storageKey;

  @Column(name = "is_s3_synced", nullable = false)
  private boolean isS3Synced;

  @Enumerated(EnumType.STRING)
  @Column(name = "processing_status", nullable = false, length = 50)
  private ProcessingStatus processingStatus;

  @Column(name = "current_version", nullable = false)
  private Integer currentVersion;

  @Column(name = "department_id")
  private UUID departmentId;

  @Column(name = "uploaded_by_user_id", nullable = false)
  private UUID uploadedByUserId;

  @Enumerated(EnumType.STRING)
  @Column(name = "access_level", nullable = false, length = 50)
  private AccessLevel accessLevel;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata")
  private String metadata;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;
}
