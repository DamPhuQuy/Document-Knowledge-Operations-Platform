package com.platform.app.iam.application.dto;

public record AuthTokensDto(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    UserProfileDto userProfile
) {
    public static AuthTokensDto ofBearer(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserProfileDto userProfile
    ) {
        return new AuthTokensDto(
            accessToken,
            refreshToken,
            "Bearer",
            expiresIn,
            userProfile
        );
    }
}
