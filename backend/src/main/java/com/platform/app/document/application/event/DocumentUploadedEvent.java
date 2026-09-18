package com.platform.app.document.application.event;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocumentUploadedEvent {

  private final UUID documentId;
  private final String title;
  private final String originalFileName;
  private final String contentType;
  private final long fileSizeBytes;
  private final String checksumSha256;
  private final String storageKey;
  private final UUID departmentId;
  private final UUID uploadedByUserId;
  private final Instant timestamp;
}
