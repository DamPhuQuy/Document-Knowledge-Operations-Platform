package com.platform.app.ai.infrastructure.config;

import com.platform.app.shared.config.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class AiClientConfig {

    private final AppProperties appProperties;

    @Bean
    public RestClient aiRestClient() {
        AppProperties.Ai aiConfig = appProperties.getAi();
        String url = aiConfig.getLlm().getUrl();
        
        // Parse timeout configuration
        String timeoutStr = aiConfig.getLlm().getTimeout();
        int timeoutMs = 10000; // Default 10 seconds
        try {
            if (timeoutStr.endsWith("s")) {
                timeoutMs = Integer.parseInt(timeoutStr.substring(0, timeoutStr.length() - 1)) * 1000;
            } else if (timeoutStr.endsWith("ms")) {
                timeoutMs = Integer.parseInt(timeoutStr.substring(0, timeoutStr.length() - 2));
            } else {
                timeoutMs = Integer.parseInt(timeoutStr);
            }
        } catch (Exception e) {
            // keep default
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);

        return RestClient.builder()
                .baseUrl(url)
                .requestFactory(requestFactory)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
