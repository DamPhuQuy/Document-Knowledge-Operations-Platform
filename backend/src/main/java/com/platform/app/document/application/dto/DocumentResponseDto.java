package com.platform.app.document.application.dto;

import com.platform.app.document.domain.model.AccessLevel;
import com.platform.app.document.domain.model.Document;
import com.platform.app.document.domain.model.DocumentStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocumentResponseDto {

    private final UUID id;
    private final String title;
    private final String originalFileName;
    private final String contentType;
    private final long fileSizeBytes;
    private final String checksumSha256;
    private final String storageKey;
    private final DocumentStatus status;
    private final AccessLevel accessLevel;
    private final UUID departmentId;
    private final UUID uploadedByUserId;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static DocumentResponseDto fromDomain(Document doc) {
        if (doc == null) {
            return null;
        }
        return DocumentResponseDto.builder()
            .id(doc.getId())
            .title(doc.getTitle())
            .originalFileName(doc.getOriginalFileName())
            .contentType(doc.getContentType())
            .fileSizeBytes(doc.getFileSizeBytes())
            .checksumSha256(doc.getChecksumSha256())
            .storageKey(doc.getStorageKey())
            .status(doc.getStatus())
            .accessLevel(doc.getAccessLevel())
            .departmentId(doc.getDepartmentId())
            .uploadedByUserId(doc.getUploadedByUserId())
            .createdAt(doc.getCreatedAt())
            .updatedAt(doc.getUpdatedAt())
            .build();
    }
}
