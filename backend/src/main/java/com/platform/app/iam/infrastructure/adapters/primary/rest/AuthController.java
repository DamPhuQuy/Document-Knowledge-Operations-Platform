package com.platform.app.iam.infrastructure.adapters.primary.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.platform.app.iam.application.dto.AuthTokensDto;
import com.platform.app.iam.application.ports.inbound.LoginCommand;
import com.platform.app.iam.application.ports.inbound.LoginUseCase;
import com.platform.app.iam.infrastructure.adapters.primary.rest.dto.request.LoginRequest;
import com.platform.app.iam.infrastructure.adapters.primary.rest.dto.response.AuthResponse;
import com.platform.app.iam.infrastructure.adapters.primary.rest.dto.response.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Endpoints for user authentication and session management")
public class AuthController {

  private final LoginUseCase loginUseCase;

  @PostMapping("/login")
  @Operation(
      summary = "User Login",
      description =
          "Authenticates user credentials and issues a JWT Access Token and long-lived Refresh Token")
  @ApiResponse(
      responseCode = "200",
      description = "Authentication successful",
      content = @Content(schema = @Schema(implementation = AuthResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Invalid payload or validation failure",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Invalid email or password",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = "Account deactivated",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "423",
      description = "Account temporarily locked due to repeated failures",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<AuthResponse> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
    String clientIp = extractClientIp(servletRequest);
    String userAgent = servletRequest.getHeader("User-Agent");
    log.info("REST POST /api/v1/auth/login received for email [{}] from IP [{}]", request.email(), clientIp);

    LoginCommand command =
        LoginCommand.builder()
            .email(request.email())
            .password(request.password())
            .clientIp(clientIp)
            .userAgent(userAgent)
            .build();
    AuthTokensDto tokens = loginUseCase.execute(command);
    log.debug("REST POST /api/v1/auth/login succeeded for user [{}]", tokens.userProfile().id());

    return ResponseEntity.ok(AuthResponse.from(tokens));
  }

  private String extractClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isBlank()) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
