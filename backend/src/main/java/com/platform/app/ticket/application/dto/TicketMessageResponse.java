package com.platform.app.ticket.application.dto;

import com.platform.app.ticket.domain.model.TicketMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketMessageResponse {

    private Long id;
    private Long senderId;
    private String senderName;
    private String senderRole;
    private String content;
    private boolean internalNote;
    private Instant createdAt;

    public static TicketMessageResponse from(TicketMessage message) {
        return TicketMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .senderName(message.getSenderName())
                .senderRole(message.getSenderRole())
                .content(message.getContent())
                .internalNote(message.isInternalNote())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
