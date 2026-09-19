package com.platform.app.document.application.ports.outbound;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.platform.app.document.domain.model.Document;

public interface DocumentRepositoryPort {

  Document save(Document document);

  Optional<Document> findById(UUID id);

  boolean softDelete(UUID id, Instant deletedAt);
}
