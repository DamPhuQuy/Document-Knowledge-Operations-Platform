package com.platform.app.document.infrastructure.adapters.secondary.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.platform.app.document.infrastructure.adapters.secondary.persistence.entity.DocumentUserAccessJpaEntity;

public interface SpringDataDocumentUserAccessRepository extends JpaRepository<DocumentUserAccessJpaEntity, UUID> {

  List<DocumentUserAccessJpaEntity> findByDocumentId(UUID documentId);

  void deleteByDocumentId(UUID documentId);
}
