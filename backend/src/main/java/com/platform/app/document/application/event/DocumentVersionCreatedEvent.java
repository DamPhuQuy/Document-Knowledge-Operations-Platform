package com.platform.app.document.application.event;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocumentVersionCreatedEvent {

  private final UUID versionId;
  private final UUID documentId;
  private final int versionNumber;
  private final String storageKey;
  private final long fileSizeBytes;
  private final String checksumSha256;
  private final String changeSummary;
  private final UUID uploadedByUserId;
  private final Instant timestamp;
}
