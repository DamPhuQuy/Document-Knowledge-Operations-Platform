package com.platform.app.document.application.ports.inbound;

import com.platform.app.document.application.dto.UploadDocumentCommand;
import com.platform.app.document.domain.model.Document;
import java.util.UUID;

public interface StoreMetadataUseCase {
    Document persistMetadata(
        UUID docId,
        UploadDocumentCommand command,
        String sanitizedFileName,
        String storageKey,
        String checksumSha256,
        String contentType
    );
}
