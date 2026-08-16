package com.platform.app.ticket.domain.event;

import com.platform.app.shared.domain.DomainEvent;
import com.platform.app.ticket.domain.model.TicketStatus;

import java.time.Instant;

public record TicketStatusChangedEvent(
        Long ticketId,
        TicketStatus previousStatus,
        TicketStatus newStatus,
        String reason,
        Instant occurredAt
) implements DomainEvent {

    public TicketStatusChangedEvent(Long ticketId, TicketStatus previousStatus, TicketStatus newStatus, String reason) {
        this(ticketId, previousStatus, newStatus, reason, Instant.now());
    }
}
