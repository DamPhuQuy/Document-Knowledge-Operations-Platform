package com.platform.app.iam.application.ports.inbound;

import java.util.Objects;
import lombok.Builder;

@Builder
public record LoginCommand(
    String email,
    String password,
    String clientIp,
    String userAgent
) {
    public LoginCommand {
        Objects.requireNonNull(email, "Email must not be null");
        Objects.requireNonNull(password, "Password must not be null");
    }
}
