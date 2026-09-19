package com.platform.app.document.infrastructure.adapters.secondary.persistence.repository;

import com.platform.app.document.infrastructure.adapters.secondary.persistence.entity.DocumentUserAccessJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataDocumentUserAccessRepository
    extends JpaRepository<DocumentUserAccessJpaEntity, UUID>
{
    List<DocumentUserAccessJpaEntity> findByDocumentId(UUID documentId);

    void deleteByDocumentId(UUID documentId);
}
