package com.platform.app.iam.application.ports.inbound;

import java.util.Objects;
import lombok.Builder;

@Builder
public record VerifyOtpCommand(String email, String otp) {
  public VerifyOtpCommand {
    Objects.requireNonNull(email, "Email must not be null");
    Objects.requireNonNull(otp, "OTP must not be null");
  }
}
