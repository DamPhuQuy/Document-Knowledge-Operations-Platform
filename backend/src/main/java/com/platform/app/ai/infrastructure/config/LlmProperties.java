package com.platform.app.ai.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.llm")
public record LlmProperties(
    String host,
    int port,
    Duration timeout
) {
    public LlmProperties {
        if (host == null || host.isBlank()) {
            host = "localhost";
        }
        if (port <= 0) {
            port = 50051;
        }
        if (timeout == null) {
            timeout = Duration.ofSeconds(10);
        }
    }
}
