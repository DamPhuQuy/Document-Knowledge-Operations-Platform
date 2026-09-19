package com.platform.app.document.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.platform.app.document.application.dto.UploadDocumentCommand;
import com.platform.app.document.application.ports.outbound.DocumentRepositoryPort;
import com.platform.app.document.application.ports.outbound.DocumentVersionRepositoryPort;
import com.platform.app.document.domain.model.AccessLevel;
import com.platform.app.document.domain.model.Document;
import com.platform.app.document.domain.model.DocumentStatus;
import com.platform.app.document.domain.model.DocumentVersion;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentMetadataServiceTest {

    @Mock
    private DocumentRepositoryPort documentRepositoryPort;

    @Mock
    private DocumentVersionRepositoryPort documentVersionRepositoryPort;

    @Captor
    private ArgumentCaptor<Document> documentCaptor;

    private DocumentMetadataService documentMetadataService;

    private static final String SHA256_1 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String SHA256_2 = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    @BeforeEach
    void setUp() {
        documentMetadataService = new DocumentMetadataService(
            documentRepositoryPort,
            documentVersionRepositoryPort
        );
    }

    @Test
    @DisplayName("Should persist initial document metadata with provided title and access level")
    void shouldPersistMetadataWithProvidedTitleAndAccessLevel() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        UploadDocumentCommand command = UploadDocumentCommand.builder()
            .userId(userId)
            .departmentId(deptId)
            .title("Annual Report 2026")
            .originalFileName("report.pdf")
            .contentType("application/pdf")
            .fileSize(1024L)
            .accessLevel(AccessLevel.RESTRICTED)
            .inputStream(new ByteArrayInputStream(new byte[0]))
            .build();

        when(documentRepositoryPort.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document savedDoc = documentMetadataService.persistMetadata(
            docId,
            command,
            "report.pdf",
            "storage/key/report.pdf",
            SHA256_1,
            "application/pdf"
        );

        assertThat(savedDoc).isNotNull();
        assertThat(savedDoc.getId()).isEqualTo(docId);
        assertThat(savedDoc.getTitle()).isEqualTo("Annual Report 2026");
        assertThat(savedDoc.getAccessLevel()).isEqualTo(AccessLevel.RESTRICTED);
        assertThat(savedDoc.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(savedDoc.getCurrentVersion()).isEqualTo(1);
        assertThat(savedDoc.getStorageKey()).isEqualTo("storage/key/report.pdf");
        assertThat(savedDoc.getChecksumSha256()).isEqualTo(SHA256_1);

        verify(documentRepositoryPort).save(documentCaptor.capture());
        Document captured = documentCaptor.getValue();
        assertThat(captured.getId()).isEqualTo(docId);
    }

    @Test
    @DisplayName("Should fallback to sanitizedFileName when title is empty and default to INTERNAL access level")
    void shouldFallbackToSanitizedFileNameWhenTitleIsEmpty() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UploadDocumentCommand command = UploadDocumentCommand.builder()
            .userId(userId)
            .departmentId(null)
            .title("   ")
            .originalFileName("fallback.docx")
            .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            .fileSize(2048L)
            .accessLevel(null)
            .inputStream(new ByteArrayInputStream(new byte[0]))
            .build();

        when(documentRepositoryPort.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document savedDoc = documentMetadataService.persistMetadata(
            docId,
            command,
            "fallback.docx",
            "storage/key/fallback.docx",
            SHA256_1,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );

        assertThat(savedDoc.getTitle()).isEqualTo("fallback.docx");
        assertThat(savedDoc.getAccessLevel()).isEqualTo(AccessLevel.INTERNAL);
    }

    @Test
    @DisplayName("Should persist new version metadata and update current document version")
    void shouldPersistVersionMetadataAndUpdateDocument() {
        UUID docId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Document document = Document.builder()
            .id(docId)
            .title("Doc Title")
            .originalFileName("v1.pdf")
            .contentType("application/pdf")
            .fileSizeBytes(100L)
            .checksumSha256(SHA256_1)
            .storageKey("old-key")
            .currentVersion(1)
            .status(DocumentStatus.UPLOADED)
            .uploadedByUserId(userId)
            .createdAt(Instant.now().minusSeconds(3600))
            .updatedAt(Instant.now().minusSeconds(3600))
            .build();

        DocumentVersion version = DocumentVersion.builder()
            .id(versionId)
            .documentId(docId)
            .versionNumber(2)
            .storageKey("new-storage-key")
            .fileSizeBytes(200L)
            .checksumSha256(SHA256_2)
            .uploadedByUserId(userId)
            .createdAt(Instant.now())
            .build();

        when(documentVersionRepositoryPort.save(version)).thenReturn(version);
        when(documentRepositoryPort.save(document)).thenReturn(document);

        DocumentVersion resultVersion = documentMetadataService.persistVersionMetadata(
            document,
            version,
            2,
            "new-storage-key",
            SHA256_2,
            200L,
            "application/pdf",
            "v2.pdf"
        );

        assertThat(resultVersion).isEqualTo(version);
        assertThat(document.getCurrentVersion()).isEqualTo(2);
        assertThat(document.getStorageKey()).isEqualTo("new-storage-key");
        assertThat(document.getChecksumSha256()).isEqualTo(SHA256_2);
        assertThat(document.getFileSizeBytes()).isEqualTo(200L);
        assertThat(document.getOriginalFileName()).isEqualTo("v2.pdf");

        verify(documentVersionRepositoryPort).save(version);
        verify(documentRepositoryPort).save(document);
    }
}
