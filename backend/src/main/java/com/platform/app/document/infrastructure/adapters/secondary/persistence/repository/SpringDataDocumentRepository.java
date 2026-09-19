package com.platform.app.document.infrastructure.adapters.secondary.persistence.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.platform.app.document.infrastructure.adapters.secondary.persistence.entity.DocumentJpaEntity;

public interface SpringDataDocumentRepository extends JpaRepository<DocumentJpaEntity, UUID> {

  Optional<DocumentJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

  boolean existsByIdAndDeletedAtIsNull(UUID id);

  @Modifying
  @Query("UPDATE DocumentJpaEntity d SET d.deletedAt = :deletedAt, d.updatedAt = :deletedAt WHERE d.id = :id AND d.deletedAt IS NULL")
  int softDeleteById(@Param("id") UUID id, @Param("deletedAt") Instant deletedAt);
}
