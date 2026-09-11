package com.platform.app.shared.domain;

import lombok.Builder;

@Builder
public record UserFlags(
    boolean enabled,
    boolean isInternal
) {
    public static UserFlags of(boolean enabled, boolean isInternal) {
        return new UserFlags(enabled, isInternal);
    }
}
