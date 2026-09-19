package com.platform.app.document.application.ports.inbound;

import com.platform.app.document.application.dto.SoftDeleteDocumentCommand;

public interface SoftDeleteDocumentUseCase {

  void softDeleteDocument(SoftDeleteDocumentCommand command);
}
