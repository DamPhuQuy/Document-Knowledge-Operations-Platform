package com.platform.app.audit.infrastructure.adapters.primary.rest;

import com.platform.app.audit.application.dto.AuditLogQueryFilter;
import com.platform.app.audit.application.dto.AuditLogResponseDto;
import com.platform.app.audit.application.ports.inbound.GetAuditLogsUseCase;
import com.platform.app.audit.domain.model.AuditStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Audit Logging",
    description = "Endpoints for inspecting system-wide immutable audit trail logs (UC-AUDIT-01)"
)
public class AuditLogController {

    private final GetAuditLogsUseCase getAuditLogsUseCase;

    @GetMapping
    @PreAuthorize(
        "hasAuthority('read:audit_logs') or hasAuthority('READ:AUDIT_LOGS') or hasRole('ADMIN') or hasRole('LEGAL_AUDITOR')"
    )
    @Operation(
        summary = "Query immutable audit logs with filters and pagination (UC-AUDIT-01)"
    )
    public ResponseEntity<Page<AuditLogResponseDto>> getAuditLogs(
        @RequestParam(value = "userId", required = false) UUID userId,
        @RequestParam(value = "action", required = false) String action,
        @RequestParam(
            value = "resourceType",
            required = false
        ) String resourceType,
        @RequestParam(value = "resourceId", required = false) String resourceId,
        @RequestParam(value = "status", required = false) AuditStatus status,
        @RequestParam(value = "startDate", required = false) Instant startDate,
        @RequestParam(value = "endDate", required = false) Instant endDate,
        @PageableDefault(
            sort = "createdAt",
            direction = Sort.Direction.DESC
        ) Pageable pageable
    ) {
        log.info(
            "REST GET /api/v1/audit-logs query: userId={}, action={}, resourceType={}, status={}, pageable={}",
            userId,
            action,
            resourceType,
            status,
            pageable
        );

        AuditLogQueryFilter filter = AuditLogQueryFilter.builder()
            .userId(userId)
            .action(action)
            .resourceType(resourceType)
            .resourceId(resourceId)
            .status(status)
            .startDate(startDate)
            .endDate(endDate)
            .build();

        Page<AuditLogResponseDto> response = getAuditLogsUseCase.getAuditLogs(
            filter,
            pageable
        );
        return ResponseEntity.ok(response);
    }
}
