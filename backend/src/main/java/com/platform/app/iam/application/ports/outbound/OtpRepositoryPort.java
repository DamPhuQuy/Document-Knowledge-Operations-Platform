package com.platform.app.iam.application.ports.outbound;

import java.time.Duration;
import java.util.Optional;

public interface OtpRepositoryPort {
  void saveOtp(String email, String otp, Duration ttl);

  Optional<String> getOtp(String email);

  void deleteOtp(String email);
}
