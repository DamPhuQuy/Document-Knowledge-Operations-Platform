package com.platform.app.ticket.domain;

import com.platform.app.shared.exception.DomainRuleViolationException;
import com.platform.app.ticket.domain.model.Ticket;
import com.platform.app.ticket.domain.model.TicketCategory;
import com.platform.app.ticket.domain.model.TicketMessage;
import com.platform.app.ticket.domain.model.TicketPriority;
import com.platform.app.ticket.domain.model.TicketStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TicketAggregateTest {

    @Test
    void shouldCreateTicketInOpenState() {
        Ticket ticket = Ticket.create(
                1L,
                "Alice",
                "Cannot login to dashboard",
                "Getting 403 Forbidden after OAuth callback",
                TicketPriority.HIGH,
                TicketCategory.TECHNICAL
        );

        assertNotNull(ticket);
        assertEquals(TicketStatus.OPEN, ticket.getStatus());
        assertEquals(TicketPriority.HIGH, ticket.getPriority());
        assertEquals(TicketCategory.TECHNICAL, ticket.getCategory());
        assertEquals(1L, ticket.getCustomerId());
        assertEquals("Alice", ticket.getCustomerName());
        assertFalse(ticket.domainEvents().isEmpty());
    }

    @Test
    void shouldAssignAgentAndTransitionToAssigned() {
        Ticket ticket = Ticket.create(
                1L,
                "Alice",
                "Billing inquiry",
                "Charged twice for subscription",
                TicketPriority.MEDIUM,
                TicketCategory.BILLING
        );

        ticket.assignTo(10L);

        assertEquals(10L, ticket.getAssignedAgentId());
        assertEquals(TicketStatus.ASSIGNED, ticket.getStatus());
    }

    @Test
    void shouldEnforceValidStateTransitions() {
        Ticket ticket = Ticket.create(
                1L,
                "Alice",
                "Bug report",
                "UI glitch in settings",
                TicketPriority.LOW,
                TicketCategory.BUG_REPORT
        );

        ticket.changeStatus(TicketStatus.IN_PROGRESS, "Started work");
        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());

        ticket.resolve("Fixed CSS overflow issue");
        assertEquals(TicketStatus.RESOLVED, ticket.getStatus());
        assertEquals("Fixed CSS overflow issue", ticket.getResolutionNotes());
        assertNotNull(ticket.getResolvedAt());

        ticket.close();
        assertEquals(TicketStatus.CLOSED, ticket.getStatus());
        assertNotNull(ticket.getClosedAt());
    }

    @Test
    void shouldRejectInvalidStateTransition() {
        Ticket ticket = Ticket.create(
                1L,
                "Alice",
                "General question",
                "How to export data?",
                TicketPriority.LOW,
                TicketCategory.GENERAL_INQUIRY
        );

        // Direct OPEN -> RESOLVED is invalid according to state machine rules
        assertThrows(DomainRuleViolationException.class, () ->
                ticket.changeStatus(TicketStatus.RESOLVED, "Invalid direct jump")
        );
    }

    @Test
    void shouldAddMessagesToTicket() {
        Ticket ticket = Ticket.create(
                1L,
                "Alice",
                "Feature request",
                "Add dark mode support",
                TicketPriority.LOW,
                TicketCategory.FEATURE_REQUEST
        );

        TicketMessage msg = ticket.addMessage(
                1L,
                "Alice",
                "CUSTOMER",
                "Any update on this?",
                false
        );

        assertNotNull(msg);
        assertEquals(1, ticket.getMessages().size());
        assertEquals("Any update on this?", ticket.getMessages().get(0).getContent());
    }

    @Test
    void shouldRejectAddingMessageToClosedTicket() {
        Ticket ticket = Ticket.create(
                1L,
                "Alice",
                "Issue",
                "Description",
                TicketPriority.LOW,
                TicketCategory.GENERAL_INQUIRY
        );
        ticket.close();

        assertThrows(DomainRuleViolationException.class, () ->
                ticket.addMessage(1L, "Alice", "CUSTOMER", "Hello?", false)
        );
    }
}
