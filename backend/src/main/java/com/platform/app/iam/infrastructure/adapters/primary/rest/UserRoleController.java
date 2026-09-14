package com.platform.app.iam.infrastructure.adapters.primary.rest;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.platform.app.iam.application.dto.UserRolesResponseDto;
import com.platform.app.iam.application.ports.inbound.AssignRolesCommand;
import com.platform.app.iam.application.ports.inbound.AssignRolesUseCase;
import com.platform.app.iam.application.ports.inbound.GetUserRolesUseCase;
import com.platform.app.iam.infrastructure.adapters.primary.rest.dto.request.AssignRolesRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserRoleController {

  private final GetUserRolesUseCase getUserRolesUseCase;
  private final AssignRolesUseCase assignRolesUseCase;

  @GetMapping("/{userId}/roles")
  @PreAuthorize("hasAuthority('manage:users') or hasAuthority('MANAGE:USERS') or hasRole('ADMIN')")
  public ResponseEntity<UserRolesResponseDto> getUserRoles(@PathVariable("userId") UUID userId) {
    log.debug("REST GET /api/v1/users/{}/roles requested", userId);
    UserRolesResponseDto response = getUserRolesUseCase.getUserRoles(userId);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{userId}/roles")
  @PreAuthorize("hasAuthority('manage:users') or hasAuthority('MANAGE:USERS') or hasRole('ADMIN')")
  public ResponseEntity<UserRolesResponseDto> assignRoles(
      @PathVariable("userId") UUID userId,
      @Valid @RequestBody AssignRolesRequest request,
      Authentication authentication) {

    UUID operatorUserId = null;
    if (authentication != null && authentication.getName() != null) {
      try {
        operatorUserId = UUID.fromString(authentication.getName());
      } catch (IllegalArgumentException _) {
        // Operator principal is not a UUID (e.g. mock user in test)
      }
    }

    log.info(
        "REST PUT /api/v1/users/{}/roles requested with {} roles by operator [{}]",
        userId,
        request.roleIds() != null ? request.roleIds().size() : 0,
        operatorUserId);

    AssignRolesCommand command =
        new AssignRolesCommand(userId, request.roleIds(), operatorUserId);

    UserRolesResponseDto response = assignRolesUseCase.assignRoles(command);
    log.debug("REST PUT /api/v1/users/{}/roles completed successfully for user [{}]", userId, response.userId());
    return ResponseEntity.ok(response);
  }
}
