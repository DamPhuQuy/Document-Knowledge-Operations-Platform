package com.platform.app.audit.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
public class AuditLog {

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

    @Builder
    public AuditLog(
        UUID id,
        UUID userId,
        String action,
        String resourceType,
        String resourceId,
        String ipAddress,
        String userAgent,
        AuditStatus status,
        Map<String, Object> details,
        Instant createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.action = Objects.requireNonNull(
            action,
            "Audit action must not be null"
        );
        this.resourceType = Objects.requireNonNull(
            resourceType,
            "Resource type must not be null"
        );
        this.resourceId = resourceId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.status = status != null ? status : AuditStatus.SUCCESS;
        this.details =
            details != null
                ? Collections.unmodifiableMap(details)
                : Collections.emptyMap();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }
}
