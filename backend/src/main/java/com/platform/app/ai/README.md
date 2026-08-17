### 1. Sơ đồ tổ chức thư mục com.platform.app.ai (Slice 1)

    com.platform.app.ai/
    ├── api/                                      # [Inbound Adapter] REST API
    │   ├── ChatController.java                  # POST /api/v1/ai/chat
    │   └── AiExceptionHandler.java              # Bắt lỗi LLM timeout, provider error -> ApiResponse chuẩn
    │
    ├── application/                              # [Application Layer] Use cases & DTOs
    │   ├── dto/
    │   │   ├── request/
    │   │   │   └── ChatRequest.java             # Request payload (@NotBlank message, model, temperature)
    │   │   └── response/
    │   │       └── ChatResponse.java            # DTO trả về cho client bọc trong ApiResponse<ChatResponse>
    │   ├── port/
    │   │   ├── in/
    │   │   │   └── ChatUseCase.java             # Inbound Port interface
    │   │   └── out/
    │   │       └── LlmClientPort.java           # Outbound Port (decouple core logic khỏi nhà mạng LLM)
    │   └── service/
    │       └── ChatService.java                 # Implements ChatUseCase, điều phối gọi LLM & validation
    │
    ├── domain/                                   # [Core Domain] Pure Java, zero framework coupling
    │   ├── model/
    │   │   ├── AssistantResponse.java           # Structured Output schema (answer, confidence, metadata)
    │   │   ├── Confidence.java                  # Enum: LOW, MEDIUM, HIGH
    │   │   └── LlmMessage.java                  # Value Object: role (system/user/assistant), content
    │   └── exception/
    │       ├── LlmProviderException.java        # Lỗi khi upstream LLM trả 5xx hoặc fail
    │       ├── LlmTimeoutException.java         # Lỗi khi gọi LLM quá timeout
    │       └── LlmSchemaValidationException.java# Lỗi khi LLM trả sai định dạng JSON/Pydantic
    │
    └── infrastructure/                           # [Outbound Adapter] Tương tác với Third-party API
        ├── client/
        │   ├── OpenAiClientAdapter.java         # Gọi OpenAI/Ollama/Gemini qua Spring RestClient
        │   └── dto/                             # Payload riêng của provider (OpenAI ChatCompletion request/response)
        └── config/
            ├── LlmProperties.java               # @ConfigurationProperties(prefix = "app.ai.llm")
            └── LlmConfig.java                   # Bean RestClient với connection/read timeout & retry

──────

### 2. Luồng thực thi (Mapping với Definition of Done của Slice 1)

    Client (POST /api/v1/ai/chat)
            ↓
    ChatController (Validate payload với Jakarta @Valid)
            ↓
    ChatService (Build System Prompt + User Message)
            ↓
    LlmClientPort / OpenAiClientAdapter (Gọi LLM qua RestClient với timeout + retry)
            ↓
    Parse & Validate Structured Output (Map về AssistantResponse)
            ↓
    ChatResponse (Bọc trong ApiResponse.ok(response))
            ↓
    Nếu Timeout/5xx/Invalid Schema → AiExceptionHandler bắt lỗi → ApiResponse.error(msg)

──────

### 3. Chi tiết triển khai code mẫu cho Slice 1

#### 3.1. Domain Model: Structured Output (AssistantResponse.java)

    package com.platform.app.ai.domain.model;

    public record AssistantResponse(
        String answer,
        Confidence confidence,
        int promptTokens,
        int completionTokens
    ) {}

    package com.platform.app.ai.domain.model;

    public enum Confidence {
        LOW,
        MEDIUM,
        HIGH
    }

──────

#### 3.2. Outbound Port: LlmClientPort.java

    package com.platform.app.ai.application.port.out;

    import java.util.List;
    import com.platform.app.ai.domain.model.AssistantResponse;
    import com.platform.app.ai.domain.model.LlmMessage;

    public interface LlmClientPort {
        AssistantResponse generateStructuredResponse(List<LlmMessage> messages, Double temperature);
    }

──────

