package com.platform.app.document.application.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SoftDeleteDocumentCommand {

  private final UUID documentId;
  private final UUID currentUserId;
  private final boolean isAdmin;
  private final boolean hasDeletePermission;
}
