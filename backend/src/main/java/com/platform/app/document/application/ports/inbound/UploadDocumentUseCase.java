package com.platform.app.document.application.ports.inbound;

import com.platform.app.document.application.dto.DocumentResponseDto;
import com.platform.app.document.application.dto.UploadDocumentCommand;

public interface UploadDocumentUseCase {
    DocumentResponseDto uploadDocument(UploadDocumentCommand command);
}
