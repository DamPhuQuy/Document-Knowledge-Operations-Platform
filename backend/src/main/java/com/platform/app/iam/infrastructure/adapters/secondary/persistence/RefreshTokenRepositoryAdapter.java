package com.platform.app.iam.infrastructure.adapters.secondary.persistence;

import com.platform.app.iam.application.ports.outbound.RefreshTokenRepositoryPort;
import com.platform.app.iam.domain.model.RefreshToken;
import com.platform.app.iam.domain.model.UserId;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

  private final SpringDataRefreshTokenRepository springDataRefreshTokenRepository;

  public RefreshTokenRepositoryAdapter(
      SpringDataRefreshTokenRepository springDataRefreshTokenRepository) {
    this.springDataRefreshTokenRepository =
        Objects.requireNonNull(
            springDataRefreshTokenRepository, "springDataRefreshTokenRepository must not be null");
  }

  @Override
  @Transactional
  public RefreshToken save(RefreshToken refreshToken) {
    Objects.requireNonNull(refreshToken, "refreshToken must not be null");
    RefreshTokenJpaEntity entity =
        new RefreshTokenJpaEntity(
            refreshToken.getId(),
            refreshToken.getUserId().value(),
            refreshToken.getToken(),
            refreshToken.getExpiryDate(),
            refreshToken.isRevoked(),
            refreshToken.getCreatedAt());

    RefreshTokenJpaEntity saved = springDataRefreshTokenRepository.save(entity);
    return toDomain(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<RefreshToken> findByToken(String token) {
    if (token == null) {
      return Optional.empty();
    }
    return springDataRefreshTokenRepository.findByToken(token).map(this::toDomain);
  }

  private RefreshToken toDomain(RefreshTokenJpaEntity entity) {
    return new RefreshToken(
        entity.getId(),
        UserId.from(entity.getUserId()),
        entity.getToken(),
        entity.getExpiryDate(),
        entity.isRevoked(),
        entity.getCreatedAt());
  }
}
