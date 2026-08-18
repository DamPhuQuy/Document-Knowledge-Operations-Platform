# AI Microservice (Python)

## 1. Tổng quan

Dịch vụ độc lập `ai` chịu trách nhiệm cung cấp khả năng AI Assistant cho hệ thống. Dự án được triển khai bằng **Python 3.11+**, quản lý package qua **`uv`** và kết nối trực tiếp với Java Backend thông qua giao thức **HTTP REST API** (FastAPI) trên cổng `8000`.

Dịch vụ được thiết kế nghiêm ngặt theo kiến trúc **Clean Architecture / Ports & Adapters (Hexagonal Architecture)** nhằm đảm bảo tính độc lập, khả năng kiểm thử cao và dễ dàng mở rộng.

---

## 2. Kiến trúc Clean Architecture (Hexagonal)

Cấu trúc phân tầng và luồng phụ thuộc (Dependency Inversion):

```
                        ┌───────────────────────────────────────────┐
                        │                 API Layer                 │
                        │             (Inbound Adapter)             │
                        │               AiController                │
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
   │            (Outbound Adapter)            │     │           (Pure Python/No FW)            │
   │      OpenAiClientAdapter, LlmConfig      │     │ AssistantResponse, Confidence, LlmMessage│
   └──────────────────────────────────────────┘     └──────────────────────────────────────────┘
```

### Chi tiết các phân tầng

| Tầng               | Thư mục                                                                  | Vai trò                                                                                                             | Ràng buộc kỹ thuật                                               |
| :----------------- | :----------------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------ | :--------------------------------------------------------------- |
| **Domain**         | `domain/model`<br>`domain/exception`                                     | Thực thể cốt lõi, Value Objects, Enums và Domain Exceptions.                                                        | Không phụ thuộc vào bất kỳ framework hay thư viện ngoài nào.     |
| **Application**    | `application/port_in`<br>`application/port_out`<br>`application/service` | Định nghĩa Use Case (Inbound Port), giao tiếp hạ tầng (Outbound Port) và điều phối luồng nghiệp vụ (`ChatService`). | Phụ thuộc vào Domain, độc lập với thư viện bên ngoài và API.     |
| **Infrastructure** | `infrastructure/client`<br>`infrastructure/config`                       | Triển khai Outbound Port (`LlmClientPort`) kết nối với OpenAI SDK, quản lý cấu hình nạp biến môi trường.            | Chứa logic tích hợp API, xử lý timeout, retry, sanitize dữ liệu. |
| **API**            | `api`                                                                    | Triển khai Inbound Adapter đón nhận REST request từ Java backend và xử lý ánh xạ ngoại lệ.                         | Chứa các router và endpoint của FastAPI.                         |

---

## 3. Cấu trúc thư mục

```text
ai/
├── src/ai/                                     # Thư mục mã nguồn chính của module AI
│   ├── api/
│   │   └── ai_controller.py                    # Inbound Adapter: HTTP Router & Endpoints
│   ├── application/
│   │   ├── port_in/
│   │   │   └── chat_use_case.py                # Giao diện Inbound Port ChatUseCase
│   │   ├── port_out/
│   │   │   └── llm_client_port.py              # Giao diện Outbound Port LlmClientPort
│   │   └── service/
│   │       └── chat_service.py                 # Triển khai Use Case chính
│   ├── domain/
│   │   ├── exception/
│   │   │   └── exceptions.py                   # Lỗi Domain chuẩn hóa
│   │   └── model/
│   │       ├── assistant_response.py           # DTO domain của Assistant
│   │       ├── confidence.py                   # Enum mức độ tin cậy
│   │       ├── llm_message.py                  # Tin nhắn hội thoại
│   │       └── llm_role.py                     # Enum vai trò tin nhắn (SYSTEM, USER, ASSISTANT)
│   ├── infrastructure/
│   │   ├── client/
│   │   │   └── openai_client_adapter.py        # Outbound Adapter kết nối OpenAI SDK (Responses API)
│   │   └── config/
│   │       ├── config.py                       # Đọc biến cấu hình từ môi trường
│   │       └── container.py                    # Dependency Injection Container (dependency-injector)
│   └── main.py                                 # Điểm khởi chạy REST API server & DI Bootstrapper
├── pyproject.toml                              # Quản lý dependencies (uv) và linter rules
└── .env                                        # Lưu cấu hình biến môi trường cục bộ
```

---

## 4. Cấu hình biến môi trường (`.env`)

Mẫu cấu hình trong file `.env`:

```env
LLM_API_KEY=your-api-key
LLM_BASE_URL=https://api.openai.com/v1
LLM_MODEL=gpt-4o-mini
LLM_TEMPERATURE=0.2
AI_HTTP_PORT=8000
```

---

## 5. Hướng dẫn cài đặt và khởi chạy

Dự án sử dụng trình quản lý package **`uv`** của Astral để tối ưu hóa hiệu năng cài đặt.

### Bước 1: Khởi tạo và cài đặt dependencies

```bash
uv sync
```

### Bước 2: Kích hoạt môi trường ảo (Virtual Env)

- **Windows (PowerShell):**
  ```powershell
  (Set-ExecutionPolicy -Scope Process -ExecutionPolicy RemoteSigned) ; (& .venv\Scripts\Activate.ps1)
  ```
- **macOS/Linux:**
  ```bash
  source .venv/bin/activate
  ```

### Bước 3: Khởi chạy REST API Server

```bash
uv run python src/ai/main.py
```

Server sẽ chạy trên cổng `8000`. Bạn có thể truy cập Swagger UI để test API tại `http://localhost:8000/docs`.

---

## 6. REST API Endpoints

### `POST /api/v1/ai/generate`

Gửi tin nhắn yêu cầu tới AI Assistant và nhận về câu trả lời có cấu trúc dưới dạng JSON hoặc stream.

**Request Headers:**
```http
Content-Type: application/json
```

**Request Body:**
```json
{
  "messages": [
    {
      "role": "user",
      "content": "Reset my password"
    }
  ],
  "temperature": 0.2,
  "stream": false
}
```

**Response Body (200 OK):**
```json
{
  "answer": "Password reset link sent.",
  "confidence": "HIGH",
  "prompt_tokens": 12,
  "completion_tokens": 5
}
```

---

## Roadmap

- Slice 1 — LLM Foundation ✅ (Đã chuyển đổi độc lập sang Python FastAPI REST Service)
- Slice 2 — Conversation State
- Slice 3 — RAG
- Slice 4 — Agentic Tool Calling
