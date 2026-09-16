package com.platform.app.document.application.ports.inbound;

import com.platform.app.document.application.dto.DocumentVersionResponseDto;
import com.platform.app.document.application.dto.UploadDocumentVersionCommand;

public interface UploadDocumentVersionUseCase {

  DocumentVersionResponseDto uploadVersion(UploadDocumentVersionCommand command);
}
