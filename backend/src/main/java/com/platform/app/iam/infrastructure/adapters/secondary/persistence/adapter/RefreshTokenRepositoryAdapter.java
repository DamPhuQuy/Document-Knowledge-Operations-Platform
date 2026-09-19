package com.platform.app.iam.infrastructure.adapters.secondary.persistence.adapter;

import com.platform.app.iam.application.ports.outbound.RefreshTokenRepositoryPort;
import com.platform.app.iam.domain.model.RefreshToken;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.RefreshTokenJpaEntity;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.repository.SpringDataRefreshTokenRepository;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter
    implements RefreshTokenRepositoryPort
{

    private final SpringDataRefreshTokenRepository springDataRefreshTokenRepository;

    @Override
    @Transactional
    public RefreshToken save(RefreshToken refreshToken) {
        Objects.requireNonNull(refreshToken, "refreshToken must not be null");
        RefreshTokenJpaEntity entity = RefreshTokenJpaEntity.builder()
            .id(refreshToken.getId())
            .userId(refreshToken.getUserId())
            .token(refreshToken.getToken())
            .expiryDate(refreshToken.getExpiryDate())
            .revoked(refreshToken.isRevoked())
            .createdAt(refreshToken.getCreatedAt())
            .build();

        RefreshTokenJpaEntity saved = springDataRefreshTokenRepository.save(
            entity
        );
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String token) {
        if (token == null) {
            return Optional.empty();
        }
        return springDataRefreshTokenRepository
            .findByToken(token)
            .map(this::toDomain);
    }

    private RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return RefreshToken.builder()
            .id(entity.getId())
            .userId(entity.getUserId())
            .token(entity.getToken())
            .expiryDate(entity.getExpiryDate())
            .revoked(entity.isRevoked())
            .createdAt(entity.getCreatedAt())
            .build();
    }
}
