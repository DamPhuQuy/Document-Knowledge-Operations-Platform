package com.platform.app.document.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccessLevelTest {

    @Test
    @DisplayName(
        "Should contain all 4 security classification tiers defined in UC-DOC-03"
    )
    void shouldContainAllFourAccessLevels() {
        assertEquals(4, AccessLevel.values().length);
        assertNotNull(AccessLevel.valueOf("PUBLIC"));
        assertNotNull(AccessLevel.valueOf("INTERNAL"));
        assertNotNull(AccessLevel.valueOf("RESTRICTED"));
        assertNotNull(AccessLevel.valueOf("CONFIDENTIAL"));
    }
}
