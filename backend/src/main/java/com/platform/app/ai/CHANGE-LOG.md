# Change log

## Change log v1.0

Codebase cho Slice 1 (LLM API thật sự) trong module ai đã được triển khai hoàn chỉnh, tuân thủ nguyên tắc Clean Architecture / DDD (Hexagonal Architecture) và đáp ứng
100% Definition of Done.
──────

### 1. Kiến trúc thư mục đã triển khai

    com.platform.app.ai/
    ├── api/                                              # [Inbound Adapter]
    │   ├── ChatController.java                          # POST /api/v1/ai/chat
    │   └── AiExceptionHandler.java                      # Map LlmTimeoutException (504), LlmProviderException (502), LlmSchemaValidationException (422)
    │
    ├── application/                                      # [Application Layer]
    │   ├── dto/
    │   │   ├── request/ChatRequest.java                 # @NotBlank, @Size(max=4000), @DecimalMin/@DecimalMax temperature
    │   │   └── response/ChatResponse.java               # answer, confidence (LOW/MEDIUM/HIGH), promptTokens, completionTokens
    │   ├── port/
    │   │   ├── in/ChatUseCase.java                      # Inbound Port interface
    │   │   └── out/LlmClientPort.java                   # Outbound Port interface
    │   └── service/
    │       └── ChatService.java                         # Implements ChatUseCase, điều phối System Prompt & gọi LlmClientPort
    │
    ├── domain/                                           # [Core Domain Layer - Pure Java]
    │   ├── model/
    │   │   ├── LlmRole.java                             # SYSTEM, USER, ASSISTANT
    │   │   ├── LlmMessage.java                          # Value Object (role, content)
    │   │   ├── Confidence.java                          # Enum (LOW, MEDIUM, HIGH)
    │   │   └── AssistantResponse.java                   # Structured Output domain model
    │   └── exception/
    │       ├── LlmTimeoutException.java                 # Bắn ra khi request quá timeout / network fail
    │       ├── LlmProviderException.java                # Bắn ra khi provider trả 4xx/5xx
    │       └── LlmSchemaValidationException.java        # Bắn ra khi LLM trả sai cấu trúc JSON schema
    │
    └── infrastructure/                                   # [Outbound Adapter]
        ├── client/
        │   ├── dto/                                     # OpenAI Chat Completion Wire DTOs (Request, Response, Choice, Usage)
        │   └── OpenAiClientAdapter.java                 # Implements LlmClientPort dùng RestClient thuần, retry exponential backoff & sanitize JSON
        └── config/
            ├── LlmProperties.java                       # @ConfigurationProperties(prefix = "app.ai.llm")
            └── LlmConfig.java                           # Config RestClient với connectTimeout/readTimeout

──────

### 2. Các điểm kỹ thuật nổi bật

1. Không phụ thuộc Framework AI trung gian: Sử dụng Spring RestClient thuần để kết nối trực tiếp API tương thích OpenAI / Ollama / Gemini, kiểm soát toàn diện request
   timeout, payload schema và retry logic.
2. Structured Outputs & Schema Validation:
   • Sử dụng OpenAI response_format: {"type": "json_object"}.
   • OpenAiClientAdapter.java xử lý làm sạch markdown fences (json ... ) và parse trực tiếp sang AssistantResponse.
   • Nếu output thiếu trường answer hoặc format sai, hệ thống ném LlmSchemaValidationException.java.
3. Resilience (Timeout & Exponential Backoff Retry):
   • Request timeout được cấu hình qua LlmProperties.java (app.ai.llm.timeout).
   • Tự động retry theo lũy thừa với các lỗi mạng tạm thời (ResourceAccessException) hoặc 5xx/429.
4. Chuẩn hóa Error Handling:
   • AiExceptionHandler.java bọc lỗi trả về định dạng ApiResponse:
   • Timeout → 504 Gateway Timeout
   • Upstream Provider Error → 502 Bad Gateway
   • Schema Validation Error → 422 Unprocessable Entity

──────

### 3. Basic flow

```java

    ChatController
    ↓
    ChatUseCase
    ↓
    ChatService
    ↓
    LlmClientPort
    ↓
    OpenAiClientAdapter

Sau đó:

                 ┌─ OpenAI Adapter
LlmClientPort ───┼─ Ollama Adapter
                 ├─ Anthropic Adapter
                 └─ vLLM Adapter

```

### 4. Bộ Unit Test & Integration Test (100% Passed)

• ChatServiceTest.java: Kiểm thử việc đóng gói System Prompt, User Message, ủy quyền cho Port và map response.
• OpenAiClientAdapterTest.java: Kiểm thử parse JSON sạch, JSON bọc trong markdown, xử lý lỗi schema, lỗi timeout và lỗi 500 từ provider.
• ChatControllerTest.java: Kiểm thử end-to-end qua MockMvc xác thực:
• POST /api/v1/ai/chat thành công trả về 200 OK với dữ liệu cấu trúc (answer, confidence, tokenUsage).
• Validation lỗi rỗng / vượt độ dài trả về 400 Bad Request.
• Chưa đăng nhập trả về 401 Unauthorized.
• Timeout trả về 504 Gateway Timeout.
• Lỗi upstream trả về 502 Bad Gateway.
• Sai schema trả về 422 Unprocessable Entity.`
