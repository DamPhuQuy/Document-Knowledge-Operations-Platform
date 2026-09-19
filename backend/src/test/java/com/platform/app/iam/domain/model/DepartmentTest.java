package com.platform.app.iam.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.platform.app.iam.domain.exception.InvalidDepartmentCodeException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DepartmentTest {

    @Test
    @DisplayName("Should successfully construct valid department")
    void shouldConstructValidDepartment() {
        UUID id = UUID.randomUUID();
        Department dept = Department.builder()
            .id(id)
            .code("HR")
            .name("Human Resources")
            .description("Handles personnel and hiring")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        assertEquals(id, dept.getId());
        assertEquals("HR", dept.getCode());
        assertEquals("Human Resources", dept.getName());
        assertEquals("Handles personnel and hiring", dept.getDescription());
        assertNotNull(dept.getCreatedAt());
        assertNotNull(dept.getUpdatedAt());
    }

    @ParameterizedTest
    @ValueSource(strings = { "HR", "FIN", "IT_DEV", "LEGAL123", "ENGINEERING" })
    @DisplayName("Should accept valid uppercase alphanumeric department codes")
    void shouldAcceptValidCodes(String validCode) {
        Department dept = Department.builder()
            .id(UUID.randomUUID())
            .code(validCode)
            .name("Department " + validCode)
            .build();

        assertEquals(validCode, dept.getCode());
    }

    @ParameterizedTest
    @ValueSource(
        strings = { "hr", "Fin", "IT-DEV", "HR dept", "IT@SYS", "", "   " }
    )
    @DisplayName(
        "Should reject invalid department codes (lowercase, symbols, spaces, blank)"
    )
    void shouldRejectInvalidCodes(String invalidCode) {
        assertThrows(InvalidDepartmentCodeException.class, () ->
            Department.builder()
                .id(UUID.randomUUID())
                .code(invalidCode)
                .name("Valid Name")
                .build()
        );
    }

    @Test
    @DisplayName("Should reject null code or blank name")
    void shouldRejectNullCodeOrBlankName() {
        assertThrows(InvalidDepartmentCodeException.class, () ->
            Department.builder()
                .id(UUID.randomUUID())
                .code(null)
                .name("Valid Name")
                .build()
        );

        assertThrows(IllegalArgumentException.class, () ->
            Department.builder()
                .id(UUID.randomUUID())
                .code("HR")
                .name("  ")
                .build()
        );
    }

    @Test
    @DisplayName("Should successfully update department details")
    void shouldUpdateDepartmentDetails() {
        Department dept = Department.builder()
            .id(UUID.randomUUID())
            .code("HR")
            .name("Human Resources")
            .description("Old desc")
            .build();

        dept.updateDetails("PEOPLE", "People & Culture", "New desc");

        assertEquals("PEOPLE", dept.getCode());
        assertEquals("People & Culture", dept.getName());
        assertEquals("New desc", dept.getDescription());
    }
}
