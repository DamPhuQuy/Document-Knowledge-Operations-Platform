package com.platform.app.iam.application.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.platform.app.iam.application.ports.inbound.VerifyOtpCommand;
import com.platform.app.iam.application.ports.outbound.OtpRepositoryPort;
import com.platform.app.iam.application.ports.outbound.UserRepositoryPort;
import com.platform.app.iam.domain.exception.InvalidOtpException;
import com.platform.app.iam.domain.exception.OtpExpiredException;
import com.platform.app.iam.domain.model.User;

@ExtendWith(MockitoExtension.class)
class VerifyOtpServiceTest {

  @Mock private OtpRepositoryPort otpRepositoryPort;
  @Mock private UserRepositoryPort userRepositoryPort;

  @InjectMocks private VerifyOtpService verifyOtpService;

  private User user;

  @BeforeEach
  void setUp() {
    user =
        User.builder()
            .id(UUID.randomUUID())
            .email("test@platform.com")
            .passwordHash("hashed")
            .fullName("Test User")
            .departmentId(null)
            .enabled(false)
            .internal(true)
            .roles(Set.of())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
  }

  @Test
  @DisplayName("Should verify OTP and enable user successfully")
  void shouldVerifyOtpAndEnableUserSuccessfully() {
    when(otpRepositoryPort.getOtp("test@platform.com")).thenReturn(Optional.of("123456"));
    when(userRepositoryPort.findByEmail("test@platform.com")).thenReturn(Optional.of(user));

    VerifyOtpCommand command = new VerifyOtpCommand("test@platform.com", "123456");
    verifyOtpService.execute(command);

    verify(otpRepositoryPort).deleteOtp("test@platform.com");
    verify(userRepositoryPort).save(user);
    assertTrue(user.isEnabled());
  }

  @Test
  @DisplayName("Should throw OtpExpiredException when OTP is not found or expired")
  void shouldThrowOtpExpiredExceptionWhenOtpNotFound() {
    when(otpRepositoryPort.getOtp("test@platform.com")).thenReturn(Optional.empty());

    VerifyOtpCommand command = new VerifyOtpCommand("test@platform.com", "123456");
    assertThrows(OtpExpiredException.class, () -> verifyOtpService.execute(command));

    verify(otpRepositoryPort, never()).deleteOtp(any());
    verify(userRepositoryPort, never()).save(any());
    assertFalse(user.isEnabled());
  }

  @Test
  @DisplayName("Should throw InvalidOtpException when OTP does not match")
  void shouldThrowInvalidOtpExceptionWhenOtpMismatch() {
    when(otpRepositoryPort.getOtp("test@platform.com")).thenReturn(Optional.of("123456"));

    VerifyOtpCommand command = new VerifyOtpCommand("test@platform.com", "999999");
    assertThrows(InvalidOtpException.class, () -> verifyOtpService.execute(command));

    verify(otpRepositoryPort, never()).deleteOtp(any());
    verify(userRepositoryPort, never()).save(any());
    assertFalse(user.isEnabled());
  }
}
