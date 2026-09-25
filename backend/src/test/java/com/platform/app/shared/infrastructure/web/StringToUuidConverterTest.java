package com.platform.app.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class StringToUuidConverterTest {

  private final StringToUuidConverter converter = new StringToUuidConverter();

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "null", "undefined"})
  @DisplayName("Should convert empty, blank, or placeholder strings to null")
  void shouldConvertEmptyStringToNull(String input) {
    UUID result = converter.convert(input);
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Should convert valid UUID string to UUID instance")
  void shouldConvertValidUuidString() {
    String uuidStr = "11111111-1111-1111-1111-111111111111";
    UUID result = converter.convert(uuidStr);
    assertThat(result).isEqualTo(UUID.fromString(uuidStr));
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when converting invalid string")
  void shouldThrowOnInvalidUuidString() {
    assertThatThrownBy(() -> converter.convert("dept-invalid-123"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
