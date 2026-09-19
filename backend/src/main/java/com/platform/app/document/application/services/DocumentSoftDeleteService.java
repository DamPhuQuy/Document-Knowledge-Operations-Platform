package com.platform.app.document.application.services;

import com.platform.app.document.application.dto.SoftDeleteDocumentCommand;
import com.platform.app.document.application.event.DocumentSoftDeletedEvent;
import com.platform.app.document.application.ports.inbound.SoftDeleteDocumentUseCase;
import com.platform.app.document.application.ports.outbound.DocumentRepositoryPort;
import com.platform.app.document.domain.exception.DocumentAccessDeniedException;
import com.platform.app.document.domain.exception.DocumentNotFoundException;
import com.platform.app.document.domain.model.Document;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentSoftDeleteService implements SoftDeleteDocumentUseCase {

    private final DocumentRepositoryPort documentRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void softDeleteDocument(SoftDeleteDocumentCommand command) {
        Objects.requireNonNull(command, "Soft delete command must not be null");
        Objects.requireNonNull(
            command.getDocumentId(),
            "Document ID must not be null"
        );
        Objects.requireNonNull(
            command.getCurrentUserId(),
            "Current user ID must not be null"
        );

        Document document = documentRepositoryPort
            .findById(command.getDocumentId())
            .orElseThrow(() ->
                new DocumentNotFoundException(
                    "Document not found with ID: " + command.getDocumentId()
                )
            );

        boolean isOwner = document
            .getUploadedByUserId()
            .equals(command.getCurrentUserId());
        boolean isAuthorized =
            isOwner || command.isAdmin() || command.isHasDeletePermission();

        if (!isAuthorized) {
            log.warn(
                "Access denied for user {} attempting to soft-delete document {} owned by {}",
                command.getCurrentUserId(),
                document.getId(),
                document.getUploadedByUserId()
            );
            throw new DocumentAccessDeniedException(
                "User does not have permission to delete document " +
                    command.getDocumentId()
            );
        }

        Instant deletedAt = Instant.now();
        boolean updated = documentRepositoryPort.softDelete(
            document.getId(),
            deletedAt
        );
        if (!updated) {
            log.warn(
                "Document {} was concurrently modified or already deleted",
                document.getId()
            );
            throw new DocumentNotFoundException(
                "Document not found or already deleted: " +
                    command.getDocumentId()
            );
        }

        DocumentSoftDeletedEvent event = DocumentSoftDeletedEvent.builder()
            .documentId(document.getId())
            .deletedByUserId(command.getCurrentUserId())
            .timestamp(deletedAt)
            .build();
        eventPublisher.publishEvent(event);

        log.info(
            "Successfully soft-deleted document {} by user {}",
            document.getId(),
            command.getCurrentUserId()
        );
    }
}
