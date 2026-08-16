package com.platform.app.ticket.application.dto;

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
public class AddTicketMessageCommand {

    @NotBlank(message = "Message content is required")
    @Size(min = 1, max = 5000, message = "Message content must not exceed 5000 characters")
    @Schema(example = "We have deployed a patch and verified the webhook endpoint is responding normally.")
    private String content;

    @Schema(example = "false", defaultValue = "false", description = "Internal note visible only to support agents")
    @Builder.Default
    private boolean internalNote = false;
}
