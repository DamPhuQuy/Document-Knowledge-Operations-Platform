package com.platform.app.iam.infrastructure.adapters.secondary.persistence.adapter;

import com.platform.app.iam.application.ports.outbound.DepartmentRepositoryPort;
import com.platform.app.iam.domain.model.Department;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.DepartmentJpaEntity;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.repository.SpringDataDepartmentRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DepartmentRepositoryAdapter implements DepartmentRepositoryPort {

    private final SpringDataDepartmentRepository springDataDepartmentRepository;

    @Override
    @Transactional
    public Department save(Department department) {
        Objects.requireNonNull(department, "department must not be null");

        DepartmentJpaEntity entity = springDataDepartmentRepository
            .findById(department.getId())
            .orElseGet(() ->
                DepartmentJpaEntity.builder()
                    .id(department.getId())
                    .createdAt(department.getCreatedAt())
                    .build()
            );

        entity.setCode(department.getCode());
        entity.setName(department.getName());
        entity.setDescription(department.getDescription());
        entity.setUpdatedAt(department.getUpdatedAt());

        DepartmentJpaEntity saved = springDataDepartmentRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Department> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return springDataDepartmentRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Department> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return springDataDepartmentRepository
            .findByCodeIgnoreCase(code.trim())
            .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCode(String code) {
        if (code == null) {
            return false;
        }
        return springDataDepartmentRepository.existsByCodeIgnoreCase(
            code.trim()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCodeAndIdNot(String code, UUID id) {
        if (code == null || id == null) {
            return false;
        }
        return springDataDepartmentRepository.existsByCodeIgnoreCaseAndIdNot(
            code.trim(),
            id
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Department> findAll() {
        return springDataDepartmentRepository
            .findAll()
            .stream()
            .map(this::toDomain)
            .toList();
    }

    private Department toDomain(DepartmentJpaEntity entity) {
        return Department.builder()
            .id(entity.getId())
            .code(entity.getCode())
            .name(entity.getName())
            .description(entity.getDescription())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
