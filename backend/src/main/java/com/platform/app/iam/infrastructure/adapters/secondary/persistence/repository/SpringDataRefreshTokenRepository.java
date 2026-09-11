package com.platform.app.iam.infrastructure.adapters.secondary.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.RefreshTokenJpaEntity;

public interface SpringDataRefreshTokenRepository
    extends JpaRepository<RefreshTokenJpaEntity, UUID> {

  Optional<RefreshTokenJpaEntity> findByToken(String token);
}
