package com.platform.app.document.application.event;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocumentSoftDeletedEvent {

  private final UUID documentId;
  private final UUID deletedByUserId;
  private final Instant timestamp;
}
