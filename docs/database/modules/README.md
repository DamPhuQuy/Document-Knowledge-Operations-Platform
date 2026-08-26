# Database Modules Overview

Tập hợp các tệp DBML mô tả độc lập từng phân hệ dữ liệu (Bounded Contexts) để thuận tiện quan sát và kiểm tra quan hệ trên [dbdiagram.io](https://dbdiagram.io).

---

### 1. [`01_iam_organization.dbml`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/docs/database/modules/01_iam_organization.dbml) — IAM & Organization
- **Ý nghĩa:** Quản lý định danh người dùng (`users`), cơ cấu phòng ban (`departments`), hệ thống phân quyền đa tầng RBAC (`roles`, `permissions`, `user_roles`, `role_permissions`) và quản lý vòng đời phiên xác thực (`refresh_tokens`).

### 2. [`02_document_management.dbml`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/docs/database/modules/02_document_management.dbml) — Document Management & Storage
- **Ý nghĩa:** Quản lý metadata tài liệu (`documents`), lịch sử các phiên bản tệp trên Object Storage S3 (`document_versions`), và kiểm soát quyền truy cập chi tiết (ACL) theo người dùng (`document_user_access`), phòng ban (`document_department_access`) và vai trò (`document_role_access`).

### 3. [`03_ai_knowledge_rag.dbml`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/docs/database/modules/03_ai_knowledge_rag.dbml) — AI Knowledge & Hybrid RAG
- **Ý nghĩa:** Quản lý các đoạn văn bản trích xuất theo từng phiên bản tài liệu (`document_chunks`), lưu trữ vector embedding (`pgvector` với HNSW index) và chỉ mục tìm kiếm toàn văn (`tsvector`) phục vụ công cụ tìm kiếm ngữ nghĩa Hybrid Search cho AI.

### 4. [`04_conversational_ai.dbml`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/docs/database/modules/04_conversational_ai.dbml) — Conversational AI & Citations
- **Ý nghĩa:** Quản lý phiên trò chuyện (`conversations`), lịch sử tin nhắn (`conversation_messages`), và bảng đối chiếu trích dẫn nguồn bằng chứng (`message_citations`) liên kết câu trả lời của AI với đúng tài liệu và đoạn chunk gốc.

### 5. [`05_workflow_automation.dbml`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/docs/database/modules/05_workflow_automation.dbml) — Workflow Automation
- **Ý nghĩa:** Định nghĩa các quy trình xử lý tự động hóa (`workflows`) kích hoạt theo sự kiện tài liệu, và quản lý tiến độ, dữ liệu đầu vào/kết quả từng bước của các phiên thực thi (`workflow_executions`).

### 6. [`06_operations_hitl.dbml`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/docs/database/modules/06_operations_hitl.dbml) — Operations & Human-in-the-Loop
- **Ý nghĩa:** Cung cấp cơ chế phê duyệt 2 pha an toàn do con người xét duyệt trước khi thực thi thay đổi dữ liệu (`action_approvals`), và quản lý luồng tác vụ nghiệp vụ cần nhân viên can thiệp xử lý (`operation_tasks`).

### 7. [`07_audit_notifications.dbml`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/docs/database/modules/07_audit_notifications.dbml) — Audit Trail & Notifications
- **Ý nghĩa:** Lưu vết nhật ký kiểm toán bất biến phục vụ bảo mật và giải trình (`audit_logs`), cùng hệ thống gửi thông báo trạng thái/yêu cầu hành động đến người dùng (`notifications`).
