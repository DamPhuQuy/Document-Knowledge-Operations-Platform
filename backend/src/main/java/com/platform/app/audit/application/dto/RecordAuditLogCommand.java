package com.platform.app.audit.application.dto;

import com.platform.app.audit.domain.model.AuditStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecordAuditLogCommand {

    private final UUID userId;
    private final String action;
    private final String resourceType;
    private final String resourceId;
    private final String ipAddress;
    private final String userAgent;
    private final AuditStatus status;
    private final Map<String, Object> details;
    private final Instant timestamp;
}
