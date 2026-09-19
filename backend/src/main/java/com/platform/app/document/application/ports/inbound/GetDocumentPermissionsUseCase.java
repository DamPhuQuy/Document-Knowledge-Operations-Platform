package com.platform.app.document.application.ports.inbound;

import com.platform.app.document.application.dto.DocumentPermissionsResponseDto;
import java.util.UUID;

public interface GetDocumentPermissionsUseCase {
    DocumentPermissionsResponseDto getPermissions(
        UUID documentId,
        UUID currentUserId,
        boolean isAdmin,
        boolean hasManagePermissions
    );
}
