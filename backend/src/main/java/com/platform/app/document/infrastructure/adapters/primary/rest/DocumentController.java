package com.platform.app.document.infrastructure.adapters.primary.rest;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.platform.app.document.application.dto.ConfigureDocumentAclCommand;
import com.platform.app.document.application.dto.DocumentPermissionsResponseDto;
import com.platform.app.document.application.dto.DocumentResponseDto;
import com.platform.app.document.application.dto.DocumentVersionResponseDto;
import com.platform.app.document.application.dto.SoftDeleteDocumentCommand;
import com.platform.app.document.application.dto.UpdateDocumentPermissionsRequest;
import com.platform.app.document.application.dto.UploadDocumentCommand;
import com.platform.app.document.application.dto.UploadDocumentVersionCommand;
import com.platform.app.document.application.ports.inbound.ConfigureDocumentAclUseCase;
import com.platform.app.document.application.ports.inbound.GetDocumentPermissionsUseCase;
import com.platform.app.document.application.ports.inbound.SoftDeleteDocumentUseCase;
import com.platform.app.document.application.ports.inbound.UploadDocumentUseCase;
import com.platform.app.document.application.ports.inbound.UploadDocumentVersionUseCase;
import com.platform.app.document.domain.exception.DocumentValidationException;
import com.platform.app.document.domain.model.AccessLevel;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
  private final ConfigureDocumentAclUseCase configureDocumentAclUseCase;
  private final GetDocumentPermissionsUseCase getDocumentPermissionsUseCase;
  private final SoftDeleteDocumentUseCase softDeleteDocumentUseCase;

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

  @PutMapping(value = "/{id}/permissions", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Configure document access control matrix (UC-DOC-03)")
  public ResponseEntity<DocumentPermissionsResponseDto> updatePermissions(
      @PathVariable("id") UUID id,
      @Valid @RequestBody UpdateDocumentPermissionsRequest request,
      Authentication authentication) {

    UUID userId = extractUserId(authentication);
    if (userId == null) {
      log.warn("Unauthenticated attempt to configure permissions for document {}", id);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    boolean hasManagePermissions = authentication != null && authentication.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equalsIgnoreCase("manage:permissions") ||
                       a.getAuthority().equalsIgnoreCase("MANAGE:PERMISSIONS"));

    log.info("REST PUT /api/v1/documents/{}/permissions received: accessLevel={}, user={}, isAdmin={}, hasManage={}",
        id, request.getAccessLevel(), userId, isAdmin, hasManagePermissions);

    ConfigureDocumentAclCommand command = ConfigureDocumentAclCommand.builder()
        .documentId(id)
        .currentUserId(userId)
        .isAdmin(isAdmin)
        .hasManagePermissions(hasManagePermissions)
        .accessLevel(request.getAccessLevel())
        .userGrants(request.getUserGrants())
        .departmentGrants(request.getDepartmentGrants())
        .roleGrants(request.getRoleGrants())
        .build();

    DocumentPermissionsResponseDto response = configureDocumentAclUseCase.configureAcl(command);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}/permissions")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get document access control matrix (UC-DOC-03)")
  public ResponseEntity<DocumentPermissionsResponseDto> getPermissions(
      @PathVariable("id") UUID id,
      Authentication authentication) {

    UUID userId = extractUserId(authentication);
    if (userId == null) {
      log.warn("Unauthenticated attempt to get permissions for document {}", id);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    boolean hasManagePermissions = authentication != null && authentication.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equalsIgnoreCase("manage:permissions") ||
                       a.getAuthority().equalsIgnoreCase("MANAGE:PERMISSIONS"));

    log.info("REST GET /api/v1/documents/{}/permissions received: user={}, isAdmin={}, hasManage={}",
        id, userId, isAdmin, hasManagePermissions);

    DocumentPermissionsResponseDto response = getDocumentPermissionsUseCase.getPermissions(id, userId, isAdmin, hasManagePermissions);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Soft-delete document (UC-DOC-04)")
  public ResponseEntity<Void> deleteDocument(
      @PathVariable("id") UUID id,
      Authentication authentication) {

    UUID userId = extractUserId(authentication);
    if (userId == null) {
      log.warn("Unauthenticated attempt to delete document {}", id);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    boolean hasDeletePermission = authentication != null && authentication.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equalsIgnoreCase("delete:documents") ||
                       a.getAuthority().equalsIgnoreCase("DELETE:DOCUMENTS"));

    log.info("REST DELETE /api/v1/documents/{} received: user={}, isAdmin={}, hasDeletePermission={}",
        id, userId, isAdmin, hasDeletePermission);

    SoftDeleteDocumentCommand command = SoftDeleteDocumentCommand.builder()
        .documentId(id)
        .currentUserId(userId)
        .isAdmin(isAdmin)
        .hasDeletePermission(hasDeletePermission)
        .build();

    softDeleteDocumentUseCase.softDeleteDocument(command);
    return ResponseEntity.noContent().build();
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
