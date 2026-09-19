package com.platform.app.iam.infrastructure.adapters.primary.rest.dto.response;

import lombok.Builder;

@Builder
public record VerifyOtpResponse(String message, boolean activated) {
    public static VerifyOtpResponse success() {
        return new VerifyOtpResponse(
            "Account verified and activated successfully.",
            true
        );
    }
}
