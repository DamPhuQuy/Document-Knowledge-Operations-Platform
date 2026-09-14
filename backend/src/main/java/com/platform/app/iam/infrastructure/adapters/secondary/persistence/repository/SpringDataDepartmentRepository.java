package com.platform.app.iam.infrastructure.adapters.secondary.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.DepartmentJpaEntity;

@Repository
public interface SpringDataDepartmentRepository extends JpaRepository<DepartmentJpaEntity, UUID> {

  Optional<DepartmentJpaEntity> findByCodeIgnoreCase(String code);

  boolean existsByCodeIgnoreCase(String code);

  boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);
}
