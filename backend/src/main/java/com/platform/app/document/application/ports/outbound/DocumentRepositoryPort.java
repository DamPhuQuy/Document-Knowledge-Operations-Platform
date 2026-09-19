package com.platform.app.document.application.ports.outbound;

import com.platform.app.document.domain.model.Document;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepositoryPort {
    Document save(Document document);

    Optional<Document> findById(UUID id);

    boolean softDelete(UUID id, Instant deletedAt);
}
