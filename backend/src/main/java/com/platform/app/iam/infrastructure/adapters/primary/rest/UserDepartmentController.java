package com.platform.app.iam.infrastructure.adapters.primary.rest;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.platform.app.iam.application.dto.UserDepartmentResponseDto;
import com.platform.app.iam.application.ports.inbound.AssignUserDepartmentCommand;
import com.platform.app.iam.application.ports.inbound.AssignUserDepartmentUseCase;
import com.platform.app.iam.infrastructure.adapters.primary.rest.dto.request.AssignUserDepartmentRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserDepartmentController {

  private final AssignUserDepartmentUseCase assignUserDepartmentUseCase;

  @PutMapping("/{userId}/department")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserDepartmentResponseDto> assignDepartment(
      @PathVariable("userId") UUID userId,
      @Valid @RequestBody AssignUserDepartmentRequest request,
      Authentication authentication) {

    UUID operatorUserId = extractOperatorId(authentication);
    log.info(
        "REST PUT /api/v1/users/{}/department requested with dept [{}] isInternal [{}] by operator [{}]",
        userId,
        request.departmentId(),
        request.isInternal(),
        operatorUserId);

    AssignUserDepartmentCommand command =
        new AssignUserDepartmentCommand(
            userId, request.departmentId(), request.isInternal(), operatorUserId);

    UserDepartmentResponseDto response = assignUserDepartmentUseCase.assignUserDepartment(command);
    log.debug("REST PUT /api/v1/users/{}/department completed for user [{}]", userId, response.userId());
    return ResponseEntity.ok(response);
  }

  private UUID extractOperatorId(Authentication authentication) {
    if (authentication != null && authentication.getName() != null) {
      try {
        return UUID.fromString(authentication.getName());
      } catch (IllegalArgumentException _) {
        // Principal is not a UUID
      }
    }
    return null;
  }
}
