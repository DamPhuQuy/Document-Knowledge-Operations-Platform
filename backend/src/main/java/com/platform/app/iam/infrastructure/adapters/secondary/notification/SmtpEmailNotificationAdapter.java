package com.platform.app.iam.infrastructure.adapters.secondary.notification;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.platform.app.iam.application.ports.outbound.EmailNotificationPort;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailNotificationAdapter implements EmailNotificationPort {

  private final ObjectProvider<JavaMailSender> mailSenderProvider;

  @Value("${spring.mail.username:}")
  private String username;

  @Value("${app.mail.from:noreply@docops.internal}")
  private String fromAddress;

  @Override
  public void sendOtpEmail(String email, String otp, String fullName) {
    JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
    if (mailSender == null || username == null || username.isBlank()) {
      log.info(
          "[ASYNC EMAIL NOTIFICATION - NO SMTP CONFIGURED] To: {} <{}> | Registration OTP: [{}] (Valid for 5 minutes)",
          fullName,
          email,
          otp);
      return;
    }

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      if (fromAddress != null && !fromAddress.isBlank()) {
        try {
          helper.setFrom(fromAddress, "DocOps Platform");
        } catch (java.io.UnsupportedEncodingException e) {
          helper.setFrom(fromAddress);
        }
      }
      helper.setTo(email);
      helper.setSubject("Document Operations Platform - Registration OTP Verification");

      String htmlBody = buildOtpHtmlContent(fullName, otp);
      helper.setText(htmlBody, true);

      mailSender.send(message);
      log.info("[SMTP EMAIL SENT] Verification OTP sent successfully to {} <{}>", fullName, email);
    } catch (MessagingException | RuntimeException ex) {
      String causeMsg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
      log.error(
          "[SMTP EMAIL ERROR] Failed to send OTP email to {} <{}>: {} (Cause: {}). Fallback OTP in logs: [{}]",
          fullName,
          email,
          ex.getMessage(),
          causeMsg,
          otp);
    }
  }

  private String buildOtpHtmlContent(String fullName, String otp) {
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="UTF-8">
          <style>
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 20px; color: #1e293b; }
            .card { max-width: 520px; margin: 0 auto; background: #ffffff; border-radius: 8px; border: 1px solid #e2e8f0; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }
            .header { background: #0f172a; padding: 24px; text-align: center; color: #ffffff; }
            .content { padding: 32px 24px; }
            .otp-box { margin: 24px 0; background: #f1f5f9; border-radius: 6px; padding: 18px; text-align: center; font-size: 32px; font-weight: 700; letter-spacing: 6px; color: #0284c7; }
            .footer { background: #f8fafc; padding: 16px 24px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #e2e8f0; }
          </style>
        </head>
        <body>
          <div class="card">
            <div class="header">
              <h2 style="margin: 0; font-size: 20px; font-weight: 600;">DocOps Platform</h2>
            </div>
            <div class="content">
              <p>Hello <strong>%s</strong>,</p>
              <p>Thank you for signing up for the Document Knowledge & Operations Platform. Please use the verification code below to activate your account:</p>
              <div class="otp-box">%s</div>
              <p style="font-size: 13px; color: #64748b;">This code will expire in <strong>5 minutes</strong>. If you did not request this code, you can safely ignore this email.</p>
            </div>
            <div class="footer">
              &copy; Document Knowledge & Operations Platform. All rights reserved.
            </div>
          </div>
        </body>
        </html>
        """.formatted(fullName != null ? fullName : "User", otp);
  }
}
