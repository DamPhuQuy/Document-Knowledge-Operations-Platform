# Module AI (Customer Support Platform)

Module `com.platform.app.ai` chịu trách nhiệm cung cấp khả năng AI Assistant cho nền tảng Customer Support. Module được thiết kế theo kiến trúc **Clean Architecture / Ports & Adapters (Hexagonal Architecture)** nhằm đảm bảo tính độc lập, khả năng kiểm thử cao và dễ dàng hoán đổi hoặc mở rộng các nhà cung cấp mô hình ngôn ngữ lớn (LLM Providers).

---

## 1. Kiến trúc tổng quan (Architecture Overview)

Module tuân thủ nghiêm ngặt quy tắc phân tầng và chiều phụ thuộc hướng tâm (Dependency Inversion):

```
                        ┌───────────────────────────────────────────┐
                        │                 API Layer                 │
                        │             (Inbound Adapter)             │
                        │    ChatController, AiExceptionHandler    │
                        └─────────────────────┬─────────────────────┘
                                              │ calls
                                              ▼
                        ┌───────────────────────────────────────────┐
                        │             APPLICATION Layer             │
                        │       ChatUseCase (Inbound Port)          │
                        │       ChatService (Orchestrator)          │
                        │       LlmClientPort (Outbound Port)       │
                        └──────────────┬─────────────┬──────────────┘
                         implements    │             │ uses
                                       ▼             ▼
  ┌──────────────────────────────────────────┐     ┌──────────────────────────────────────────┐
  │           INFRASTRUCTURE Layer           │     │               DOMAIN Layer               │
  │            (Outbound Adapter)            │     │              (Pure Java/DDD)             │
  │ OpenAiClientAdapter, LlmConfig/Properties│     │ AssistantResponse, Confidence, LlmMessage│
  │ (Ollama, Claude, vLLM Adapters...)       │     │ LlmTimeoutException, LlmProviderException│
  └──────────────────────────────────────────┘     └──────────────────────────────────────────┘
```

### Chi tiết các tầng

| Tầng               | Package                                                          | Vai trò                                                                                                                | Ràng buộc kỹ thuật                                                          |
| :----------------- | :--------------------------------------------------------------- | :--------------------------------------------------------------------------------------------------------------------- | :-------------------------------------------------------------------------- |
| **Domain**         | `domain.model`<br>`domain.exception`                             | Chứa các thực thể, Value Objects, Enums và Domain Exceptions cốt lõi.                                                  | Pure Java, tuyệt đối **không phụ thuộc framework** (Spring, Jackson, v.v.). |
| **Application**    | `application.port`<br>`application.service`<br>`application.dto` | Định nghĩa Use Case (Inbound Port), giao tiếp hạ tầng (Outbound Port) và điều phối luồng nghiệp vụ (`ChatService`).    | Phụ thuộc vào Domain, không phụ thuộc chi tiết kỹ thuật ở Infrastructure.   |
| **Infrastructure** | `infrastructure.client`<br>`infrastructure.config`               | Triển khai các Outbound Port (`LlmClientPort`) kết nối với API bên ngoài (OpenAI, Ollama...), cấu hình HTTP Client.    | Chứa logic serialization, network timeout, retry, sanitize dữ liệu.         |
| **API**            | `api`                                                            | Inbound REST Controller nhận HTTP request, xác thực dữ liệu đầu vào và chuyển đổi Exception thành HTTP Response chuẩn. | Sử dụng Jakarta Validation (`@Valid`) và `ApiResponse<T>`.                  |

---

## 2. Cấu trúc thư mục (Directory Structure)

