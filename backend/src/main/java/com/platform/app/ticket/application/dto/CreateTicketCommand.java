package com.platform.app.ticket.application.dto;

import com.platform.app.ticket.domain.model.TicketCategory;
import com.platform.app.ticket.domain.model.TicketPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketCommand {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    @Schema(example = "Unable to connect to payment gateway")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 5000, message = "Description must be between 10 and 5000 characters")
    @Schema(example = "When attempting to checkout with credit card, the error code ERR_502 appears consistently.")
    private String description;

    @Schema(example = "HIGH", defaultValue = "MEDIUM")
    private TicketPriority priority;

    @Schema(example = "BILLING", defaultValue = "GENERAL_INQUIRY")
    private TicketCategory category;
}
