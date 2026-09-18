package com.platform.app.document.application.ports.inbound;

import java.util.UUID;

import com.platform.app.document.application.dto.DocumentPermissionsResponseDto;

public interface GetDocumentPermissionsUseCase {

  DocumentPermissionsResponseDto getPermissions(
      UUID documentId,
      UUID currentUserId,
      boolean isAdmin,
      boolean hasManagePermissions);
}
