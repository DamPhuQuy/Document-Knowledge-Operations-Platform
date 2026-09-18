package com.platform.app.iam.application.ports.outbound;

public interface EmailNotificationPort {
  void sendOtpEmail(String email, String otp, String fullName);
}
