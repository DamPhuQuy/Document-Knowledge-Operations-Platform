package com.platform.app.shared.domain;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@MappedSuperclass
public abstract class AggregateRoot extends BaseEntity {

    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    @DomainEvents
    public List<DomainEvent> domainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    @AfterDomainEventPublication
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}
