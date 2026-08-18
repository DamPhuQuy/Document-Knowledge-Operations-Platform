package com.platform.app.ai.domain.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum LlmRole {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant");

    private final String value;

    LlmRole(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
