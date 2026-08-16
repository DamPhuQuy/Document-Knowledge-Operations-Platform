package com.platform.app.iam.web;

import com.platform.app.iam.service.IamService;
import com.platform.app.iam.web.dto.AuthResponse;
import com.platform.app.iam.web.dto.LoginRequest;
import com.platform.app.iam.web.dto.RegisterRequest;
import com.platform.app.iam.web.dto.UserResponse;
import com.platform.app.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Identity & Access Management", description = "Endpoints for user authentication and authorization")
public class AuthController {

    private final IamService iamService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account and returns JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = iamService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("User registered successfully", response));
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticates credentials and returns JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = iamService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get Current User Profile", description = "Returns details of the currently authenticated user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse response = iamService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok("User profile retrieved", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Access Token", description = "Issues a new access token using a valid refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestHeader("X-Refresh-Token") String refreshToken
    ) {
        AuthResponse response = iamService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed successfully", response));
    }
}
