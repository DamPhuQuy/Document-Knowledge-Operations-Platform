package com.platform.app.ticket.infrastructure.persistence;

import com.platform.app.ticket.domain.model.Ticket;
import com.platform.app.ticket.domain.model.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataJpaTicketRepository extends JpaRepository<Ticket, Long> {

    Page<Ticket> findByCustomerId(Long customerId, Pageable pageable);

    Page<Ticket> findByAssignedAgentId(Long agentId, Pageable pageable);

    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    long countByStatus(TicketStatus status);
}
