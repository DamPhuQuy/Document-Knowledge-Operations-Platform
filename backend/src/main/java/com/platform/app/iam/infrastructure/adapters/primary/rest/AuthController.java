package com.platform.app.iam.infrastructure.adapters.primary.rest;

import com.platform.app.iam.application.dto.AuthTokensDto;
import com.platform.app.iam.application.ports.inbound.LoginCommand;
import com.platform.app.iam.application.ports.inbound.LoginUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication and session management")
public class AuthController {

  private final LoginUseCase loginUseCase;

  public AuthController(LoginUseCase loginUseCase) {
    this.loginUseCase = Objects.requireNonNull(loginUseCase, "loginUseCase must not be null");
  }

  @PostMapping("/login")
  @Operation(
      summary = "User Login",
      description =
          "Authenticates user credentials and issues a JWT Access Token and long-lived Refresh Token")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Authentication successful",
        content = @Content(schema = @Schema(implementation = AuthResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid payload or validation failure",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Invalid email or password",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Account deactivated",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "423",
        description = "Account temporarily locked due to repeated failures",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<AuthResponse> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
    String clientIp = extractClientIp(servletRequest);
    String userAgent = servletRequest.getHeader("User-Agent");

    LoginCommand command =
        new LoginCommand(request.email(), request.password(), clientIp, userAgent);
    AuthTokensDto tokens = loginUseCase.execute(command);

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
