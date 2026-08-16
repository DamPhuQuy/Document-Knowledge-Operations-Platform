package com.platform.app.ticket.application.dto;

import com.platform.app.ticket.domain.model.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketStatusCommand {

    @NotNull(message = "Target status is required")
    @Schema(example = "IN_PROGRESS")
    private TicketStatus status;

    @Schema(example = "Customer provided log files, proceeding with investigation")
    private String reason;

    @Schema(example = "Resolved by configuring proper API webhook endpoint")
    private String resolutionNotes;
}
