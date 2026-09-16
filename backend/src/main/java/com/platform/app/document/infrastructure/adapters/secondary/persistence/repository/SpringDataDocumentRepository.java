package com.platform.app.document.infrastructure.adapters.secondary.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.platform.app.document.infrastructure.adapters.secondary.persistence.entity.DocumentJpaEntity;

@Repository
public interface SpringDataDocumentRepository extends JpaRepository<DocumentJpaEntity, UUID> {
}
