package com.platform.app.iam.application.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.platform.app.iam.application.dto.AuthTokensDto;
import com.platform.app.iam.application.dto.UserLoginFailedEvent;
import com.platform.app.iam.application.dto.UserLoginSuccessEvent;
import com.platform.app.iam.application.dto.UserProfileDto;
import com.platform.app.iam.application.ports.inbound.LoginCommand;
import com.platform.app.iam.application.ports.inbound.LoginUseCase;
import com.platform.app.iam.application.ports.outbound.AccountLockoutPort;
import com.platform.app.iam.application.ports.outbound.RefreshTokenRepositoryPort;
import com.platform.app.iam.application.ports.outbound.TokenProviderPort;
import com.platform.app.iam.application.ports.outbound.UserRepositoryPort;
import com.platform.app.iam.domain.exception.AccountDisabledException;
import com.platform.app.iam.domain.exception.AccountLockedException;
import com.platform.app.iam.domain.exception.InvalidCredentialsException;
import com.platform.app.iam.domain.model.RefreshToken;
import com.platform.app.iam.domain.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginService implements LoginUseCase {

  private final UserRepositoryPort userRepositoryPort;
  private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
  private final PasswordEncoder passwordEncoder;
  private final TokenProviderPort tokenProviderPort;
  private final AccountLockoutPort accountLockoutPort;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  public AuthTokensDto execute(LoginCommand command) {
    Objects.requireNonNull(command, "LoginCommand must not be null");
    String email = command.email().trim().toLowerCase();

    // 1. Check account lockout policy (Rule B4)
    if (accountLockoutPort.isLocked(email)) {
      log.warn("Login attempt blocked for email [{}]: account locked", email);
      eventPublisher.publishEvent(
          UserLoginFailedEvent.builder()
              .email(email)
              .clientIp(command.clientIp())
              .userAgent(command.userAgent())
              .reason("ACCOUNT_LOCKED")
              .timestamp(Instant.now())
              .build());
      throw new AccountLockedException(
          "Account is temporarily locked due to 5 consecutive failed login attempts. Please try again after 15 minutes.");
    }

    // 2. Lookup user
    Optional<User> userOptional = userRepositoryPort.findByEmail(email);
    if (userOptional.isEmpty()) {
      accountLockoutPort.recordFailure(email);
      log.warn("Login attempt failed for email [{}]: user not found", email);
      eventPublisher.publishEvent(
          UserLoginFailedEvent.builder()
              .email(email)
              .clientIp(command.clientIp())
              .userAgent(command.userAgent())
              .reason("USER_NOT_FOUND")
              .timestamp(Instant.now())
              .build());
      throw new InvalidCredentialsException("Invalid email or password");
    }

    User user = userOptional.get();

    // 3. Verify user is active/enabled
    if (!user.isEnabled()) {
      log.warn("Login attempt rejected for email [{}]: account deactivated", email);
      eventPublisher.publishEvent(
          UserLoginFailedEvent.builder()
              .email(email)
              .clientIp(command.clientIp())
              .userAgent(command.userAgent())
              .reason("ACCOUNT_DISABLED")
              .timestamp(Instant.now())
              .build());
      throw new AccountDisabledException("Account is deactivated");
    }

    // 4. Verify password against BCrypt hash (Rule B1)
    if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
      accountLockoutPort.recordFailure(email);
      log.warn("Login attempt failed for email [{}]: invalid password", email);
      eventPublisher.publishEvent(
          UserLoginFailedEvent.builder()
              .email(email)
              .clientIp(command.clientIp())
              .userAgent(command.userAgent())
              .reason("INVALID_PASSWORD")
              .timestamp(Instant.now())
              .build());
      throw new InvalidCredentialsException("Invalid email or password");
    }

    // 5. Reset lockout counter on successful authentication
    accountLockoutPort.resetAttempts(email);
    log.info("User [{}] authenticated successfully with {} roles", user.getId(), user.getRoleCodes().size());

    // 6. Generate access token & refresh token
    String accessToken = tokenProviderPort.generateAccessToken(user);
    String refreshTokenString = tokenProviderPort.generateRefreshTokenString();
    long expiresIn = tokenProviderPort.getAccessTokenExpirationSeconds();

    // 7. Persist refresh token (30-day lifetime per Rule B3)
    Instant refreshExpiryDate = Instant.now().plus(30, ChronoUnit.DAYS);
    RefreshToken refreshToken =
        RefreshToken.create(user.getId(), refreshTokenString, refreshExpiryDate);
    refreshTokenRepositoryPort.save(refreshToken);

    // 8. Publish login success domain event
    eventPublisher.publishEvent(
        UserLoginSuccessEvent.builder()
            .userId(user.getId())
            .email(user.getEmail())
            .clientIp(command.clientIp())
            .userAgent(command.userAgent())
            .timestamp(Instant.now())
            .build());

    // 9. Build response
    UserProfileDto userProfile =
        UserProfileDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .departmentId(user.getDepartmentId())
            .isInternal(user.isInternal())
            .roles(user.getRoleCodes())
            .permissions(user.getAllPermissionCodes())
            .build();

    return AuthTokensDto.ofBearer(accessToken, refreshTokenString, expiresIn, userProfile);
  }
}
