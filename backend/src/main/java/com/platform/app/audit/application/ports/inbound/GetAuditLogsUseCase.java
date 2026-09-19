package com.platform.app.audit.application.ports.inbound;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.platform.app.audit.application.dto.AuditLogQueryFilter;
import com.platform.app.audit.application.dto.AuditLogResponseDto;

public interface GetAuditLogsUseCase {

  Page<AuditLogResponseDto> getAuditLogs(AuditLogQueryFilter filter, Pageable pageable);
}
