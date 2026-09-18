package com.platform.app.document.infrastructure.adapters.secondary.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.platform.app.document.infrastructure.adapters.secondary.persistence.entity.DocumentJpaEntity;

public interface SpringDataDocumentRepository extends JpaRepository<DocumentJpaEntity, UUID> {
}
