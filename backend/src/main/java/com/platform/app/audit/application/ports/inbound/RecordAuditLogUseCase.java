package com.platform.app.audit.application.ports.inbound;

import com.platform.app.audit.application.dto.RecordAuditLogCommand;
import com.platform.app.audit.domain.model.AuditLog;

public interface RecordAuditLogUseCase {

  AuditLog recordAuditLog(RecordAuditLogCommand command);
}
