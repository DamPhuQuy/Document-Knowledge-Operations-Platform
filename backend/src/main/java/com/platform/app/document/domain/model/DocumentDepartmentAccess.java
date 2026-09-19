package com.platform.app.document.domain.model;

import com.platform.app.shared.util.IdGenerator;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
public class DocumentDepartmentAccess {

    @ToString.Include
    @EqualsAndHashCode.Include
    private final UUID id;

    @ToString.Include
    private final UUID documentId;

    @ToString.Include
    private final UUID departmentId;

    @ToString.Include
    private final PermissionLevel permissionLevel;

    private final Instant createdAt;

    public DocumentDepartmentAccess(
        UUID id,
        UUID documentId,
        UUID departmentId,
        PermissionLevel permissionLevel,
        Instant createdAt
    ) {
        this.id = id != null ? id : IdGenerator.nextId();
        this.documentId = Objects.requireNonNull(
            documentId,
            "Document ID must not be null"
        );
        this.departmentId = Objects.requireNonNull(
            departmentId,
            "Department ID must not be null"
        );
        this.permissionLevel = Objects.requireNonNull(
            permissionLevel,
            "Permission level must not be null"
        );
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }
}
