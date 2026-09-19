package com.platform.app.audit.application.services;

import com.platform.app.audit.application.dto.AuditLogQueryFilter;
import com.platform.app.audit.application.dto.AuditLogResponseDto;
import com.platform.app.audit.application.dto.RecordAuditLogCommand;
import com.platform.app.audit.application.ports.inbound.GetAuditLogsUseCase;
import com.platform.app.audit.application.ports.inbound.RecordAuditLogUseCase;
import com.platform.app.audit.application.ports.outbound.AuditLogRepositoryPort;
import com.platform.app.audit.domain.model.AuditLog;
import com.platform.app.audit.domain.model.AuditStatus;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLoggingService
    implements RecordAuditLogUseCase, GetAuditLogsUseCase
{

    private final AuditLogRepositoryPort auditLogRepositoryPort;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog recordAuditLog(RecordAuditLogCommand command) {
        Objects.requireNonNull(
            command,
            "RecordAuditLogCommand must not be null"
        );

        AuditLog auditLog = AuditLog.builder()
            .userId(command.getUserId())
            .action(command.getAction())
            .resourceType(command.getResourceType())
            .resourceId(command.getResourceId())
            .ipAddress(command.getIpAddress())
            .userAgent(command.getUserAgent())
            .status(
                command.getStatus() != null
                    ? command.getStatus()
                    : AuditStatus.SUCCESS
            )
            .details(command.getDetails())
            .createdAt(command.getTimestamp())
            .build();

        AuditLog saved = auditLogRepositoryPort.save(auditLog);
        log.debug(
            "Recorded audit log id={}, action={}, resource={}:{}, user={}",
            saved.getId(),
            saved.getAction(),
            saved.getResourceType(),
            saved.getResourceId(),
            saved.getUserId()
        );
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> getAuditLogs(
        AuditLogQueryFilter filter,
        Pageable pageable
    ) {
        return auditLogRepositoryPort
            .findAll(filter, pageable)
            .map(AuditLogResponseDto::fromDomain);
    }
}
