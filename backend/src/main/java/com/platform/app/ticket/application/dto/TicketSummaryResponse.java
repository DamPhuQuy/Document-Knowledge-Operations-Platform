package com.platform.app.ticket.application.dto;

import com.platform.app.ticket.domain.model.Ticket;
import com.platform.app.ticket.domain.model.TicketCategory;
import com.platform.app.ticket.domain.model.TicketPriority;
import com.platform.app.ticket.domain.model.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketSummaryResponse {

    private Long id;
    private Long customerId;
    private String customerName;
    private Long assignedAgentId;
    private String title;
    private TicketStatus status;
    private TicketPriority priority;
    private TicketCategory category;
    private int messageCount;
    private Instant createdAt;
    private Instant updatedAt;

    public static TicketSummaryResponse from(Ticket ticket) {
        return TicketSummaryResponse.builder()
                .id(ticket.getId())
                .customerId(ticket.getCustomerId())
                .customerName(ticket.getCustomerName())
                .assignedAgentId(ticket.getAssignedAgentId())
                .title(ticket.getTitle())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .category(ticket.getCategory())
                .messageCount(ticket.getMessages() != null ? ticket.getMessages().size() : 0)
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
