package com.platform.app.ticket.domain.model;

import com.platform.app.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "ticket_messages")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TicketMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Column(name = "sender_role", nullable = false)
    private String senderRole;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_internal_note", nullable = false)
    @Builder.Default
    private boolean internalNote = false;

    void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }
}
