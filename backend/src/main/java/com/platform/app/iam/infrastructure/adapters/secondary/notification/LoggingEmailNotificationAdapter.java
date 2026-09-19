package com.platform.app.iam.infrastructure.adapters.secondary.notification;

import org.springframework.stereotype.Component;

import com.platform.app.iam.application.ports.outbound.EmailNotificationPort;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LoggingEmailNotificationAdapter implements EmailNotificationPort {

  @Override
  public void sendOtpEmail(String email, String otp, String fullName) {
    log.info(
        "[ASYNC EMAIL SENT] To: {} <{}> | Registration OTP: [{}] (Valid for 5 minutes)",
        fullName,
        email,
        otp);
  }
}
