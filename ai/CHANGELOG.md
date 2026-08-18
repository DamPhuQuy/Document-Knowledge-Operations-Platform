# Changelog

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
- Nâng cấp [OpenAiClientAdapter.java](infrastructure/client/OpenAiClientAdapter.java) chuyển sang chuẩn **OpenAI Responses API** mới (`/v1/responses` endpoint) thông qua official SDK (`openAIClient.responses().create(...)`).
- Sử dụng `ResponseCreateParams`, tách biệt rõ ràng giữa `instructions` (Developer/System Prompt), `inputOfResponse` (Conversation turns với `EasyInputMessage`) và `ResponseTextConfig` (JSON Object format enforcement).
- Xử lý và trích xuất dữ liệu kết quả từ `ResponseOutputItem` -> `ResponseOutputMessage` -> `ResponseOutputText`.
- Đọc số lượng token đầu vào/đầu ra qua `ResponseUsage` (`inputTokens()`, `outputTokens()`).
- Cập nhật test suite [OpenAiClientAdapterTest.java](../../../../../../test/java/com/platform/app/ai/infrastructure/client/OpenAiClientAdapterTest.java) mock chuẩn Responses API payload và kiểm thử đầy đủ các kịch bản.

---

## v1.1

### Changed

- Chuyển đổi từ `RestClient` thủ công sang **Official OpenAI Java SDK** (`com.openai:openai-java:0.42.0`).
- Tinh gọn [OpenAiClientAdapter.java](infrastructure/client/OpenAiClientAdapter.java) với type-safe builder và SDK error handling.
- Tận dụng cơ chế built-in timeout và retry của official SDK.
- Cập nhật test suite [OpenAiClientAdapterTest.java](../../../../../../test/java/com/platform/app/ai/infrastructure/client/OpenAiClientAdapterTest.java) tương thích 100% với OpenAIClient.

### Removed

- Xóa bỏ 5 file DTOs wire format thủ công (`OpenAiChatRequest`, `OpenAiChatResponse`, `OpenAiChoiceDto`, `OpenAiMessageDto`, `OpenAiUsageDto`).

---

## v1.0

### Added

- Codebase cho Slice 1 (LLM API thật sự) trong module ai đã được triển khai hoàn chỉnh, tuân thủ nguyên tắc Clean Architecture / DDD (Hexagonal Architecture) và đáp ứng 100% Definition of Done.
