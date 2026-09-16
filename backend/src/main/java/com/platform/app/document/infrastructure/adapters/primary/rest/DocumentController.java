package com.platform.app.document.infrastructure.adapters.primary.rest;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.platform.app.document.application.dto.DocumentResponseDto;
import com.platform.app.document.application.dto.DocumentVersionResponseDto;
import com.platform.app.document.application.dto.UploadDocumentCommand;
import com.platform.app.document.application.dto.UploadDocumentVersionCommand;
import com.platform.app.document.application.ports.inbound.UploadDocumentUseCase;
import com.platform.app.document.application.ports.inbound.UploadDocumentVersionUseCase;
import com.platform.app.document.domain.exception.DocumentValidationException;
import com.platform.app.document.domain.model.AccessLevel;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Document Management", description = "Endpoints for uploading and managing documents")
public class DocumentController {

  private final UploadDocumentUseCase uploadDocumentUseCase;
  private final UploadDocumentVersionUseCase uploadDocumentVersionUseCase;

  @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('write:documents') or hasAuthority('WRITE:DOCUMENTS') or hasRole('ADMIN')")
  @Operation(summary = "Upload replacement revision for an existing document (UC-DOC-02)")
  public ResponseEntity<DocumentVersionResponseDto> uploadVersion(
      @PathVariable("id") UUID id,
      @RequestPart("file") MultipartFile file,
      @RequestParam(value = "changeSummary", required = false) String changeSummary,
      Authentication authentication) throws IOException {

    if (file == null || file.isEmpty()) {
      throw new DocumentValidationException("Uploaded file must not be empty");
    }

    UUID userId = extractUserId(authentication);
    if (userId == null) {
      log.warn("Unauthenticated attempt to upload document version for id={}", id);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

    String originalFilename = file.getOriginalFilename();
    log.info("REST POST /api/v1/documents/{}/versions received: fileName={}, size={}, user={}, isAdmin={}",
        id, originalFilename, file.getSize(), userId, isAdmin);

    UploadDocumentVersionCommand command = UploadDocumentVersionCommand.builder()
        .documentId(id)
        .userId(userId)
        .inputStream(file.getInputStream())
        .originalFileName(originalFilename)
        .contentType(file.getContentType())
        .fileSize(file.getSize())
        .changeSummary(changeSummary)
        .isAdmin(isAdmin)
        .build();

    DocumentVersionResponseDto response = uploadDocumentVersionUseCase.uploadVersion(command);
    return ResponseEntity.ok(response);
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('write:documents') or hasAuthority('WRITE:DOCUMENTS') or hasRole('ADMIN')")
  @Operation(summary = "Upload document and store on S3 object storage (UC-DOC-01)")
  public ResponseEntity<DocumentResponseDto> uploadDocument(
      @RequestPart("file") MultipartFile file,
      @RequestParam(value = "title", required = false) String title,
      @RequestParam(value = "description", required = false) String description,
      @RequestParam(value = "accessLevel", required = false) AccessLevel accessLevel,
      @RequestParam(value = "departmentId", required = false) UUID departmentId,
      Authentication authentication) throws IOException {

    if (file == null || file.isEmpty()) {
      throw new DocumentValidationException("Uploaded file must not be empty");
    }

    UUID userId = extractUserId(authentication);
    if (userId == null) {
      log.warn("Unauthenticated attempt to upload document");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String originalFilename = file.getOriginalFilename();
    log.info("REST POST /api/v1/documents received: fileName={}, size={}, user={}",
        originalFilename, file.getSize(), userId);

    UploadDocumentCommand command = UploadDocumentCommand.builder()
        .inputStream(file.getInputStream())
        .originalFileName(originalFilename)
        .contentType(file.getContentType())
        .fileSize(file.getSize())
        .title(title)
        .description(description)
        .accessLevel(accessLevel != null ? accessLevel : AccessLevel.INTERNAL)
        .departmentId(departmentId)
        .userId(userId)
        .build();

    DocumentResponseDto response = uploadDocumentUseCase.uploadDocument(command);

    URI location = URI.create("/api/v1/documents/" + response.getId());
    return ResponseEntity.created(location).body(response);
  }

  private UUID extractUserId(Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      return null;
    }
    try {
      return UUID.fromString(authentication.getName());
    } catch (IllegalArgumentException e) {
      log.warn("Authentication name is not a valid UUID: {}", authentication.getName());
      return null;
    }
  }
}
