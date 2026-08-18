package com.platform.app.ai.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LlmMessage(
    @NotNull(message = "Role must not be null")
    LlmRole role,

    @NotBlank(message = "Content must not be blank")
    String content
) {}
