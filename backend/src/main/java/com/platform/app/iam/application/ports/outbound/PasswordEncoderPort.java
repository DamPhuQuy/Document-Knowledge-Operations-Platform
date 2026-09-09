package com.platform.app.iam.application.ports.outbound;

public interface PasswordEncoderPort {
  boolean matches(String rawPassword, String encodedPassword);

  String encode(String rawPassword);
}
