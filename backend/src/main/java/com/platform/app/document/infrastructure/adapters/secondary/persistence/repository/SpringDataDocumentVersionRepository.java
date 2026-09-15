package com.platform.app.document.infrastructure.adapters.secondary.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.platform.app.document.infrastructure.adapters.secondary.persistence.entity.DocumentVersionJpaEntity;

@Repository
public interface SpringDataDocumentVersionRepository extends JpaRepository<DocumentVersionJpaEntity, UUID> {

  List<DocumentVersionJpaEntity> findByDocumentIdOrderByVersionNumberDesc(UUID documentId);

  Optional<DocumentVersionJpaEntity> findByDocumentIdAndVersionNumber(UUID documentId, Integer versionNumber);
}
