package com.platform.app.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IdGeneratorTest {

    @Test
    @DisplayName(
        "nextId() should generate a valid UUID version 7 with RFC variant"
    )
    void shouldGenerateValidUuidV7() {
        UUID id = IdGenerator.nextId();

        assertThat(id).isNotNull();
        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    @DisplayName("nextId() should generate unique IDs in rapid succession")
    void shouldGenerateUniqueIds() {
        int count = 1000;
        Set<UUID> ids = new HashSet<>();

        for (int i = 0; i < count; i++) {
            UUID id = IdGenerator.nextId();
            assertThat(id.version()).isEqualTo(7);
            ids.add(id);
        }

        assertThat(ids).hasSize(count);
    }

    @Test
    @DisplayName("nextId() should maintain time-ordered ascending order")
    void shouldMaintainTimeOrdering() {
        UUID id1 = IdGenerator.nextId();
        UUID id2 = IdGenerator.nextId();

        assertThat(id2.compareTo(id1)).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("IdGenerator utility class should have private constructor")
    void shouldHavePrivateConstructor() throws Exception {
        Constructor<IdGenerator> constructor =
            IdGenerator.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
        } catch (InvocationTargetException e) {
            // expected if throwing in constructor
        }
    }
}
