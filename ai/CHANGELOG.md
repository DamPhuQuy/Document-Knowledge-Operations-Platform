# Changelog

## v2.0 - Python Migration & Clean Architecture (Hexagonal)

### Added

- Khởi tạo độc lập dự án AI Microservice viết bằng Python, được quản lý thông qua công cụ `uv` hiện đại.
- Tái thiết kế toàn bộ hệ thống theo mô hình **Clean Architecture / Ports & Adapters (Hexagonal)**:
  - **Domain Layer:** Định nghĩa thực thể độc lập (`LlmMessage`, `AssistantResponse`, `Confidence`) và lỗi nghiệp vụ lõi (`LlmTimeoutException`, `LlmProviderException`, `LlmSchemaValidationException`).
  - **Application Layer:** Khai báo cổng nghiệp vụ (`port_in.ChatUseCase`, `port_out.LlmClientPort`) và điều phối xử lý (`ChatService`).
  - **Infrastructure Layer:** Quản trị cấu hình (`LlmConfig`) và tích hợp adapter gọi ngoài (`OpenAiClientAdapter`).
  - **API Layer (Inbound Adapter):** Triển khai lớp `AiGrpcController` tiếp nhận yêu cầu gRPC từ Java backend và chuyển đổi kiểu dữ liệu.
  - **Generated Layer:** Biên dịch stubs tự động vào gói `src/ai/generated` kèm file `.pyi` hỗ trợ hiển thị IntelliSense trên IDE.
- Thiết lập cấu hình tĩnh loại trừ cảnh báo linter cho tệp tự sinh trong `pyproject.toml` (cho Ruff, Pylint) và `pyrightconfig.json` (cho Pyright/Pylance).

### Changed

- Cập nhật `OpenAiClientAdapter` phía Python sử dụng chuẩn **OpenAI Responses API** (`client.responses.create(...)`), bóc tách `instructions`, `input` và bắt buộc định dạng qua `text={"format": {"type": "json_object"}}`.
- Loại bỏ hoàn toàn module Java AI cũ trong Java backend, thay thế bằng `GrpcAiClientAdapter` kết nối đến Python AI Service qua gRPC trên cổng `50051`.

---

## v1.2

### Changed

- OpenAI Client implementation migrated to Responses API:
  ```
  OpenAiClientAdapter
          │
          ├── before: Chat Completions / old mapping
          │
          └── now:
                ResponseCreateParams
                      ↓
                Responses API
                      ↓
                ResponseOutputItem
                      ↓
                Structured Output
  ```
- Nâng cấp `OpenAiClientAdapter.java` chuyển sang chuẩn **OpenAI Responses API** mới (`/v1/responses` endpoint) thông qua official SDK (`openAIClient.responses().create(...)`).
- Sử dụng `ResponseCreateParams`, tách biệt rõ ràng giữa `instructions` (Developer/System Prompt), `inputOfResponse` (Conversation turns với `EasyInputMessage`) và `ResponseTextConfig` (JSON Object format enforcement).
- Xử lý và trích xuất dữ liệu kết quả từ `ResponseOutputItem` -> `ResponseOutputMessage` -> `ResponseOutputText`.
- Đọc số lượng token đầu vào/đầu ra qua `ResponseUsage` (`inputTokens()`, `outputTokens()`).
- Cập nhật test suite `OpenAiClientAdapterTest.java` mock chuẩn Responses API payload và kiểm thử đầy đủ các kịch bản.

---

## v1.1

### Changed

- Chuyển đổi từ `RestClient` thủ công sang **Official OpenAI Java SDK** (`com.openai:openai-java:0.42.0`).
- Tinh gọn `OpenAiClientAdapter.java` với type-safe builder và SDK error handling.
- Tận dụng cơ chế built-in timeout và retry của official SDK.
- Cập nhật test suite `OpenAiClientAdapterTest.java` tương thích 100% với OpenAIClient.

### Removed

- Xóa bỏ 5 file DTOs wire format thủ công (`OpenAiChatRequest`, `OpenAiChatResponse`, `OpenAiChoiceDto`, `OpenAiMessageDto`, `OpenAiUsageDto`).

---

## v1.0

### Added

- Codebase cho Slice 1 (LLM API thật sự) trong module ai đã được triển khai hoàn chỉnh, tuân thủ nguyên tắc Clean Architecture / DDD (Hexagonal Architecture) và đáp ứng 100% Definition of Done.
