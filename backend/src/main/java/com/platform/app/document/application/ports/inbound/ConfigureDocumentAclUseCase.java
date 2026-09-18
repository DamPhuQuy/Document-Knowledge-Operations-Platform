package com.platform.app.document.application.ports.inbound;

import com.platform.app.document.application.dto.ConfigureDocumentAclCommand;
import com.platform.app.document.application.dto.DocumentPermissionsResponseDto;

public interface ConfigureDocumentAclUseCase {

  DocumentPermissionsResponseDto configureAcl(ConfigureDocumentAclCommand command);
}