#### 3.3. Configuration & Properties: LlmProperties.java

    package com.platform.app.ai.infrastructure.config;

    import java.time.Duration;
    import org.springframework.boot.context.properties.ConfigurationProperties;

    @ConfigurationProperties(prefix = "app.ai.llm")
    public record LlmProperties(
        String apiKey,
        String baseUrl,
        String model,
        Double defaultTemperature,
        Duration timeout,
        int maxRetries
    ) {}

Thêm vào application.yaml:

    app:
      ai:
        llm:
          api-key: ${OPENAI_API_KEY:mock-key}
          base-url: ${LLM_BASE_URL:https://api.openai.com/v1}
          model: ${LLM_MODEL:gpt-4o-mini}
          default-temperature: 0.2
          timeout: 10s
          max-retries: 3

──────

#### 3.4. Infrastructure Adapter: OpenAiClientAdapter.java

Tận dụng RestClient của Spring Boot với cấu hình timeout và retry rõ ràng:

    package com.platform.app.ai.infrastructure.client;

    import java.util.List;
    import java.util.Map;
    import org.springframework.http.MediaType;
    import org.springframework.stereotype.Component;
    import org.springframework.web.client.ResourceAccessException;
    import org.springframework.web.client.RestClient;
    import org.springframework.web.client.RestClientResponseException;

    import com.fasterxml.jackson.databind.ObjectMapper;
    import com.platform.app.ai.application.port.out.LlmClientPort;
    import com.platform.app.ai.domain.exception.LlmProviderException;
    import com.platform.app.ai.domain.exception.LlmTimeoutException;
    import com.platform.app.ai.domain.model.AssistantResponse;
    import com.platform.app.ai.domain.model.LlmMessage;
    import com.platform.app.ai.infrastructure.config.LlmProperties;

    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;

    @Slf4j
    @Component
    @RequiredArgsConstructor
    public class OpenAiClientAdapter implements LlmClientPort {

        private final RestClient restClient;
        private final LlmProperties properties;
        private final ObjectMapper objectMapper;

        @Override
        public AssistantResponse generateStructuredResponse(List<LlmMessage> messages, Double temperature) {
            // Build JSON Schema hoặc Function Call / response_format json_object
            var requestBody = Map.of(
                "model", properties.model(),
                "temperature", temperature != null ? temperature : properties.defaultTemperature(),
                "response_format", Map.of("type", "json_object"),
                "messages", messages.stream().map(m -> Map.of("role", m.role().name().toLowerCase(), "content", m.

content())).toList()
);

            try {
                var responseJson = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

                return parseLlmResponse(responseJson);
            } catch (ResourceAccessException ex) {
                log.error("LLM Provider Timeout or Connection Error: {}", ex.getMessage());
                throw new LlmTimeoutException("LLM request timed out after " + properties.timeout(), ex);
            } catch (RestClientResponseException ex) {
                log.error("LLM Provider returned error status: {} body: {}", ex.getStatusCode(), ex.

getResponseBodyAsString());
throw new LlmProviderException("LLM upstream failed with status " + ex.getStatusCode(), ex);
}
}

        private AssistantResponse parseLlmResponse(String responseJson) {
            // Parse content JSON string từ OpenAI payload và map vào AssistantResponse
            // ...
            return null;
        }
    }

──────

#### 3.5. Application Layer: Request DTO & Service

ChatRequest.java:

    package com.platform.app.ai.application.dto.request;

    import jakarta.validation.constraints.NotBlank;
    import jakarta.validation.constraints.Size;

    public record ChatRequest(
        @NotBlank(message = "Message cannot be blank")
        @Size(max = 4000, message = "Message exceeds maximum length")
        String message,

        Double temperature
    ) {}

ChatService.java:

    package com.platform.app.ai.application.service;

    import java.util.List;
    import org.springframework.stereotype.Service;

    import com.platform.app.ai.application.dto.request.ChatRequest;
    import com.platform.app.ai.application.dto.response.ChatResponse;
    import com.platform.app.ai.application.port.in.ChatUseCase;
    import com.platform.app.ai.application.port.out.LlmClientPort;
    import com.platform.app.ai.domain.model.LlmMessage;
    import com.platform.app.ai.domain.model.LlmRole;

    import lombok.RequiredArgsConstructor;

    @Service
    @RequiredArgsConstructor
    public class ChatService implements ChatUseCase {

        private final LlmClientPort llmClientPort;

        private static final String SYSTEM_PROMPT = """
            You are a customer support AI assistant.
            Always reply in valid JSON conforming to the schema:
            {
              "answer": string,
              "confidence": "LOW" | "MEDIUM" | "HIGH"
            }
            """;

        @Override
        public ChatResponse chat(ChatRequest request) {
            var messages = List.of(
                new LlmMessage(LlmRole.SYSTEM, SYSTEM_PROMPT),
                new LlmMessage(LlmRole.USER, request.message())
            );

            var response = llmClientPort.generateStructuredResponse(messages, request.temperature());

            return new ChatResponse(
                response.answer(),
                response.confidence().name(),
                response.promptTokens(),
                response.completionTokens()
            );
        }
    }

──────

#### 3.6. API Inbound: ChatController.java & AiExceptionHandler.java

ChatController.java:

    package com.platform.app.ai.api;

    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.PostMapping;
    import org.springframework.web.bind.annotation.RequestBody;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RestController;

    import com.platform.app.ai.application.dto.request.ChatRequest;
    import com.platform.app.ai.application.dto.response.ChatResponse;
    import com.platform.app.ai.application.port.in.ChatUseCase;
    import com.platform.app.shared.dto.ApiResponse;

    import io.swagger.v3.oas.annotations.Operation;
    import io.swagger.v3.oas.annotations.tags.Tag;
    import jakarta.validation.Valid;
    import lombok.RequiredArgsConstructor;

    @RestController
    @RequiredArgsConstructor
    @RequestMapping("/api/v1/ai/chat")
    @Tag(name = "AI Chat", description = "AI Assistant endpoints (Slice 1)")
    public class ChatController {

        private final ChatUseCase chatUseCase;

        @PostMapping
        @Operation(summary = "Send a prompt and receive structured AI answer")
        public ResponseEntity<ApiResponse<ChatResponse>> chat(@Valid @RequestBody ChatRequest request) {
            ChatResponse response = chatUseCase.chat(request);
            return ResponseEntity.ok(ApiResponse.ok("Message processed successfully", response));
        }
    }

AiExceptionHandler.java:

    package com.platform.app.ai.api;

    import org.springframework.core.Ordered;
    import org.springframework.core.annotation.Order;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.ExceptionHandler;
    import org.springframework.web.bind.annotation.RestControllerAdvice;

    import com.platform.app.ai.domain.exception.LlmProviderException;
    import com.platform.app.ai.domain.exception.LlmTimeoutException;
    import com.platform.app.shared.dto.ApiResponse;

    @RestControllerAdvice(basePackages = "com.platform.app.ai")
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public class AiExceptionHandler {

        @ExceptionHandler(LlmTimeoutException.class)
        public ResponseEntity<ApiResponse<Void>> handleTimeout(LlmTimeoutException ex) {
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                    .body(ApiResponse.error("AI service timed out. Please try again later."));
        }

        @ExceptionHandler(LlmProviderException.class)
        public ResponseEntity<ApiResponse<Void>> handleProviderError(LlmProviderException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(ApiResponse.error("AI provider returned an error: " + ex.getMessage()));
        }
    }

──────

### 4. Lợi ích khi tổ chức theo cấu trúc này

1. Đáp ứng 100% Definition of Done Slice 1: Có Bean Validation, Timeout, Retry, Structured Output, và Clean Exception
   Handling.
2. Decouple hoàn toàn Vendor: Khi đổi từ OpenAI sang Gemini/Claude/Ollama, chỉ cần đổi/thêm implementation của
   LlmClientPort trong layer infrastructure/, không phải sửa ChatService hay ChatController.
3. Chuẩn bị sẵn sàng cho Slice 2 & Slice 3:
   • Slice 2 chỉ cần thêm PromptTemplate vào application/ hoặc domain/.
   • Slice 3 chỉ cần thêm package rag/ (ingestion, vector store, embedding adapter) và inject vào ChatService.
