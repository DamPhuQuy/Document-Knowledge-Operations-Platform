package com.platform.app.shared.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class UuidUtilsTest {

  private static final String VALID_UUID_STR = "11111111-1111-1111-1111-111111111111";

  @Test
  @DisplayName("isValid returns true for valid UUID string")
  void isValidReturnsTrueForValidUuid() {
    assertThat(UuidUtils.isValid(VALID_UUID_STR)).isTrue();
    assertThat(UuidUtils.isValid(UUID.randomUUID().toString())).isTrue();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "not-a-uuid", "dept-1790316739247", "12345"})
  @DisplayName("isValid returns false for null, blank, or malformed strings")
  void isValidReturnsFalseForInvalidStrings(String input) {
    assertThat(UuidUtils.isValid(input)).isFalse();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "null", "NULL", "undefined", "UNDEFINED"})
  @DisplayName("parseOrNull returns null for null, blank, or placeholder strings")
  void parseOrNullReturnsNullForEmptyOrPlaceholders(String input) {
    assertThat(UuidUtils.parseOrNull(input)).isNull();
  }

  @Test
  @DisplayName("parseOrNull correctly parses valid UUID")
  void parseOrNullParsesValidUuid() {
    UUID result = UuidUtils.parseOrNull(VALID_UUID_STR);
    assertThat(result).isNotNull();
    assertThat(result).isEqualTo(UUID.fromString(VALID_UUID_STR));
  }

  @Test
  @DisplayName("parseOrNull throws IllegalArgumentException for malformed non-blank string")
  void parseOrNullThrowsForMalformedString() {
    assertThatThrownBy(() -> UuidUtils.parseOrNull("dept-1790316739247"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("tryParse returns Optional with UUID for valid input")
  void tryParseReturnsOptionalForValidUuid() {
    Optional<UUID> opt = UuidUtils.tryParse(VALID_UUID_STR);
    assertThat(opt).isPresent();
    assertThat(opt.get()).isEqualTo(UUID.fromString(VALID_UUID_STR));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "null", "undefined", "invalid-uuid"})
  @DisplayName("tryParse returns empty Optional for invalid or empty strings")
  void tryParseReturnsEmptyForInvalidOrEmptyStrings(String input) {
    assertThat(UuidUtils.tryParse(input)).isEmpty();
  }
}
