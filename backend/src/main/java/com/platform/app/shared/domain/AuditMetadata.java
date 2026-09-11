package com.platform.app.shared.domain;

import java.time.Instant;

import lombok.Builder;

@Builder
public record AuditMetadata(
    Instant createdAt,
    Instant updatedAt
) {
    public static AuditMetadata of(Instant createdAt, Instant updatedAt) {
        return new AuditMetadata(createdAt, updatedAt);
    }

    public static AuditMetadata now() {
        Instant now = Instant.now();
        return new AuditMetadata(now, now);
    }

    public AuditMetadata withUpdatedAt(Instant updatedAt) {
        return new AuditMetadata(this.createdAt, updatedAt);
    }
}
