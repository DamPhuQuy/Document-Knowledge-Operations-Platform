# Change log

## Change log v1.2

- Nâng cấp [OpenAiClientAdapter.java](infrastructure/client/OpenAiClientAdapter.java) chuyển sang chuẩn **OpenAI Responses API** mới (`/v1/responses` endpoint) thông qua official SDK (`openAIClient.responses().create(...)`).
- Sử dụng `ResponseCreateParams`, tách biệt rõ ràng giữa `instructions` (Developer/System Prompt), `inputOfResponse` (Conversation turns với `EasyInputMessage`) và `ResponseTextConfig` (JSON Object format enforcement).
- Xử lý và trích xuất dữ liệu kết quả từ `ResponseOutputItem` -> `ResponseOutputMessage` -> `ResponseOutputText`.
- Đọc số lượng token đầu vào/đầu ra qua `ResponseUsage` (`inputTokens()`, `outputTokens()`).
- Cập nhật test suite [OpenAiClientAdapterTest.java](../../../../../../test/java/com/platform/app/ai/infrastructure/client/OpenAiClientAdapterTest.java) mock chuẩn Responses API payload và kiểm thử đầy đủ các kịch bản (happy path, markdown code fence stripping, JSON schema parse failure, timeout/IO exception, provider error).

──────

## Change log v1.1

- Chuyển đổi từ `RestClient` thủ công sang **Official OpenAI Java SDK** (`com.openai:openai-java:0.42.0`).
- Xóa bỏ 5 file DTOs wire format thủ công (`OpenAiChatRequest`, `OpenAiChatResponse`, `OpenAiChoiceDto`, `OpenAiMessageDto`, `OpenAiUsageDto`).
- Tinh gọn [OpenAiClientAdapter.java](infrastructure/client/OpenAiClientAdapter.java) với type-safe builder và SDK error handling.
- Tận dụng cơ chế built-in timeout và retry của official SDK.
- Cập nhật test suite [OpenAiClientAdapterTest.java](../../../../../../test/java/com/platform/app/ai/infrastructure/client/OpenAiClientAdapterTest.java) tương thích 100% với OpenAIClient.

──────

## Change log v1.0

Codebase cho Slice 1 (LLM API thật sự) trong module ai đã được triển khai hoàn chỉnh, tuân thủ nguyên tắc Clean Architecture / DDD (Hexagonal Architecture) và đáp ứng 100% Definition of Done.
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
        │   └── OpenAiClientAdapter.java                 # Implements LlmClientPort dùng Official OpenAI Java SDK (Responses API)
        └── config/
            ├── LlmProperties.java                       # @ConfigurationProperties(prefix = "app.ai.llm")
            └── LlmConfig.java                           # Config OpenAIClient với connectTimeout, requestTimeout và maxRetries