```text
com.platform.app.ai/
├── api/                                      # [Inbound Adapter]
│   ├── ChatController.java                  # REST endpoint: POST /api/v1/ai/chat
│   └── AiExceptionHandler.java              # Controller Advice chuẩn hóa lỗi Domain -> HTTP Status
│
├── application/                              # [Application Layer]
│   ├── dto/
│   │   ├── request/ChatRequest.java         # DTO đầu vào kèm Jakarta Validation
│   │   └── response/ChatResponse.java       # DTO trả về cho Client
│   ├── port/
│   │   ├── in/ChatUseCase.java              # Inbound Port interface
│   │   └── out/LlmClientPort.java           # Outbound Port interface
│   └── service/
│       └── ChatService.java                 # Use case implementation & prompt orchestration
│
├── domain/                                   # [Domain Layer - Zero Framework Coupling]
│   ├── model/
│   │   ├── AssistantResponse.java           # Record chứa phản hồi đã cấu trúc (answer, confidence, tokens)
│   │   ├── Confidence.java                  # Enum: LOW, MEDIUM, HIGH
│   │   ├── LlmMessage.java                  # Value object: role (SYSTEM/USER/ASSISTANT) & content
│   │   └── LlmRole.java                     # Enum định danh vai trò tin nhắn
│   └── exception/
│       ├── LlmTimeoutException.java         # Bắn ra khi request gọi LLM quá thời gian quy định
│       ├── LlmProviderException.java        # Bắn ra khi nhà cung cấp LLM trả lỗi (5xx, 429...)
│       └── LlmSchemaValidationException.java# Bắn ra khi LLM trả về sai schema JSON quy định
│
└── infrastructure/                           # [Outbound Adapter]
    ├── client/
    │   ├── dto/                             # Wire DTOs đặc thù của OpenAI Chat Completions API
    │   └── OpenAiClientAdapter.java         # Implementation của LlmClientPort sử dụng Spring RestClient
    └── config/
        ├── LlmProperties.java               # Type-safe Properties (prefix: app.ai.llm)
        └── LlmConfig.java                   # Cấu hình Bean RestClient và Timeouts
```

---

## 3. Các nguyên tắc kỹ thuật (Core Principles)

### 3.1. Phân tách nhà mạng LLM qua Outbound Port (`LlmClientPort`)

Toàn bộ nghiệp vụ trong `ChatService` chỉ tương tác với `LlmClientPort`. Core application không biết và không quan tâm API thực tế đằng sau là OpenAI, Anthropic Claude, Ollama hay vLLM.

```
                 ┌── OpenAiClientAdapter (Hiện tại)
LlmClientPort ───┼── OllamaClientAdapter (Mở rộng Local LLM)
                 ├── ClaudeClientAdapter (Mở rộng Anthropic)
                 └── VllmClientAdapter   (Mở rộng Private Cloud)
```

### 3.2. Structured Outputs & Schema Enforcement

