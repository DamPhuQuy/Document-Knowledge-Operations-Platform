package com.platform.app.document.application.dto;

import com.platform.app.document.domain.model.AccessLevel;
import java.io.InputStream;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UploadDocumentCommand {

    private final InputStream inputStream;
    private final String originalFileName;
    private final String contentType;
    private final long fileSize;
    private final String title;
    private final String description;
    private final AccessLevel accessLevel;
    private final UUID departmentId;
    private final UUID userId;
}
