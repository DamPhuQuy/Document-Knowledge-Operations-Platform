package com.platform.app.ai.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.llm")
public record LlmProperties(
    String apiKey,
    String baseUrl,
    String model,
    Double temperature,
    Duration timeout,
    int maxRetries,
    Duration retryDelay
) {
    public LlmProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.openai.com/v1";
        }
        if (model == null || model.isBlank()) {
            model = "gpt-4o-mini";
        }
        if (temperature == null) {
            temperature = 0.2;
        }
        if (timeout == null) {
            timeout = Duration.ofSeconds(10);
        }
        if (maxRetries <= 0) {
            maxRetries = 3;
        }
        if (retryDelay == null) {
            retryDelay = Duration.ofMillis(500);
        }
    }

    public Double defaultTemperature() {
        return temperature();
    }
}
