package com.platform.app.iam.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PermissionTest {

    @Test
    @DisplayName("Should construct Permission with uppercase code and required fields")
    void shouldConstructPermissionWithUppercaseCode() {
        UUID id = UUID.randomUUID();
        Permission permission = Permission.builder()
            .id(id)
            .code("doc:read")
            .name("Read Document")
            .module("DOCUMENT")
            .description("Allows reading documents")
            .build();

        assertThat(permission.getId()).isEqualTo(id);
        assertThat(permission.getCode()).isEqualTo("DOC:READ");
        assertThat(permission.getName()).isEqualTo("Read Document");
        assertThat(permission.getModule()).isEqualTo("DOCUMENT");
        assertThat(permission.getDescription()).isEqualTo("Allows reading documents");
    }

    @Test
    @DisplayName("Should verify equals, hashCode, and toString based on included fields")
    void shouldVerifyEqualsHashCodeAndToString() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        Permission p1 = Permission.builder()
            .id(id1)
            .code("READ")
            .name("Read")
            .module("SYS")
            .build();

        Permission p2 = Permission.builder()
            .id(id2)
            .code("READ")
            .name("Read")
            .module("SYS")
            .build();

        assertThat(p1).isEqualTo(p2);
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
        assertThat(p1.toString()).contains("READ");
    }

    @Test
    @DisplayName("Should throw NullPointerException when required fields are null")
    void shouldThrowWhenRequiredFieldsNull() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> Permission.builder().id(null).code("READ").name("Read").build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Permission id must not be null");

        assertThatThrownBy(() -> Permission.builder().id(id).code(null).name("Read").build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Permission code must not be null");

        assertThatThrownBy(() -> Permission.builder().id(id).code("READ").name(null).build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Permission name must not be null");
    }
}
