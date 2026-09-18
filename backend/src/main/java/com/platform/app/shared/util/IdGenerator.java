package com.platform.app.shared.util;

import java.util.UUID;

import com.github.f4b6a3.uuid.UuidCreator;

public final class IdGenerator {

  private IdGenerator() {}

  /**
   * @return a new time-ordered UUIDv7 instance
   */
  public static UUID nextId() {
    return UuidCreator.getTimeOrderedEpoch();
  }
}
