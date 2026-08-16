package com.platform.app.ticket.domain.event;

import com.platform.app.shared.domain.DomainEvent;

import java.time.Instant;

public record TicketAssignedEvent(
        Long ticketId,
        Long agentId,
        Instant occurredAt
) implements DomainEvent {

    public TicketAssignedEvent(Long ticketId, Long agentId) {
        this(ticketId, agentId, Instant.now());
    }
}
