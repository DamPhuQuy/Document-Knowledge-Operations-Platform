package com.platform.app.audit.application.dto;

import com.platform.app.audit.domain.model.AuditLog;
import com.platform.app.audit.domain.model.AuditStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditLogResponseDto {

    private final UUID id;
    private final UUID userId;
    private final String action;
    private final String resourceType;
    private final String resourceId;
    private final String ipAddress;
    private final String userAgent;
    private final AuditStatus status;
    private final Map<String, Object> details;
    private final Instant createdAt;

    public static AuditLogResponseDto fromDomain(AuditLog log) {
        if (log == null) {
            return null;
        }
        return AuditLogResponseDto.builder()
            .id(log.getId())
            .userId(log.getUserId())
            .action(log.getAction())
            .resourceType(log.getResourceType())
            .resourceId(log.getResourceId())
            .ipAddress(log.getIpAddress())
            .userAgent(log.getUserAgent())
            .status(log.getStatus())
            .details(log.getDetails())
            .createdAt(log.getCreatedAt())
            .build();
    }
}
