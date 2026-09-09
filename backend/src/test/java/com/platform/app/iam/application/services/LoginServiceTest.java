package com.platform.app.iam.application.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.platform.app.iam.application.dto.AuthTokensDto;
import com.platform.app.iam.application.dto.UserLoginFailedEvent;
import com.platform.app.iam.application.dto.UserLoginSuccessEvent;
import com.platform.app.iam.application.ports.inbound.LoginCommand;
import com.platform.app.iam.application.ports.outbound.AccountLockoutPort;
import com.platform.app.iam.application.ports.outbound.EventPublisherPort;
import com.platform.app.iam.application.ports.outbound.PasswordEncoderPort;
import com.platform.app.iam.application.ports.outbound.RefreshTokenRepositoryPort;
import com.platform.app.iam.application.ports.outbound.TokenProviderPort;
import com.platform.app.iam.application.ports.outbound.UserRepositoryPort;
import com.platform.app.iam.domain.exception.AccountDisabledException;
import com.platform.app.iam.domain.exception.AccountLockedException;
import com.platform.app.iam.domain.exception.InvalidCredentialsException;
import com.platform.app.iam.domain.model.RefreshToken;
import com.platform.app.iam.domain.model.User;
import com.platform.app.iam.domain.model.UserId;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

  @Mock private UserRepositoryPort userRepositoryPort;
  @Mock private RefreshTokenRepositoryPort refreshTokenRepositoryPort;
  @Mock private PasswordEncoderPort passwordEncoderPort;
  @Mock private TokenProviderPort tokenProviderPort;
  @Mock private AccountLockoutPort accountLockoutPort;
  @Mock private EventPublisherPort eventPublisherPort;

  private LoginService loginService;

  @BeforeEach
  void setUp() {
    loginService =
        new LoginService(
            userRepositoryPort,
            refreshTokenRepositoryPort,
            passwordEncoderPort,
            tokenProviderPort,
            accountLockoutPort,
            eventPublisherPort);
  }

  @Test
  @DisplayName("Should successfully authenticate valid credentials and issue tokens")
  void shouldAuthenticateSuccessfully() {
    String email = "test@platform.com";
    String rawPassword = "password123";
    String hashedPassword = "$2a$12$somehashedpassword";
    UserId userId = UserId.generate();

    User user =
        new User(
            userId,
            email,
            hashedPassword,
            "Test User",
            null,
            true,
            true,
            Set.of(),
            Instant.now(),
            Instant.now());

    when(accountLockoutPort.isLocked(email)).thenReturn(false);
    when(userRepositoryPort.findByEmail(email)).thenReturn(Optional.of(user));
    when(passwordEncoderPort.matches(rawPassword, hashedPassword)).thenReturn(true);
    when(tokenProviderPort.generateAccessToken(user)).thenReturn("mock.jwt.token");
    when(tokenProviderPort.generateRefreshTokenString()).thenReturn("mock-refresh-token-opaque");
    when(tokenProviderPort.getAccessTokenExpirationSeconds()).thenReturn(3600L);

    LoginCommand command = new LoginCommand(email, rawPassword, "127.0.0.1", "JUnit-Agent");
    AuthTokensDto result = loginService.execute(command);

    assertNotNull(result);
    assertEquals("mock.jwt.token", result.accessToken());
    assertEquals("mock-refresh-token-opaque", result.refreshToken());
    assertEquals(3600L, result.expiresIn());
    assertEquals(email, result.userProfile().email());

    verify(accountLockoutPort).resetAttempts(email);
    verify(refreshTokenRepositoryPort).save(any(RefreshToken.class));
    verify(eventPublisherPort).publish(any(UserLoginSuccessEvent.class));
  }

  @Test
  @DisplayName("Should throw AccountLockedException when account is temporarily locked")
  void shouldThrowAccountLockedException() {
    String email = "locked@platform.com";
    when(accountLockoutPort.isLocked(email)).thenReturn(true);

    LoginCommand command = new LoginCommand(email, "anyPassword", "127.0.0.1", "JUnit-Agent");

    assertThrows(AccountLockedException.class, () -> loginService.execute(command));
    verify(eventPublisherPort).publish(any(UserLoginFailedEvent.class));
    verify(userRepositoryPort, never()).findByEmail(anyString());
  }

  @Test
  @DisplayName("Should throw InvalidCredentialsException when email is not found")
  void shouldThrowWhenUserNotFound() {
    String email = "notfound@platform.com";
    when(accountLockoutPort.isLocked(email)).thenReturn(false);
    when(userRepositoryPort.findByEmail(email)).thenReturn(Optional.empty());

    LoginCommand command = new LoginCommand(email, "anyPassword", "127.0.0.1", "JUnit-Agent");

    assertThrows(InvalidCredentialsException.class, () -> loginService.execute(command));
    verify(accountLockoutPort).recordFailure(email);
    verify(eventPublisherPort).publish(any(UserLoginFailedEvent.class));
  }

  @Test
  @DisplayName("Should throw AccountDisabledException when account is deactivated")
  void shouldThrowWhenAccountDisabled() {
    String email = "disabled@platform.com";
    User user =
        new User(
            UserId.generate(),
            email,
            "$2a$12$hash",
            "Disabled User",
            null,
            false,
            true,
            Set.of(),
            Instant.now(),
            Instant.now());

    when(accountLockoutPort.isLocked(email)).thenReturn(false);
    when(userRepositoryPort.findByEmail(email)).thenReturn(Optional.of(user));

    LoginCommand command = new LoginCommand(email, "password", "127.0.0.1", "JUnit-Agent");

    assertThrows(AccountDisabledException.class, () -> loginService.execute(command));
    verify(eventPublisherPort).publish(any(UserLoginFailedEvent.class));
  }

  @Test
  @DisplayName("Should throw InvalidCredentialsException when password does not match")
  void shouldThrowWhenPasswordMismatch() {
    String email = "test@platform.com";
    User user =
        new User(
            UserId.generate(),
            email,
            "$2a$12$correcthash",
            "Test User",
            null,
            true,
            true,
            Set.of(),
            Instant.now(),
            Instant.now());

    when(accountLockoutPort.isLocked(email)).thenReturn(false);
    when(userRepositoryPort.findByEmail(email)).thenReturn(Optional.of(user));
    when(passwordEncoderPort.matches("wrongPassword", "$2a$12$correcthash")).thenReturn(false);

    LoginCommand command = new LoginCommand(email, "wrongPassword", "127.0.0.1", "JUnit-Agent");

    assertThrows(InvalidCredentialsException.class, () -> loginService.execute(command));
    verify(accountLockoutPort).recordFailure(email);
    verify(eventPublisherPort).publish(any(UserLoginFailedEvent.class));
  }
}
