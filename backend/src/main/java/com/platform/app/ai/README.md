# AI Module

## 1. Overview

Module `com.platform.app.ai` chịu trách nhiệm cung cấp khả năng AI Assistant cho nền tảng Customer Support. Module được thiết kế theo kiến trúc **Clean Architecture / Ports & Adapters (Hexagonal Architecture)** nhằm đảm bảo tính độc lập, khả năng kiểm thử cao và dễ dàng hoán đổi hoặc mở rộng các nhà cung cấp mô hình ngôn ngữ lớn (LLM Providers).

> **Vai trò trong tiến trình Agentic**: Slice 1 được thiết kế dưới dạng một **Atomic Execution Step (hàm suy luận đơn nguyên tử)**. Thiết kế cô lập này đảm bảo khi tiến lên **Slice 4 (Agentic Loop)**, hệ thống chỉ việc tái sử dụng Slice 1 làm Execution Node bên trong vòng lặp ReAct / State Machine mà không phải chỉnh sửa logic gọi mô hình.

---

## 2. Current Architecture

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

API
↓
Application
↓
Outbound Port
↓
Infrastructure
↓
LLM Provider

### Chi tiết các tầng

| Tầng               | Package                                                          | Vai trò                                                                                                                | Ràng buộc kỹ thuật                                                          |
| :----------------- | :--------------------------------------------------------------- | :--------------------------------------------------------------------------------------------------------------------- | :-------------------------------------------------------------------------- |
| **Domain**         | `domain.model`<br>`domain.exception`                             | Chứa các thực thể, Value Objects, Enums và Domain Exceptions cốt lõi.                                                  | Pure Java, tuyệt đối **không phụ thuộc framework** (Spring, Jackson, v.v.). |
| **Application**    | `application.port`<br>`application.service`<br>`application.dto` | Định nghĩa Use Case (Inbound Port), giao tiếp hạ tầng (Outbound Port) và điều phối luồng nghiệp vụ (`ChatService`).    | Phụ thuộc vào Domain, không phụ thuộc chi tiết kỹ thuật ở Infrastructure.   |
| **Infrastructure** | `infrastructure.client`<br>`infrastructure.config`               | Triển khai các Outbound Port (`LlmClientPort`) kết nối với API bên ngoài (OpenAI, Ollama...), cấu hình HTTP Client.    | Chứa logic serialization, network timeout, retry, sanitize dữ liệu.         |
| **API**            | `api`                                                            | Inbound REST Controller nhận HTTP request, xác thực dữ liệu đầu vào và chuyển đổi Exception thành HTTP Response chuẩn. | Sử dụng Jakarta Validation (`@Valid`) và `ApiResponse<T>`.                  |

---

## 3. Directory Structure

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
    │   ├── dto/                             # Payload schema parsing (OpenAiStructuredOutputPayload)
    │   └── OpenAiClientAdapter.java         # Implementation của LlmClientPort sử dụng Official OpenAI Java SDK (Responses API)
    └── config/
        ├── LlmProperties.java               # Type-safe Properties (prefix: app.ai.llm)
        └── LlmConfig.java                   # Cấu hình Bean OpenAIClient với Timeouts & Retries
```

---

## 4. Core Design Principles

- **Ports & Adapters (Hexagonal Architecture)**: Tách biệt hoàn toàn phần logic nghiệp vụ lõi (Domain/Application) khỏi các chi tiết hạ tầng (LLM Providers, REST APIs) qua hệ thống Port & Adapter.
- **Structured Output Mode**: Ép kiểu JSON đầu ra để giao tiếp an toàn, tin cậy, tránh việc LLM trả về dữ liệu tự do phi cấu trúc.
- **Fail-safe & Resilient Design**: Tích hợp các cơ chế quản trị độ trễ (timeout), retry tự động và chuẩn hóa lỗi nghiệp vụ thành HTTP Status Code.

---

## 5. Configuration

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

## 6. API

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

## 7. Testing

Chạy toàn bộ test suite:

```bash
./gradlew.bat test --tests "com.platform.app.ai.*"
```

---

## 8. Extension

### Thêm một LLM Provider mới (Ví dụ: Ollama cho Local AI)

1. Tạo class `OllamaClientAdapter` trong `infrastructure.client` implement `LlmClientPort`.
2. Sử dụng `@ConditionalOnProperty(prefix = "app.ai.llm", name = "provider", havingValue = "ollama")`.
3. Không cần chỉnh sửa bất kỳ dòng code nào trong tầng `domain` hay `application`.

---

## 9. Documentation Links

- [Architecture Design](docs/ARCHITECTURE.md) — Chi tiết kiến trúc chi tiết, sequence diagrams, request lifecycle và data transformations.
- [Learning Roadmap & Notes](docs/LEARNING.md) — Tổng hợp kiến thức thu thập và engineering journal qua từng slices.
- [Changelog](CHANGELOG.md) — Lịch sử cập nhật của module qua từng phiên bản.

---

## Roadmap

- Slice 1 — LLM Foundation ✅
- Slice 2 — Conversation State
- Slice 3 — RAG
- Slice 4 — Agentic Tool Calling

See [Learning Notes](docs/LEARNING.md) for the complete learning roadmap.
