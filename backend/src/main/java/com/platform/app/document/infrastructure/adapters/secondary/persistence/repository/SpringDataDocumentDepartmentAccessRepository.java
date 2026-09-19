package com.platform.app.document.infrastructure.adapters.secondary.persistence.repository;

import com.platform.app.document.infrastructure.adapters.secondary.persistence.entity.DocumentDepartmentAccessJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataDocumentDepartmentAccessRepository
    extends JpaRepository<DocumentDepartmentAccessJpaEntity, UUID>
{
    List<DocumentDepartmentAccessJpaEntity> findByDocumentId(UUID documentId);

    void deleteByDocumentId(UUID documentId);
}
