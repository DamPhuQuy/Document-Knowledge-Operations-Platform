package com.platform.app.iam.application.ports.outbound;

import com.platform.app.iam.domain.model.Department;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepositoryPort {
    Department save(Department department);

    Optional<Department> findById(UUID id);

    Optional<Department> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    List<Department> findAll();
}
