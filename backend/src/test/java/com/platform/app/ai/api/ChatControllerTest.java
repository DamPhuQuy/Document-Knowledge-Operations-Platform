package com.platform.app.ai.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.app.ai.application.dto.request.ChatRequest;
import com.platform.app.ai.application.port.out.LlmClientPort;
import com.platform.app.ai.domain.exception.LlmProviderException;
import com.platform.app.ai.domain.exception.LlmSchemaValidationException;
import com.platform.app.ai.domain.exception.LlmTimeoutException;
import com.platform.app.ai.domain.model.AssistantResponse;
import com.platform.app.ai.domain.model.Confidence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ChatControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LlmClientPort llmClientPort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/ai/chat - 200 OK with structured response")
    void shouldReturnOkWithStructuredResponse() throws Exception {
        AssistantResponse mockAssistantResponse = AssistantResponse.builder()
            .answer("You can track your order in the Orders tab.")
            .confidence(Confidence.HIGH)
            .promptTokens(30)
            .completionTokens(15)
            .build();

        when(llmClientPort.generateResponse(any(), any())).thenReturn(mockAssistantResponse);

        ChatRequest request = ChatRequest.builder()
            .message("Where can I see my orders?")
            .temperature(0.3)
            .build();

        mockMvc.perform(post("/api/v1/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Message processed successfully"))
            .andExpect(jsonPath("$.data.answer").value("You can track your order in the Orders tab."))
            .andExpect(jsonPath("$.data.confidence").value("HIGH"))
            .andExpect(jsonPath("$.data.promptTokens").value(30))
            .andExpect(jsonPath("$.data.completionTokens").value(15));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/ai/chat - 400 Bad Request when message is blank")
    void shouldReturnBadRequestWhenMessageIsBlank() throws Exception {
        ChatRequest request = ChatRequest.builder()
            .message("   ")
            .build();

        mockMvc.perform(post("/api/v1/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/ai/chat - 400 Bad Request when temperature is out of range")
    void shouldReturnBadRequestWhenTemperatureInvalid() throws Exception {
        ChatRequest request = ChatRequest.builder()
            .message("Valid message")
            .temperature(2.5) // Max is 2.0
            .build();

        mockMvc.perform(post("/api/v1/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/ai/chat - 504 Gateway Timeout when LLM times out")
    void shouldReturnGatewayTimeoutWhenLlmTimesOut() throws Exception {
        when(llmClientPort.generateResponse(any(), any()))
            .thenThrow(new LlmTimeoutException("LLM request timed out after 10s"));

        ChatRequest request = ChatRequest.builder()
            .message("Help me please")
            .build();

        mockMvc.perform(post("/api/v1/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isGatewayTimeout())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("AI service timed out: LLM request timed out after 10s"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/ai/chat - 502 Bad Gateway when LLM provider returns 5xx")
    void shouldReturnBadGatewayWhenProviderFails() throws Exception {
        when(llmClientPort.generateResponse(any(), any()))
            .thenThrow(new LlmProviderException("500 Internal Server Error from upstream provider"));

        ChatRequest request = ChatRequest.builder()
            .message("Help me please")
            .build();

        mockMvc.perform(post("/api/v1/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("AI provider error: 500 Internal Server Error from upstream provider"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/ai/chat - 422 Unprocessable Entity when response schema is invalid")
    void shouldReturnUnprocessableEntityWhenSchemaInvalid() throws Exception {
        when(llmClientPort.generateResponse(any(), any()))
            .thenThrow(new LlmSchemaValidationException("Missing required 'answer' field"));

        ChatRequest request = ChatRequest.builder()
            .message("Help me please")
            .build();

        mockMvc.perform(post("/api/v1/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Failed to parse AI structured response: Missing required 'answer' field"));
    }

    @Test
    @DisplayName("POST /api/v1/ai/chat - 401 Unauthorized when not authenticated")
    void shouldReturnUnauthorizedWhenAnonymous() throws Exception {
        ChatRequest request = ChatRequest.builder()
            .message("Help me please")
            .build();

        mockMvc.perform(post("/api/v1/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }
}
