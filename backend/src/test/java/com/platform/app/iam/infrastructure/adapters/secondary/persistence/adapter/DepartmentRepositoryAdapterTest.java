package com.platform.app.iam.infrastructure.adapters.secondary.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.platform.app.iam.domain.model.Department;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(DepartmentRepositoryAdapter.class)
class DepartmentRepositoryAdapterTest {

    @Autowired
    private DepartmentRepositoryAdapter departmentRepositoryAdapter;

    @Test
    @DisplayName("Should save, findById, and findByCode a department")
    void shouldSaveAndFindDepartment() {
        UUID deptId = UUID.randomUUID();
        Department dept = Department.builder()
            .id(deptId)
            .code("HR")
            .name("Human Resources")
            .description("HR Dept")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        Department saved = departmentRepositoryAdapter.save(dept);
        assertNotNull(saved);
        assertEquals(deptId, saved.getId());
        assertEquals("HR", saved.getCode());

        Optional<Department> foundById = departmentRepositoryAdapter.findById(
            deptId
        );
        assertTrue(foundById.isPresent());
        assertEquals("Human Resources", foundById.get().getName());

        Optional<Department> foundByCode =
            departmentRepositoryAdapter.findByCode("hr");
        assertTrue(foundByCode.isPresent());
        assertEquals(deptId, foundByCode.get().getId());

        assertTrue(departmentRepositoryAdapter.existsByCode("HR"));
        assertFalse(departmentRepositoryAdapter.existsByCode("NON_EXISTENT"));
    }

    @Test
    @DisplayName("Should test existsByCodeAndIdNot correctly")
    void shouldTestExistsByCodeAndIdNot() {
        UUID deptId1 = UUID.randomUUID();
        Department dept1 = Department.builder()
            .id(deptId1)
            .code("IT")
            .name("Information Technology")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        departmentRepositoryAdapter.save(dept1);

        // IT exists, but for deptId1 itself it should return false
        assertFalse(
            departmentRepositoryAdapter.existsByCodeAndIdNot("IT", deptId1)
        );

        // For another department ID, IT should return true
        UUID deptId2 = UUID.randomUUID();
        assertTrue(
            departmentRepositoryAdapter.existsByCodeAndIdNot("IT", deptId2)
        );
    }

    @Test
    @DisplayName("Should find all departments")
    void shouldFindAllDepartments() {
        Department d1 = Department.builder()
            .id(UUID.randomUUID())
            .code("FIN")
            .name("Finance")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        Department d2 = Department.builder()
            .id(UUID.randomUUID())
            .code("LEGAL")
            .name("Legal")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        departmentRepositoryAdapter.save(d1);
        departmentRepositoryAdapter.save(d2);

        List<Department> list = departmentRepositoryAdapter.findAll();
        assertTrue(list.size() >= 2);
    }
}
