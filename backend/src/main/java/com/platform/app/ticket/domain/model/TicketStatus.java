package com.platform.app.ticket.domain.model;

import java.util.Set;

public enum TicketStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    PENDING_CUSTOMER,
    RESOLVED,
    CLOSED;

    public boolean canTransitionTo(TicketStatus target) {
        if (this == target) {
            return true;
        }

        return switch (this) {
            case OPEN -> Set.of(ASSIGNED, IN_PROGRESS, CLOSED).contains(target);
            case ASSIGNED -> Set.of(IN_PROGRESS, PENDING_CUSTOMER, RESOLVED, CLOSED, OPEN).contains(target);
            case IN_PROGRESS -> Set.of(PENDING_CUSTOMER, RESOLVED, CLOSED, ASSIGNED).contains(target);
            case PENDING_CUSTOMER -> Set.of(IN_PROGRESS, RESOLVED, CLOSED).contains(target);
            case RESOLVED -> Set.of(CLOSED, OPEN).contains(target);
            case CLOSED -> Set.of(OPEN).contains(target);
        };
    }
}
