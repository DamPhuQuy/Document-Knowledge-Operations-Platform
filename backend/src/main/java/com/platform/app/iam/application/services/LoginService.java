package com.platform.app.iam.application.services;

import com.platform.app.iam.application.dto.AuthTokensDto;
import com.platform.app.iam.application.dto.UserLoginFailedEvent;
import com.platform.app.iam.application.dto.UserLoginSuccessEvent;
import com.platform.app.iam.application.dto.UserProfileDto;
import com.platform.app.iam.application.ports.inbound.LoginCommand;
import com.platform.app.iam.application.ports.inbound.LoginUseCase;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class LoginService implements LoginUseCase {

  private final UserRepositoryPort userRepositoryPort;
  private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
  private final PasswordEncoderPort passwordEncoderPort;
  private final TokenProviderPort tokenProviderPort;
  private final AccountLockoutPort accountLockoutPort;
  private final EventPublisherPort eventPublisherPort;

  public LoginService(
      UserRepositoryPort userRepositoryPort,
      RefreshTokenRepositoryPort refreshTokenRepositoryPort,
      PasswordEncoderPort passwordEncoderPort,
      TokenProviderPort tokenProviderPort,
      AccountLockoutPort accountLockoutPort,
      EventPublisherPort eventPublisherPort) {
    this.userRepositoryPort =
        Objects.requireNonNull(userRepositoryPort, "userRepositoryPort must not be null");
    this.refreshTokenRepositoryPort =
        Objects.requireNonNull(
            refreshTokenRepositoryPort, "refreshTokenRepositoryPort must not be null");
    this.passwordEncoderPort =
        Objects.requireNonNull(passwordEncoderPort, "passwordEncoderPort must not be null");
    this.tokenProviderPort =
        Objects.requireNonNull(tokenProviderPort, "tokenProviderPort must not be null");
    this.accountLockoutPort =
        Objects.requireNonNull(accountLockoutPort, "accountLockoutPort must not be null");
    this.eventPublisherPort =
        Objects.requireNonNull(eventPublisherPort, "eventPublisherPort must not be null");
  }

  @Override
  public AuthTokensDto execute(LoginCommand command) {
    Objects.requireNonNull(command, "LoginCommand must not be null");
    String email = command.email().trim().toLowerCase();

    // 1. Check account lockout policy (Rule B4)
    if (accountLockoutPort.isLocked(email)) {
      eventPublisherPort.publish(
          new UserLoginFailedEvent(
              email, command.clientIp(), command.userAgent(), "ACCOUNT_LOCKED", Instant.now()));
      throw new AccountLockedException(
          "Account is temporarily locked due to 5 consecutive failed login attempts. Please try again after 15 minutes.");
    }

    // 2. Lookup user
    Optional<User> userOptional = userRepositoryPort.findByEmail(email);
    if (userOptional.isEmpty()) {
      accountLockoutPort.recordFailure(email);
      eventPublisherPort.publish(
          new UserLoginFailedEvent(
              email, command.clientIp(), command.userAgent(), "USER_NOT_FOUND", Instant.now()));
      throw new InvalidCredentialsException("Invalid email or password");
    }

    User user = userOptional.get();

    // 3. Verify user is active/enabled
    if (!user.isEnabled()) {
      eventPublisherPort.publish(
          new UserLoginFailedEvent(
              email, command.clientIp(), command.userAgent(), "ACCOUNT_DISABLED", Instant.now()));
      throw new AccountDisabledException("Account is deactivated");
    }

    // 4. Verify password against BCrypt hash (Rule B1)
    if (!passwordEncoderPort.matches(command.password(), user.getPasswordHash())) {
      accountLockoutPort.recordFailure(email);
      eventPublisherPort.publish(
          new UserLoginFailedEvent(
              email, command.clientIp(), command.userAgent(), "INVALID_PASSWORD", Instant.now()));
      throw new InvalidCredentialsException("Invalid email or password");
    }

    // 5. Reset lockout counter on successful authentication
    accountLockoutPort.resetAttempts(email);

    // 6. Generate access token & refresh token
    String accessToken = tokenProviderPort.generateAccessToken(user);
    String refreshTokenString = tokenProviderPort.generateRefreshTokenString();
    long expiresIn = tokenProviderPort.getAccessTokenExpirationSeconds();

    // 7. Persist refresh token (30-day lifetime per Rule B3)
    Instant refreshExpiryDate = Instant.now().plus(30, ChronoUnit.DAYS);
    RefreshToken refreshToken =
        RefreshToken.create(user.getId(), refreshTokenString, refreshExpiryDate);
    refreshTokenRepositoryPort.save(refreshToken);

    // 8. Publish login success domain event (ready for future UC-AUDIT-01 ingestion)
    eventPublisherPort.publish(
        new UserLoginSuccessEvent(
            user.getId().value(),
            user.getEmail(),
            command.clientIp(),
            command.userAgent(),
            Instant.now()));

    // 9. Build response
    UserProfileDto userProfile =
        new UserProfileDto(
            user.getId().value(),
            user.getEmail(),
            user.getFullName(),
            user.getDepartmentId() != null ? user.getDepartmentId().value() : null,
            user.isInternal(),
            user.getRoleCodes(),
            user.getAllPermissionCodes());

    return AuthTokensDto.ofBearer(accessToken, refreshTokenString, expiresIn, userProfile);
  }
}
