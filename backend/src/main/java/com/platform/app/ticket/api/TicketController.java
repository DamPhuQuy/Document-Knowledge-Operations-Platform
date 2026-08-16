package com.platform.app.ticket.api;

import com.platform.app.shared.dto.ApiResponse;
import com.platform.app.shared.dto.PageResponse;
import com.platform.app.ticket.application.dto.AddTicketMessageCommand;
import com.platform.app.ticket.application.dto.AssignTicketCommand;
import com.platform.app.ticket.application.dto.CreateTicketCommand;
import com.platform.app.ticket.application.dto.TicketMessageResponse;
import com.platform.app.ticket.application.dto.TicketResponse;
import com.platform.app.ticket.application.dto.TicketSummaryResponse;
import com.platform.app.ticket.application.dto.UpdateTicketStatusCommand;
import com.platform.app.ticket.application.service.TicketApplicationService;
import com.platform.app.ticket.domain.model.TicketStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Ticket Management (Tactical DDD Archetype)", description = "Inbound REST Adapter for ticket management use cases")
public class TicketController {

    private final TicketApplicationService ticketApplicationService;

    @PostMapping
    @Operation(summary = "Create Support Ticket", description = "Inbound command triggering ticket aggregate creation")
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(@Valid @RequestBody CreateTicketCommand command) {
        TicketResponse response = ticketApplicationService.createTicket(command);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("Support ticket created successfully", response));
    }

    @GetMapping
    @Operation(summary = "List Tickets", description = "Query use case for listing paginated tickets")
    public ResponseEntity<ApiResponse<PageResponse<TicketSummaryResponse>>> getTickets(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) TicketStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        var page = ticketApplicationService.getTickets(customerId, agentId, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Tickets retrieved successfully", PageResponse.from(page)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Ticket Details", description = "Query use case for ticket aggregate details with conversation thread")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicketById(@PathVariable Long id) {
        TicketResponse response = ticketApplicationService.getTicketById(id);
        return ResponseEntity.ok(ApiResponse.ok("Ticket details retrieved", response));
    }

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Assign Agent", description = "Inbound command to assign an agent to the ticket aggregate")
    public ResponseEntity<ApiResponse<TicketResponse>> assignTicket(
            @PathVariable Long id,
            @Valid @RequestBody AssignTicketCommand command
    ) {
        TicketResponse response = ticketApplicationService.assignTicket(id, command);
        return ResponseEntity.ok(ApiResponse.ok("Ticket assigned successfully", response));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update Ticket Status", description = "Inbound command to transition aggregate state")
    public ResponseEntity<ApiResponse<TicketResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketStatusCommand command
    ) {
        TicketResponse response = ticketApplicationService.updateStatus(id, command);
        return ResponseEntity.ok(ApiResponse.ok("Ticket status updated successfully", response));
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Add Ticket Message", description = "Inbound command to add message entity into ticket aggregate")
    public ResponseEntity<ApiResponse<TicketMessageResponse>> addMessage(
            @PathVariable Long id,
            @Valid @RequestBody AddTicketMessageCommand command
    ) {
        TicketMessageResponse response = ticketApplicationService.addMessage(id, command);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("Message added to ticket conversation", response));
    }
}
