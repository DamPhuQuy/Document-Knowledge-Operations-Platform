package com.platform.app.iam.infrastructure.adapters.secondary.notification;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@SuppressWarnings("unchecked")
class SmtpEmailNotificationAdapterTest {

  private JavaMailSender mailSender;
  private ObjectProvider<JavaMailSender> mailSenderProvider;
  private SmtpEmailNotificationAdapter adapter;

  @BeforeEach
  void setUp() {
    mailSender = mock(JavaMailSender.class);
    mailSenderProvider = mock(ObjectProvider.class);
    when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
    adapter = new SmtpEmailNotificationAdapter(mailSenderProvider);
  }

  @Test
  @DisplayName("Should skip sending and log simulated email when mail sender is not available")
  void shouldSkipSendingWhenMailSenderNotAvailable() {
    when(mailSenderProvider.getIfAvailable()).thenReturn(null);
    ReflectionTestUtils.setField(adapter, "username", "smtp_user");
    ReflectionTestUtils.setField(adapter, "fromAddress", "noreply@docops.internal");

    assertThatCode(() -> adapter.sendOtpEmail("user@example.com", "123456", "John Doe"))
        .doesNotThrowAnyException();

    verify(mailSender, never()).createMimeMessage();
    verify(mailSender, never()).send(any(MimeMessage.class));
  }

  @Test
  @DisplayName("Should skip sending and log simulated email when username is blank")
  void shouldSkipSendingWhenUsernameIsBlank() {
    ReflectionTestUtils.setField(adapter, "username", "");
    ReflectionTestUtils.setField(adapter, "fromAddress", "noreply@docops.internal");

    assertThatCode(() -> adapter.sendOtpEmail("user@example.com", "123456", "John Doe"))
        .doesNotThrowAnyException();

    verify(mailSender, never()).createMimeMessage();
    verify(mailSender, never()).send(any(MimeMessage.class));
  }

  @Test
  @DisplayName("Should send email when username is configured and mail sender is available")
  void shouldSendEmailWhenUsernameIsConfigured() {
    ReflectionTestUtils.setField(adapter, "username", "smtp_user");
    ReflectionTestUtils.setField(adapter, "fromAddress", "noreply@docops.internal");

    MimeMessage mimeMessage = new MimeMessage((Session) null);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

    assertThatCode(() -> adapter.sendOtpEmail("user@example.com", "123456", "John Doe"))
        .doesNotThrowAnyException();

    verify(mailSender).createMimeMessage();
    verify(mailSender).send(mimeMessage);
  }

  @Test
  @DisplayName("Should catch exception and log fallback OTP when sending fails")
  void shouldHandleSendExceptionGracefully() {
    ReflectionTestUtils.setField(adapter, "username", "smtp_user");
    ReflectionTestUtils.setField(adapter, "fromAddress", "noreply@docops.internal");

    MimeMessage mimeMessage = new MimeMessage((Session) null);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    doThrow(new MailSendException("SMTP server connection timeout"))
        .when(mailSender)
        .send(any(MimeMessage.class));

    assertThatCode(() -> adapter.sendOtpEmail("user@example.com", "123456", "John Doe"))
        .doesNotThrowAnyException();

    verify(mailSender).send(mimeMessage);
  }
}
