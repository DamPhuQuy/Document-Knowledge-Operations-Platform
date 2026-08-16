package com.platform.app.ticket.domain.repository;

import com.platform.app.ticket.domain.model.Ticket;
import com.platform.app.ticket.domain.model.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface TicketRepository {

    Ticket save(Ticket ticket);

    Optional<Ticket> findById(Long id);

    Page<Ticket> findAll(Pageable pageable);

    Page<Ticket> findByCustomerId(Long customerId, Pageable pageable);

    Page<Ticket> findByAssignedAgentId(Long agentId, Pageable pageable);

    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    long countByStatus(TicketStatus status);

    void deleteById(Long id);
}
