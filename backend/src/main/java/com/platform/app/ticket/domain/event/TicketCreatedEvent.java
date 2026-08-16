package com.platform.app.ticket.domain.event;

import com.platform.app.shared.domain.DomainEvent;
import com.platform.app.ticket.domain.model.TicketPriority;

import java.time.Instant;

public record TicketCreatedEvent(
        Long ticketId,
        Long customerId,
        String title,
        TicketPriority priority,
        Instant occurredAt
) implements DomainEvent {

    public TicketCreatedEvent(Long ticketId, Long customerId, String title, TicketPriority priority) {
        this(ticketId, customerId, title, priority, Instant.now());
    }
}
