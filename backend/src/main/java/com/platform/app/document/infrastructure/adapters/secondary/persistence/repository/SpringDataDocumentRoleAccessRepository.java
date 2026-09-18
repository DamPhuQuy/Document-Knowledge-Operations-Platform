package com.platform.app.document.infrastructure.adapters.secondary.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.platform.app.document.infrastructure.adapters.secondary.persistence.entity.DocumentRoleAccessJpaEntity;

public interface SpringDataDocumentRoleAccessRepository extends JpaRepository<DocumentRoleAccessJpaEntity, UUID> {

  List<DocumentRoleAccessJpaEntity> findByDocumentId(UUID documentId);

  void deleteByDocumentId(UUID documentId);
}
