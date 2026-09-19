package com.platform.app.document.application.ports.outbound;

import com.platform.app.document.domain.model.DocumentVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentVersionRepositoryPort {
    DocumentVersion save(DocumentVersion version);

    List<DocumentVersion> findByDocumentId(UUID documentId);

    Optional<DocumentVersion> findByDocumentIdAndVersionNumber(
        UUID documentId,
        int versionNumber
    );
}
