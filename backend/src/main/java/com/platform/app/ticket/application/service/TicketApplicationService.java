package com.platform.app.ticket.application.service;

import com.platform.app.shared.exception.ForbiddenException;
import com.platform.app.shared.exception.ResourceNotFoundException;
import com.platform.app.shared.exception.UnauthorizedException;
import com.platform.app.shared.security.UserPrincipal;
import com.platform.app.ticket.application.dto.AddTicketMessageCommand;
import com.platform.app.ticket.application.dto.AssignTicketCommand;
import com.platform.app.ticket.application.dto.CreateTicketCommand;
import com.platform.app.ticket.application.dto.TicketMessageResponse;
import com.platform.app.ticket.application.dto.TicketResponse;
import com.platform.app.ticket.application.dto.TicketSummaryResponse;
import com.platform.app.ticket.application.dto.UpdateTicketStatusCommand;
import com.platform.app.ticket.domain.model.Ticket;
import com.platform.app.ticket.domain.model.TicketMessage;
import com.platform.app.ticket.domain.model.TicketStatus;
import com.platform.app.ticket.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketApplicationService {

    private final TicketRepository ticketRepository;

    @Transactional
    public TicketResponse createTicket(CreateTicketCommand command) {
        UserPrincipal currentUser = getCurrentAuthenticatedUser();

        Ticket ticket = Ticket.create(
                currentUser.getId(),
                currentUser.getFullName(),
                command.getTitle(),
                command.getDescription(),
                command.getPriority(),
                command.getCategory()
        );

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket created with ID: {} by customer: {}", savedTicket.getId(), currentUser.getEmail());
        return TicketResponse.from(savedTicket);
    }

    @Transactional
    public TicketResponse assignTicket(Long ticketId, AssignTicketCommand command) {
        UserPrincipal currentUser = getCurrentAuthenticatedUser();
        verifyAgentOrAdmin(currentUser);

        Ticket ticket = findTicketOrThrow(ticketId);
        ticket.assignTo(command.getAgentId());

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket ID: {} assigned to agent ID: {}", ticketId, command.getAgentId());
        return TicketResponse.from(savedTicket);
    }

    @Transactional
    public TicketResponse updateStatus(Long ticketId, UpdateTicketStatusCommand command) {
        UserPrincipal currentUser = getCurrentAuthenticatedUser();
        Ticket ticket = findTicketOrThrow(ticketId);

        // Check permissions: Customers can only close/reopen their own tickets
        if ("CUSTOMER".equals(currentUser.getRole()) && !ticket.getCustomerId().equals(currentUser.getId())) {
            throw new ForbiddenException("Customers can only manage their own tickets");
        }

        if (command.getStatus() == TicketStatus.RESOLVED) {
            ticket.resolve(command.getResolutionNotes() != null ? command.getResolutionNotes() : "Issue resolved");
        } else if (command.getStatus() == TicketStatus.CLOSED) {
            ticket.close();
        } else if (command.getStatus() == TicketStatus.OPEN && (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED)) {
            ticket.reopen();
        } else {
            ticket.changeStatus(command.getStatus(), command.getReason() != null ? command.getReason() : "Status updated by " + currentUser.getEmail());
        }

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket ID: {} status changed to: {}", ticketId, command.getStatus());
        return TicketResponse.from(savedTicket);
    }

    @Transactional
    public TicketMessageResponse addMessage(Long ticketId, AddTicketMessageCommand command) {
        UserPrincipal currentUser = getCurrentAuthenticatedUser();
        Ticket ticket = findTicketOrThrow(ticketId);

        // Customers can only comment on their own tickets and cannot post internal notes
        boolean isCustomer = "CUSTOMER".equals(currentUser.getRole());
        if (isCustomer && !ticket.getCustomerId().equals(currentUser.getId())) {
            throw new ForbiddenException("Cannot add message to tickets owned by other customers");
        }

        boolean internalNote = !isCustomer && command.isInternalNote();

        TicketMessage message = ticket.addMessage(
                currentUser.getId(),
                currentUser.getFullName(),
                currentUser.getRole(),
                command.getContent(),
                internalNote
        );

        ticketRepository.save(ticket);
        log.info("Added message to ticket ID: {} by user: {}", ticketId, currentUser.getEmail());
        return TicketMessageResponse.from(message);
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long ticketId) {
        UserPrincipal currentUser = getCurrentAuthenticatedUser();
        Ticket ticket = findTicketOrThrow(ticketId);

        if ("CUSTOMER".equals(currentUser.getRole()) && !ticket.getCustomerId().equals(currentUser.getId())) {
            throw new ForbiddenException("Access denied to view this ticket");
        }

        return TicketResponse.from(ticket);
    }

    @Transactional(readOnly = true)
    public Page<TicketSummaryResponse> getTickets(Long customerId, Long agentId, TicketStatus status, Pageable pageable) {
        UserPrincipal currentUser = getCurrentAuthenticatedUser();

        // Enforce customer isolation
        if ("CUSTOMER".equals(currentUser.getRole())) {
            return ticketRepository.findByCustomerId(currentUser.getId(), pageable)
                    .map(TicketSummaryResponse::from);
        }

        if (customerId != null) {
            return ticketRepository.findByCustomerId(customerId, pageable).map(TicketSummaryResponse::from);
        }

        if (agentId != null) {
            return ticketRepository.findByAssignedAgentId(agentId, pageable).map(TicketSummaryResponse::from);
        }

        if (status != null) {
            return ticketRepository.findByStatus(status, pageable).map(TicketSummaryResponse::from);
        }

        return ticketRepository.findAll(pageable).map(TicketSummaryResponse::from);
    }

    private Ticket findTicketOrThrow(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));
    }

    private UserPrincipal getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return principal;
    }

    private void verifyAgentOrAdmin(UserPrincipal user) {
        if (!"AGENT".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            throw new ForbiddenException("Only agents and admins can perform this action");
        }
    }
}
