package com.platform.app.document.domain.model;

import com.platform.app.document.domain.exception.DocumentValidationException;
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
public class DocumentVersion {

    @ToString.Include
    @EqualsAndHashCode.Include
    private final UUID id;

    @ToString.Include
    private final UUID documentId;

    @ToString.Include
    private final int versionNumber;

    private final String storageKey;

    private final long fileSizeBytes;

    private final String checksumSha256;

    private final String changeSummary;

    private final UUID uploadedByUserId;

    private final Instant createdAt;

    public DocumentVersion(
        UUID id,
        UUID documentId,
        int versionNumber,
        String storageKey,
        long fileSizeBytes,
        String checksumSha256,
        String changeSummary,
        UUID uploadedByUserId,
        Instant createdAt
    ) {
        this.id = id != null ? id : IdGenerator.nextId();
        this.documentId = Objects.requireNonNull(
            documentId,
            "Document ID must not be null"
        );

        if (versionNumber < 1) {
            throw new DocumentValidationException(
                "Version number must be at least 1"
            );
        }
        this.versionNumber = versionNumber;

        if (storageKey == null || storageKey.trim().isEmpty()) {
            throw new DocumentValidationException(
                "Storage key must not be blank"
            );
        }
        this.storageKey = storageKey.trim();

        if (fileSizeBytes <= 0) {
            throw new DocumentValidationException(
                "File size must be strictly positive"
            );
        }
        this.fileSizeBytes = fileSizeBytes;

        if (checksumSha256 == null || checksumSha256.trim().length() != 64) {
            throw new DocumentValidationException(
                "Checksum SHA-256 must be a 64-character hex string"
            );
        }
        this.checksumSha256 = checksumSha256.trim().toLowerCase();

        this.uploadedByUserId = Objects.requireNonNull(
            uploadedByUserId,
            "Uploaded by user ID must not be null"
        );
        this.changeSummary = changeSummary;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }
}
