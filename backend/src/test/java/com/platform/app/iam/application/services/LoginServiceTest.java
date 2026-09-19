package com.platform.app.iam.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.platform.app.iam.application.dto.AuthTokensDto;
import com.platform.app.iam.application.dto.UserLoginFailedEvent;
import com.platform.app.iam.application.dto.UserLoginSuccessEvent;
import com.platform.app.iam.application.ports.inbound.LoginCommand;
import com.platform.app.iam.application.ports.outbound.AccountLockoutPort;
import com.platform.app.iam.application.ports.outbound.RefreshTokenRepositoryPort;
import com.platform.app.iam.application.ports.outbound.TokenProviderPort;
import com.platform.app.iam.application.ports.outbound.UserRepositoryPort;
import com.platform.app.iam.domain.exception.AccountDisabledException;
import com.platform.app.iam.domain.exception.AccountLockedException;
import com.platform.app.iam.domain.exception.InvalidCredentialsException;
import com.platform.app.iam.domain.model.RefreshToken;
import com.platform.app.iam.domain.model.User;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepositoryPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenProviderPort tokenProviderPort;

    @Mock
    private AccountLockoutPort accountLockoutPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private LoginService loginService;

    @BeforeEach
    void setUp() {
        loginService = new LoginService(
            userRepositoryPort,
            refreshTokenRepositoryPort,
            passwordEncoder,
            tokenProviderPort,
            accountLockoutPort,
            eventPublisher
        );
    }

    @Test
    @DisplayName(
        "Should successfully authenticate valid credentials and issue tokens"
    )
    void shouldAuthenticateSuccessfully() {
        String email = "test@platform.com";
        String rawPassword = "password123";
        String hashedPassword = "$2a$12$somehashedpassword";
        UUID userId = UUID.randomUUID();

        User user = User.builder()
            .id(userId)
            .email(email)
            .passwordHash(hashedPassword)
            .fullName("Test User")
            .departmentId(null)
            .enabled(true)
            .internal(true)
            .roles(Set.of())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(accountLockoutPort.isLocked(email)).thenReturn(false);
        when(userRepositoryPort.findByEmail(email)).thenReturn(
            Optional.of(user)
        );
        when(passwordEncoder.matches(rawPassword, hashedPassword)).thenReturn(
            true
        );
        when(tokenProviderPort.generateAccessToken(user)).thenReturn(
            "mock.jwt.token"
        );
        when(tokenProviderPort.generateRefreshTokenString()).thenReturn(
            "mock-refresh-token-opaque"
        );
        when(tokenProviderPort.getAccessTokenExpirationSeconds()).thenReturn(
            3600L
        );

        LoginCommand command = LoginCommand.builder()
            .email(email)
            .password(rawPassword)
            .clientIp("127.0.0.1")
            .userAgent("JUnit-Agent")
            .build();
        AuthTokensDto result = loginService.execute(command);

        assertNotNull(result);
        assertEquals("mock.jwt.token", result.accessToken());
        assertEquals("mock-refresh-token-opaque", result.refreshToken());
        assertEquals(3600L, result.expiresIn());
        assertEquals(email, result.userProfile().email());

        verify(accountLockoutPort).resetAttempts(email);
        verify(refreshTokenRepositoryPort).save(any(RefreshToken.class));
        verify(eventPublisher).publishEvent(any(UserLoginSuccessEvent.class));
    }

    @Test
    @DisplayName(
        "Should throw AccountLockedException when account is temporarily locked"
    )
    void shouldThrowAccountLockedException() {
        String email = "locked@platform.com";
        when(accountLockoutPort.isLocked(email)).thenReturn(true);

        LoginCommand command = LoginCommand.builder()
            .email(email)
            .password("anyPassword")
            .clientIp("127.0.0.1")
            .userAgent("JUnit-Agent")
            .build();

        assertThrows(AccountLockedException.class, () ->
            loginService.execute(command)
        );
        verify(eventPublisher).publishEvent(any(UserLoginFailedEvent.class));
        verify(userRepositoryPort, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName(
        "Should throw InvalidCredentialsException when email is not found"
    )
    void shouldThrowWhenUserNotFound() {
        String email = "notfound@platform.com";
        when(accountLockoutPort.isLocked(email)).thenReturn(false);
        when(userRepositoryPort.findByEmail(email)).thenReturn(
            Optional.empty()
        );

        LoginCommand command = LoginCommand.builder()
            .email(email)
            .password("anyPassword")
            .clientIp("127.0.0.1")
            .userAgent("JUnit-Agent")
            .build();

        assertThrows(InvalidCredentialsException.class, () ->
            loginService.execute(command)
        );
        verify(accountLockoutPort).recordFailure(email);
        verify(eventPublisher).publishEvent(any(UserLoginFailedEvent.class));
    }

    @Test
    @DisplayName(
        "Should throw AccountDisabledException when account is deactivated"
    )
    void shouldThrowWhenAccountDisabled() {
        String email = "disabled@platform.com";
        User user = User.builder()
            .id(UUID.randomUUID())
            .email(email)
            .passwordHash("$2a$12$hash")
            .fullName("Disabled User")
            .departmentId(null)
            .enabled(false)
            .internal(true)
            .roles(Set.of())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(accountLockoutPort.isLocked(email)).thenReturn(false);
        when(userRepositoryPort.findByEmail(email)).thenReturn(
            Optional.of(user)
        );

        LoginCommand command = LoginCommand.builder()
            .email(email)
            .password("password")
            .clientIp("127.0.0.1")
            .userAgent("JUnit-Agent")
            .build();

        assertThrows(AccountDisabledException.class, () ->
            loginService.execute(command)
        );
        verify(eventPublisher).publishEvent(any(UserLoginFailedEvent.class));
    }

    @Test
    @DisplayName(
        "Should throw InvalidCredentialsException when password does not match"
    )
    void shouldThrowWhenPasswordMismatch() {
        String email = "test@platform.com";
        User user = User.builder()
            .id(UUID.randomUUID())
            .email(email)
            .passwordHash("$2a$12$correcthash")
            .fullName("Test User")
            .departmentId(null)
            .enabled(true)
            .internal(true)
            .roles(Set.of())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(accountLockoutPort.isLocked(email)).thenReturn(false);
        when(userRepositoryPort.findByEmail(email)).thenReturn(
            Optional.of(user)
        );
        when(
            passwordEncoder.matches("wrongPassword", "$2a$12$correcthash")
        ).thenReturn(false);

        LoginCommand command = LoginCommand.builder()
            .email(email)
            .password("wrongPassword")
            .clientIp("127.0.0.1")
            .userAgent("JUnit-Agent")
            .build();

        assertThrows(InvalidCredentialsException.class, () ->
            loginService.execute(command)
        );
        verify(accountLockoutPort).recordFailure(email);
        verify(eventPublisher).publishEvent(any(UserLoginFailedEvent.class));
    }
}
