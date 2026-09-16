package com.platform.app.document.application.ports.inbound;

import com.platform.app.document.domain.model.Document;
import com.platform.app.document.domain.model.DocumentVersion;

public interface StoreVersionMetadataUseCase {

  DocumentVersion persistVersionMetadata(
      Document document,
      DocumentVersion version,
      int nextVersion,
      String storageKey,
      String checksumSha256,
      long fileSize,
      String contentType,
      String originalFileName);
}