- Model được cấu hình trả về định dạng JSON bắt buộc (`response_format: {"type": "json_object"}`).
- [OpenAiClientAdapter](file:///c:/Users/DamPhuQuy/Develop/Custom-support-platform/backend/src/main/java/com/platform/app/ai/infrastructure/client/OpenAiClientAdapter.java) thực hiện tiền xử lý làm sạch markdown fences (`json ... `) trước khi deserialize về [AssistantResponse](file:///c:/Users/DamPhuQuy/Develop/Custom-support-platform/backend/src/main/java/com/platform/app/ai/domain/model/AssistantResponse.java).
- Trường hợp payload thiếu trường bắt buộc (`answer`) hoặc JSON lỗi, hệ thống ném `LlmSchemaValidationException` để tầng API trả về mã `422 Unprocessable Entity`.

### 3.3. Cơ chế chịu lỗi (Resilience & Retry)

- **Timeout Boundaries**: Thiết lập `connectTimeout` và `readTimeout` nghiêm ngặt qua cấu hình `app.ai.llm.timeout` nhằm tránh tình trạng treo thread server khi upstream phản hồi chậm.
- **Exponential Backoff Retry**: Tự động thử lại khi gặp các lỗi tạm thời (`ResourceAccessException` do nghẽn mạng hoặc mã `5xx` / `429` từ LLM provider) trước khi kết luận thất bại.

### 3.4. Chuẩn hóa mã lỗi HTTP (Error Mapping)

| Domain Exception                  | HTTP Status Code           | Diễn giải                                                    |
| :-------------------------------- | :------------------------- | :----------------------------------------------------------- |
| `LlmTimeoutException`             | `504 Gateway Timeout`      | Hết thời gian chờ kết nối hoặc đọc dữ liệu từ LLM API        |
| `LlmProviderException`            | `502 Bad Gateway`          | LLM Provider gặp sự cố nội bộ hoặc từ chối phục vụ           |
| `LlmSchemaValidationException`    | `422 Unprocessable Entity` | LLM trả về cấu trúc không hợp lệ hoặc thiếu dữ liệu bắt buộc |
| `MethodArgumentNotValidException` | `400 Bad Request`          | Payload gửi lên từ client vi phạm ràng buộc validation       |

---

## 4. Cấu hình (Configuration Reference)

Các tham số cấu hình trong `application.yaml`:

```yaml
app:
  ai:
    llm:
      api-key: ${LLM_API_KEY:your-api-key}
      base-url: ${LLM_BASE_URL:https://api.openai.com/v1}
      model: ${LLM_MODEL:gpt-4o-mini}
      temperature: ${LLM_TEMPERATURE:0.2}
      timeout: ${LLM_TIMEOUT:10s}
      max-retries: ${LLM_MAX_RETRIES:3}
      retry-delay: ${LLM_RETRY_DELAY:500ms}
```

---

## 5. Tài liệu API (API Endpoints)

### `POST /api/v1/ai/chat`

Gửi tin nhắn yêu cầu tới AI Assistant và nhận về câu trả lời có cấu trúc.

**Headers:**

```http
Content-Type: application/json
Authorization: Bearer <JWT_ACCESS_TOKEN>
```

**Request Body:**

```json
{
  "message": "Làm thế nào để đổi mật khẩu tài khoản?",
  "temperature": 0.3
}
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Message processed successfully",
  "data": {
    "answer": "Bạn có thể đổi mật khẩu bằng cách truy cập mục Cài đặt tài khoản > Đổi mật khẩu.",
    "confidence": "HIGH",
    "promptTokens": 65,
    "completionTokens": 32
  },
  "timestamp": "2026-08-17T11:20:00Z"
}
```

---

## 6. Hướng dẫn mở rộng (Extensibility Guide)

### Thêm một LLM Provider mới (Ví dụ: Ollama cho Local AI)

1. Tạo class `OllamaClientAdapter` trong `infrastructure.client` implement `LlmClientPort`.
2. Sử dụng `@ConditionalOnProperty(prefix = "app.ai.llm", name = "provider", havingValue = "ollama")`.
3. Không cần chỉnh sửa bất kỳ dòng code nào trong tầng `domain` hay `application`.

### Lộ trình tích hợp các Slice tiếp theo

- **Slice 2 (Conversation State)**: Bổ sung `ChatHistoryPort` (outbound) và `ConversationSession` trong `application/service` để lưu trữ ngữ cảnh hội thoại vào PostgreSQL.
- **Slice 3 (RAG / Knowledge Base)**: Bổ sung `KnowledgeRetrieverPort` (outbound) để truy vấn vector embedding từ pgvector và inject evidence vào context của prompt.
- **Slice 4 (Tool Calling / Agent Loop)**: Bổ sung `ToolRegistryPort` và điều phối vòng lặp tool execution trong `application`.

---

## 7. Chiến lược kiểm thử (Testing Strategy)

- **Unit Test**: Kiểm thử độc lập logic của `ChatService` và `OpenAiClientAdapter` bằng Mockito (mock `LlmClientPort`, mock `RestClient`).
- **Integration Test**: Sử dụng `ChatControllerTest` kết hợp `@SpringBootTest` + `MockMvc` và `@MockitoBean` để kiểm thử toàn diện từ API endpoint, Authentication/Authorization, Request Validation cho đến Exception Handler.

Chạy toàn bộ test suite:

```bash
./gradlew.bat test --tests "com.platform.app.ai.*"
```
