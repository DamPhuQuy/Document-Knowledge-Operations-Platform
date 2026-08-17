package com.platform.app.ai.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfig {

    @Bean
    public OpenAIClient openAIClient(LlmProperties properties) {
        String apiKey = properties.apiKey() != null && !properties.apiKey().isBlank()
            ? properties.apiKey()
            : "test-api-key";

        Duration timeout = properties.timeout() != null ? properties.timeout() : Duration.ofSeconds(10);

        return OpenAIOkHttpClient.builder()
            .apiKey(apiKey)
            .baseUrl(properties.baseUrl())
            .timeout(timeout)
            .maxRetries(properties.maxRetries())
            .build();
    }
}
