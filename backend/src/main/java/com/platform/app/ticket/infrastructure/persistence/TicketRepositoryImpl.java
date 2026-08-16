package com.platform.app.ticket.infrastructure.persistence;

import com.platform.app.ticket.domain.model.Ticket;
import com.platform.app.ticket.domain.model.TicketStatus;
import com.platform.app.ticket.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TicketRepositoryImpl implements TicketRepository {

    private final SpringDataJpaTicketRepository jpaRepository;

    @Override
    public Ticket save(Ticket ticket) {
        return jpaRepository.save(ticket);
    }

    @Override
    public Optional<Ticket> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Page<Ticket> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }

    @Override
    public Page<Ticket> findByCustomerId(Long customerId, Pageable pageable) {
        return jpaRepository.findByCustomerId(customerId, pageable);
    }

    @Override
    public Page<Ticket> findByAssignedAgentId(Long agentId, Pageable pageable) {
        return jpaRepository.findByAssignedAgentId(agentId, pageable);
    }

    @Override
    public Page<Ticket> findByStatus(TicketStatus status, Pageable pageable) {
        return jpaRepository.findByStatus(status, pageable);
    }

    @Override
    public long countByStatus(TicketStatus status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
