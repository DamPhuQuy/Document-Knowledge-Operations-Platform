package com.platform.app.iam.infrastructure.adapters.secondary.persistence.repository;

import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.RefreshTokenJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataRefreshTokenRepository
    extends JpaRepository<RefreshTokenJpaEntity, UUID>
{
    Optional<RefreshTokenJpaEntity> findByToken(String token);
}
