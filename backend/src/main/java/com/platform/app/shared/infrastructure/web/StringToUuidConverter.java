package com.platform.app.shared.infrastructure.web;

import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.platform.app.shared.util.UuidUtils;

/**
 * Spring Web Converter converting String inputs (e.g. from {@code @RequestParam} or
 * {@code @PathVariable}) into {@link UUID}.
 * Treats blank strings (""), "null", and "undefined" as {@code null} instead of throwing
 * conversion errors on optional request parameters.
 */
@Component
public class StringToUuidConverter implements Converter<String, UUID> {

  @Override
  @Nullable
  public UUID convert(String source) {
    return UuidUtils.parseOrNull(source);
  }
}
