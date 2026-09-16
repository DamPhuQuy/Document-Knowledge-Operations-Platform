package com.platform.app.document.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.platform.app.document.domain.model.DocumentVersion;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocumentVersionResponseDto {

  private final UUID id;
  private final UUID documentId;
  private final int versionNumber;
  private final String storageKey;
  private final long fileSizeBytes;
  private final String checksumSha256;
  private final String changeSummary;
  private final UUID uploadedByUserId;
  private final Instant createdAt;

  public static DocumentVersionResponseDto fromDomain(DocumentVersion version) {
    if (version == null) {
      return null;
    }
    return DocumentVersionResponseDto.builder()
        .id(version.getId())
        .documentId(version.getDocumentId())
        .versionNumber(version.getVersionNumber())
        .storageKey(version.getStorageKey())
        .fileSizeBytes(version.getFileSizeBytes())
        .checksumSha256(version.getChecksumSha256())
        .changeSummary(version.getChangeSummary())
        .uploadedByUserId(version.getUploadedByUserId())
        .createdAt(version.getCreatedAt())
        .build();
  }
}
