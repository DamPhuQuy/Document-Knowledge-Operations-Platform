package com.platform.app.ticket.domain.model;

import com.platform.app.shared.domain.AggregateRoot;
import com.platform.app.shared.exception.DomainRuleViolationException;
import com.platform.app.ticket.domain.event.TicketAssignedEvent;
import com.platform.app.ticket.domain.event.TicketCreatedEvent;
import com.platform.app.ticket.domain.event.TicketStatusChangedEvent;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Entity
@Table(name = "tickets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket extends AggregateRoot {

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "assigned_agent_id")
    private Long assignedAgentId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private TicketCategory category;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<TicketMessage> messages = new ArrayList<>();

    // Domain Factory Method
    public static Ticket create(
            Long customerId,
            String customerName,
            String title,
            String description,
            TicketPriority priority,
            TicketCategory category
    ) {
        Assert.notNull(customerId, "Customer ID must not be null");
        Assert.hasText(customerName, "Customer name must not be empty");
        Assert.hasText(title, "Ticket title must not be empty");
        Assert.hasText(description, "Ticket description must not be empty");

        Ticket ticket = new Ticket();
        ticket.customerId = customerId;
        ticket.customerName = customerName.trim();
        ticket.title = title.trim();
        ticket.description = description.trim();
        ticket.priority = priority != null ? priority : TicketPriority.MEDIUM;
        ticket.category = category != null ? category : TicketCategory.GENERAL_INQUIRY;
        ticket.status = TicketStatus.OPEN;

        // Register domain event
        ticket.registerEvent(new TicketCreatedEvent(ticket.getId(), customerId, ticket.title, ticket.priority));
        return ticket;
    }

    // Business Method: Assign Agent
    public void assignTo(Long agentId) {
        Assert.notNull(agentId, "Agent ID must not be null");

        if (this.status == TicketStatus.CLOSED) {
            throw new DomainRuleViolationException("Cannot assign an agent to a closed ticket");
        }

        this.assignedAgentId = agentId;
        if (this.status == TicketStatus.OPEN) {
            changeStatus(TicketStatus.ASSIGNED, "Assigned to agent ID: " + agentId);
        }

        registerEvent(new TicketAssignedEvent(this.getId(), agentId));
    }

    // Business Method: Change Status
    public void changeStatus(TicketStatus newStatus, String reason) {
        Assert.notNull(newStatus, "Target status must not be null");

        if (!this.status.canTransitionTo(newStatus)) {
            throw new DomainRuleViolationException(
                    String.format("Invalid status transition from %s to %s", this.status, newStatus)
            );
        }

        TicketStatus previous = this.status;
        this.status = newStatus;

        if (newStatus == TicketStatus.RESOLVED && this.resolvedAt == null) {
            this.resolvedAt = Instant.now();
        } else if (newStatus == TicketStatus.CLOSED && this.closedAt == null) {
            this.closedAt = Instant.now();
        } else if (newStatus == TicketStatus.OPEN) {
            this.resolvedAt = null;
            this.closedAt = null;
        }

        registerEvent(new TicketStatusChangedEvent(this.getId(), previous, newStatus, reason));
    }

    // Business Method: Resolve Ticket
    public void resolve(String resolutionNotes) {
        Assert.hasText(resolutionNotes, "Resolution notes are required when resolving a ticket");

        this.resolutionNotes = resolutionNotes.trim();
        changeStatus(TicketStatus.RESOLVED, "Ticket marked as resolved with notes");
    }

    // Business Method: Close Ticket
    public void close() {
        changeStatus(TicketStatus.CLOSED, "Ticket closed");
    }

    // Business Method: Reopen Ticket
    public void reopen() {
        if (this.status != TicketStatus.RESOLVED && this.status != TicketStatus.CLOSED) {
            throw new DomainRuleViolationException("Only resolved or closed tickets can be reopened");
        }
        changeStatus(TicketStatus.OPEN, "Ticket reopened by user/customer");
    }

    // Business Method: Add Conversation Message
    public TicketMessage addMessage(Long senderId, String senderName, String senderRole, String content, boolean internalNote) {
        Assert.notNull(senderId, "Sender ID must not be null");
        Assert.hasText(senderName, "Sender name must not be empty");
        Assert.hasText(content, "Message content must not be empty");

        if (this.status == TicketStatus.CLOSED) {
            throw new DomainRuleViolationException("Cannot add messages to a closed ticket");
        }

        TicketMessage message = TicketMessage.builder()
                .ticket(this)
                .senderId(senderId)
                .senderName(senderName.trim())
                .senderRole(senderRole)
                .content(content.trim())
                .internalNote(internalNote)
                .build();

        this.messages.add(message);

        // Auto transition status when customer or agent replies
        if ("CUSTOMER".equalsIgnoreCase(senderRole) && this.status == TicketStatus.PENDING_CUSTOMER) {
            changeStatus(TicketStatus.IN_PROGRESS, "Customer replied, resuming progress");
        }

        return message;
    }

    public List<TicketMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }
}
