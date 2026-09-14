package com.platform.app.iam.infrastructure.adapters.primary.rest.dto.response;

import com.platform.app.iam.application.dto.AuthTokensDto;
import com.platform.app.iam.application.dto.UserProfileDto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    UserProfileDto user) {

  public static AuthResponse from(AuthTokensDto dto) {
    return new AuthResponse(
        dto.accessToken(),
        dto.refreshToken(),
        dto.tokenType(),
        dto.expiresIn(),
        dto.userProfile());
  }
}
