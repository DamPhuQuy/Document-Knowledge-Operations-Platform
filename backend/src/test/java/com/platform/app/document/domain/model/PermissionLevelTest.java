package com.platform.app.document.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PermissionLevelTest {

  @Test
  @DisplayName("Should contain all 3 permission levels defined in UC-DOC-03")
  void shouldContainAllThreePermissionLevels() {
    assertEquals(3, PermissionLevel.values().length);
    assertNotNull(PermissionLevel.valueOf("VIEW"));
    assertNotNull(PermissionLevel.valueOf("EDIT"));
    assertNotNull(PermissionLevel.valueOf("ADMIN"));
  }
}
