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
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {

    private Long id;
    private Long customerId;
    private String customerName;
    private Long assignedAgentId;
    private String title;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private TicketCategory category;
    private String resolutionNotes;
    private Instant resolvedAt;
    private Instant closedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private List<TicketMessageResponse> messages;

    public static TicketResponse from(Ticket ticket) {
        List<TicketMessageResponse> messageResponses = ticket.getMessages() != null
                ? ticket.getMessages().stream().map(TicketMessageResponse::from).toList()
                : List.of();

        return TicketResponse.builder()
                .id(ticket.getId())
                .customerId(ticket.getCustomerId())
                .customerName(ticket.getCustomerName())
                .assignedAgentId(ticket.getAssignedAgentId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .category(ticket.getCategory())
                .resolutionNotes(ticket.getResolutionNotes())
                .resolvedAt(ticket.getResolvedAt())
                .closedAt(ticket.getClosedAt())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .messages(messageResponses)
                .build();
    }
}
