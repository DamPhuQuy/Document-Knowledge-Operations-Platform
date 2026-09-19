package com.platform.app.document.application.dto;

import java.io.InputStream;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UploadDocumentVersionCommand {

    private final UUID documentId;
    private final UUID userId;
    private final InputStream inputStream;
    private final String originalFileName;
    private final String contentType;
    private final long fileSize;
    private final String changeSummary;
    private final boolean isAdmin;
}
