package com.platform.app.document.application.event;

import java.time.Instant;
import java.util.UUID;

import com.platform.app.document.domain.model.AccessLevel;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocumentAclUpdatedEvent {

  private final UUID documentId;
  private final UUID updatedByUserId;
  private final AccessLevel accessLevel;
  private final int userGrantsCount;
  private final int departmentGrantsCount;
  private final int roleGrantsCount;
  private final Instant timestamp;
}
