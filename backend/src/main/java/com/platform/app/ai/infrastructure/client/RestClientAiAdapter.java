package com.platform.app.ai.infrastructure.client;

import com.platform.app.ai.domain.model.AiResponse;
import com.platform.app.ai.domain.model.LlmMessage;
import com.platform.app.ai.domain.port.AiClientPort;
import com.platform.app.shared.config.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestClientAiAdapter implements AiClientPort {

    private final RestClient aiRestClient;
    private final AppProperties appProperties;

    @Override
    public AiResponse generateResponse(List<LlmMessage> messages, double temperature) {
        log.debug("Sending REST API chat generation request to AI service with {} messages", messages.size());

        ChatRequestPayload payload = new ChatRequestPayload(messages, temperature);
        String path = appProperties.getAi().getLlm().getGeneratePath();

        try {
            return aiRestClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(AiResponse.class);
        } catch (Exception e) {
            log.error("Failed to generate response from AI service REST API", e);
            throw new RuntimeException("AI service communication failure: " + e.getMessage(), e);
        }
    }

    private record ChatRequestPayload(
            List<LlmMessage> messages,
            double temperature
    ) {}
}
