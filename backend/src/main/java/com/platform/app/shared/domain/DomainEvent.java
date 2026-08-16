package com.platform.app.shared.domain;

import java.time.Instant;

public interface DomainEvent {

    Instant occurredAt();
}
