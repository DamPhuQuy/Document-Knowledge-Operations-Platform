package com.platform.app.document.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.platform.app.document.domain.model.AccessLevel;
import com.platform.app.document.domain.model.Document;
import com.platform.app.document.domain.model.ProcessingStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocumentResponseDto {

  private final UUID id;
  private final String originalFileName;
  private final String title;
  private final String description;
  private final String fileType;
  private final String mimeType;
  private final long fileSizeBytes;
  private final String checksumSha256;
  private final AccessLevel accessLevel;
  private final ProcessingStatus processingStatus;
  private final int currentVersion;
  private final UUID departmentId;
  private final UUID uploadedByUserId;
  private final Instant createdAt;
  private final Instant updatedAt;

  public static DocumentResponseDto fromDomain(Document doc) {
    if (doc == null) {
      return null;
    }
    return DocumentResponseDto.builder()
        .id(doc.getId())
        .originalFileName(doc.getOriginalFileName())
        .title(doc.getTitle())
        .description(doc.getDescription())
        .fileType(doc.getFileType())
        .mimeType(doc.getMimeType())
        .fileSizeBytes(doc.getFileSizeBytes())
        .checksumSha256(doc.getChecksumSha256())
        .accessLevel(doc.getAccessLevel())
        .processingStatus(doc.getProcessingStatus())
        .currentVersion(doc.getCurrentVersion())
        .departmentId(doc.getDepartmentId())
        .uploadedByUserId(doc.getUploadedByUserId())
        .createdAt(doc.getCreatedAt())
        .updatedAt(doc.getUpdatedAt())
        .build();
  }
}
