# Thiết Kế Cơ Sở Dữ Liệu Khởi Tạo (Database Schema Specification V1)

> **Dự án:** Nền tảng quản lý tài liệu nội bộ và tự động hóa nghiệp vụ (*Document Knowledge & Operations Platform*)  
> **Tài liệu tham chiếu:** Báo cáo đề tài PBL4 ([`report.docx`](../../reports/foundation_report.docx)), Kiến trúc hệ thống ([`architecture.md`](../architecture.md)), Business MVP ([`MVP.md`](../business/MVP.md)).  
> **Tệp đặc tả DBML nguồn:** [`schema.dbml`](schema.dbml) *(Trực quan hóa trên [dbdiagram.io](https://dbdiagram.io))*.  
> **Hệ quản trị CSDL:** PostgreSQL 16 + Extension `pgvector` (Phase 2).  
> **Lưu trữ nhị phân (Object Storage):** Amazon S3 (mã hóa SSE-AES256, presigned URLs $\le 15$ phút).

---

## 1. Tổng Quan Kiến Trúc Dữ Liệu (Data Architecture Overview)

Hệ thống được thiết kế theo hướng **Domain-Driven Design (DDD) & Modular Bounded Contexts**, phân tách rõ ràng theo chiến lược phân kỳ phát triển:

```text
+---------------------------------------------------------------------------------------------------+
|                     PHASE 1: CORE CLOUD MVP BASELINE (Ưu Tiên Hạ Tầng AWS & Nghiệp Vụ)             |
+-------------------+--------------------+-----------------------+------------------+---------------+
| IAM_Organization  |Document_Management |  Workflow_Automation  | Operations_HITL  | Audit_System  |
| (Users, Depts,    | (Docs S3 Meta,     | (Workflows, Execs,    | (2-Phase Approvals| (Immutable Log|
|  Roles, Perms)    |  Versions, ACL)    |  Execution Pipeline)  |  HITL Tasks)     |  Alerts)      |
+-------------------+--------------------+-----------------------+------------------+---------------+
                                                     |
                                                     v (Pluggable Fast-Follow)
+---------------------------------------------------------------------------------------------------+
|                     PHASE 2: PLUGGABLE AI KNOWLEDGE & RAG EXTENSION (Mở Rộng Sau)                 |
+----------------------------------------+----------------------------------------------------------+
|          AI_Knowledge_RAG              |                   Conversational_AI                      |
| (Semantic Chunks, 1536d HNSW, tsvector)| (Sessions, Message History, Citations, Confidence Guard)  |
+----------------------------------------+----------------------------------------------------------+
```

### Nguyên Tắc Lưu Trữ & Bất Biến (Invariants)
1. **Zero BLOBs in PostgreSQL:** Toàn bộ tệp nhị phân (PDF, DOCX, XLSX, TXT) lưu trực tiếp trên AWS S3. Cơ sở dữ liệu chỉ lưu trữ metadata, bucket, S3 object key, checksum SHA-256 và trạng thái đồng bộ `is_s3_synced`.
2. **Phase 1 Document Lifecycle:** Tài liệu tải lên S3 thành công nhận ngay trạng thái `processing_status = 'UPLOADED'`, cho phép tải xuống và phân quyền nghiệp vụ an toàn mà chưa cần phụ thuộc vào AI worker.
3. **2-Phase HITL Safety Gate:** Các thao tác nghiệp vụ nhạy cảm hoặc phá hủy bắt buộc trải qua phê duyệt 2 pha (Prepare/Diff Preview $\rightarrow$ Waiting Approval $\rightarrow$ Commit) kèm khóa chống lặp `idempotency_key`.
4. **Append-Only Immutable Audit:** Bảng `audit_logs` chỉ hỗ trợ thao tác ghi mới (INSERT), nghiêm cấm UPDATE/DELETE để đảm bảo giá trị pháp lý và truy vết bảo mật.
5. **Soft Delete Compliance:** Bản ghi tài liệu áp dụng cơ chế xóa mềm qua trường `deleted_at` có đánh chỉ mục, không xóa vật lý trực tiếp.

---

## 2. Từ Điển Dữ Liệu (Data Dictionary)

---

### 2.1. Nhóm Phase 1: Core Cloud MVP Baseline

#### A. Phân Hệ IAM & Tổ Chức (`Phase1_IAM_Organization`)

##### 1. `departments` (Phòng ban / Đơn vị tổ chức)
Xác định phạm vi sở hữu tài liệu (`document ownership`) và cách ly dữ liệu giữa các đơn vị.

| Cột | Kiểu | Ràng Buộc | Ý Nghĩa Nghiệp Vụ |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | **PK** | Định danh duy nhất phòng ban |
| `code` | `VARCHAR(50)` | **UNIQUE, NOT NULL** | Mã viết tắt duy nhất (e.g. `IT`, `HR`, `FIN`, `LEGAL`) |
| `name` | `VARCHAR(255)` | **NOT NULL** | Tên phòng ban đầy đủ |
| `description` | `TEXT` | | Chức năng, nhiệm vụ chính |
| `created_at` / `updated_at` | `TIMESTAMPTZ` | **NOT NULL** | Dấu thời gian tạo và cập nhật bản ghi |

##### 2. `users` (Tài khoản người dùng)
Lưu trữ định danh người dùng, mật khẩu băm, phòng ban chủ quản và cờ phân loại bảo mật.

| Cột | Kiểu | Ràng Buộc | Ý Nghĩa Nghiệp Vụ |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | **PK** | Định danh duy nhất người dùng |
| `email` | `VARCHAR(255)` | **UNIQUE, NOT NULL** | Email đăng nhập doanh nghiệp |
| `password_hash` | `VARCHAR(255)` | **NOT NULL** | Chuỗi băm mật khẩu bảo mật (BCrypt/Argon2) |
| `full_name` | `VARCHAR(255)` | **NOT NULL** | Họ và tên hiển thị |
| `department_id` | `BIGINT` | **FK** $\rightarrow$ `departments(id)` | Phòng ban trực thuộc (NULL nếu là đối tác bên ngoài) |
| `enabled` | `BOOLEAN` | **NOT NULL, DEFAULT TRUE** | Trạng thái kích hoạt tài khoản |
| `is_internal` | `BOOLEAN` | **NOT NULL, DEFAULT TRUE** | Phân biệt nhân sự nội bộ hay nhà thầu/khách hàng |
| `created_at` / `updated_at` | `TIMESTAMPTZ` | **NOT NULL** | Dấu thời gian tạo và cập nhật |

##### 3. `roles` (Vai trò hệ thống)
Định nghĩa các nhóm vai trò nghiệp vụ (e.g. `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_STAFF`, `ROLE_AUDITOR`).

| Cột | Kiểu | Ràng Buộc | Ý Nghĩa Nghiệp Vụ |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | **PK** | Định danh vai trò |
| `code` | `VARCHAR(50)` | **UNIQUE, NOT NULL** | Mã vai trò chuẩn hóa |
| `name` | `VARCHAR(100)` | **NOT NULL** | Tên vai trò hiển thị |
| `description` | `TEXT` | | Mô tả phạm vi trách nhiệm |
| `created_at` | `TIMESTAMPTZ` | **NOT NULL** | Thời điểm khởi tạo |

##### 4. `permissions` (Quyền hạn chức năng mức hạt mịn)
Định nghĩa quyền hạn cụ thể (e.g. `DOC_READ`, `DOC_WRITE`, `APPROVAL_COMMIT`, `AUDIT_VIEW`).

| Cột | Kiểu | Ràng Buộc | Ý Nghĩa Nghiệp Vụ |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | **PK** | Định danh quyền hạn |
| `code` | `VARCHAR(100)` | **UNIQUE, NOT NULL** | Mã quyền hạn chuẩn hóa |
| `name` | `VARCHAR(150)` | **NOT NULL** | Tên quyền hiển thị |
| `module` | `VARCHAR(50)` | **NOT NULL** | Phân hệ nghiệp vụ (`IAM`, `DMS`, `WORKFLOW`, `AUDIT`) |
| `description` | `TEXT` | | Mô tả chi tiết hành động được phép |
| `created_at` | `TIMESTAMPTZ` | **NOT NULL** | Thời điểm khởi tạo |

##### 5. `user_roles` (Bảng gán vai trò người dùng)
Liên kết Nhiều-Nhiều (N-N) giữa tài khoản người dùng và vai trò.

| Cột | Kiểu | Ràng Buộc | Ý Nghĩa Nghiệp Vụ |
| :--- | :--- | :--- | :--- |
| `user_id` | `BIGINT` | **PK, FK** $\rightarrow$ `users(id)` | ID người dùng được gán vai trò |
| `role_id` | `BIGINT` | **PK, FK** $\rightarrow$ `roles(id)` | ID vai trò được gán |
| `assigned_at` | `TIMESTAMPTZ` | **NOT NULL** | Thời điểm gán vai trò |

##### 6. `role_permissions` (Bảng gán quyền cho vai trò)
Liên kết Nhiều-Nhiều (N-N) giữa vai trò và các quyền hạn chức năng.

| Cột | Kiểu | Ràng Buộc | Ý Nghĩa Nghiệp Vụ |
| :--- | :--- | :--- | :--- |
| `role_id` | `BIGINT` | **PK, FK** $\rightarrow$ `roles(id)` | ID vai trò |
| `permission_id` | `BIGINT` | **PK, FK** $\rightarrow$ `permissions(id)` | ID quyền hạn cấp cho vai trò |
| `granted_at` | `TIMESTAMPTZ` | **NOT NULL** | Thời điểm cấp quyền |

##### 7. `refresh_tokens` (Phiên làm việc JWT)
Quản lý vòng đời token làm mới, hỗ trợ cơ chế thu hồi phiên đăng nhập tức thì.

| Cột | Kiểu | Ràng Buộc | Ý Nghĩa Nghiệp Vụ |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | **PK** | Định danh bản ghi phiên |
| `user_id` | `BIGINT` | **FK** $\rightarrow$ `users(id)` | Người dùng sở hữu phiên |
| `token` | `VARCHAR(512)` | **UNIQUE, NOT NULL** | Chuỗi Refresh Token ngẫu nhiên bảo mật |
| `expiry_date` | `TIMESTAMPTZ` | **NOT NULL** | Thời điểm hết hạn phiên |
| `revoked` | `BOOLEAN` | **NOT NULL, DEFAULT FALSE** | Trạng thái bị thu hồi (đăng xuất/bị khóa) |
| `created_at` | `TIMESTAMPTZ` | **NOT NULL** | Thời điểm phát hành token |

---

#### B. Phân Hệ Quản Lý Tài Liệu & AWS S3 (`Phase1_Document_Management`)

##### 8. `documents` (Tài liệu trung tâm & Con trỏ AWS S3)
Quản lý metadata tài liệu, định danh S3 Object Key, trạng thái toàn vẹn và mức phân loại bảo mật.

| Cột | Kiểu | Ràng Buộc | Ý Nghĩa Nghiệp Vụ |
| :--- | :--- | :--- | :--- |
| `id` | `VARCHAR(64)` | **PK** | Định danh tài liệu duy nhất (UUIDv4) |
| `original_file_name` | `VARCHAR(255)` | **NOT NULL** | Tên tệp gốc kèm phần mở rộng |
| `title` | `VARCHAR(255)` | **NOT NULL** | Tiêu đề nghiệp vụ của tài liệu |
| `description` | `TEXT` | | Tóm tắt hoặc mô tả nội dung tài liệu |
| `file_type` | `Enum` | **NOT NULL** | `PDF`, `DOCX`, `TXT`, `XLSX`, `IMAGE`, `OTHER` |
| `mime_type` | `VARCHAR(100)` | **NOT NULL** | Chuỗi định dạng MIME chuẩn (e.g. `application/pdf`) |
| `file_size_bytes` | `BIGINT` | **NOT NULL** | Kích thước nhị phân chính xác (bytes) |
| `checksum_sha256` | `VARCHAR(64)` | **NOT NULL** | Mã băm SHA-256 chống giả mạo và hỗ trợ deduplication |
| `storage_bucket` | `VARCHAR(128)` | **NOT NULL** | Tên AWS S3 Bucket riêng tư (e.g. `docs-platform-prod`) |
| `storage_key` | `VARCHAR(512)` | **NOT NULL** | Khóa S3 Object Key: `{dept_code}/{doc_id}/{filename}` |
| `is_s3_synced` | `BOOLEAN` | **NOT NULL, DEFAULT TRUE** | Xác nhận tệp đã lưu trữ an toàn trên AWS S3 |
| `processing_status` | `Enum` | **NOT NULL, DEFAULT 'UPLOADED'** | `UPLOADED`, `PENDING`, `PARSING`, `CHUNKED`, `INDEXED`, `FAILED` |
| `current_version` | `INT` | **NOT NULL, DEFAULT 1** | Số hiệu phiên bản hiện hành |
| `department_id` | `BIGINT` | **FK** $\rightarrow$ `departments(id)` | Phòng ban chủ quản tài liệu |
| `uploaded_by_user_id`| `BIGINT` | **FK** $\rightarrow$ `users(id)` | Người tải lên tài liệu |
| `access_level` | `Enum` | **NOT NULL, DEFAULT 'INTERNAL'** | `PUBLIC`, `INTERNAL`, `RESTRICTED`, `CONFIDENTIAL` |
| `metadata` | `JSONB` | **NOT NULL, DEFAULT '{}'** | Metadata động (tác giả, số trang, tags...) |
| `created_at` / `updated_at` | `TIMESTAMPTZ` | **NOT NULL** | Dấu thời gian tạo và cập nhật |
| `deleted_at` | `TIMESTAMPTZ` | **INDEX** | Thời điểm xóa mềm (NULL nếu tài liệu còn hoạt động) |

##### 9. `document_versions` (Lịch sử các phiên bản tệp S3)
Lưu trữ ảnh chụp bất biến của mọi phiên bản tệp trên S3, cho phép truy nguyên hoặc khôi phục.

| Cột | Kiểu | Ràng Buộc | Ý Nghĩa Nghiệp Vụ |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | **PK** | Định danh bản ghi phiên bản |
| `document_id` | `VARCHAR(64)` | **FK** $\rightarrow$ `documents(id)` | Tài liệu cha sở hữu phiên bản |
| `version_number` | `INT` | **NOT NULL** | Số thứ tự phiên bản (1, 2, 3...) |
| `storage_bucket` | `VARCHAR(128)` | **NOT NULL** | Tên AWS S3 Bucket lưu trữ phiên bản này |
| `storage_key` | `VARCHAR(512)` | **NOT NULL** | Đường dẫn S3 Object Key cho phiên bản cụ thể |
| `file_size_bytes` | `BIGINT` | **NOT NULL** | Dung lượng tệp của phiên bản này |
| `checksum_sha256` | `VARCHAR(64)` | **NOT NULL** | Mã băm SHA-256 của tệp phiên bản |
| `is_s3_synced` | `BOOLEAN` | **NOT NULL, DEFAULT TRUE** | Xác nhận đồng bộ nhị phân lên S3 |
| `change_summary` | `VARCHAR(500)` | | Ghi chú tóm tắt nội dung thay đổi ở phiên bản này |
| `uploaded_by_user_id`| `BIGINT` | **FK** $\rightarrow$ `users(id)` | Người tải lên phiên bản này |
| `created_at` | `TIMESTAMPTZ` | **NOT NULL** | Thời điểm khởi tạo phiên bản |

##### 10. Ma Trận Phân Quyền Truy Cập Chi Tiết (Access Control Lists - ACLs)
Chuẩn hóa 3NF thành 3 bảng độc lập, loại bỏ hoàn toàn đa hình (Polymorphic anti-pattern):
- **`document_user_access`:** Cấp quyền cho từng người dùng cá nhân (`document_id`, `user_id`, `permission_level`).
- **`document_department_access`:** Cấp quyền cho toàn bộ nhân sự phòng ban (`document_id`, `department_id`, `permission_level`).
- **`document_role_access`:** Cấp quyền cho các tài khoản mang vai trò nhất định (`document_id`, `role_id`, `permission_level`).
*(Mức quyền `permission_level`: `VIEW` - đọc & tải tệp S3; `EDIT` - sửa metadata & tải phiên bản mới; `ADMIN` - toàn quyền & xóa mềm).*

---

#### C. Phân Hệ Tự Động Hóa Quy Trình (`Phase1_Workflow_Automation`)

##### 11. `workflows` (Mẫu quy trình xử lý tự động)
Định nghĩa kịch bản xử lý tự động khi phát sinh sự kiện tài liệu.

| Cột | Kiểu | Ràng Buộc | Ý Nghĩa Nghiệp Vụ |
| :--- | :--- | :--- | :--- |
| `id` | `VARCHAR(64)` | **PK** | Định danh quy trình (e.g. `wf_doc_indexing`) |
| `name` | `VARCHAR(255)` | **NOT NULL** | Tên hiển thị quy trình |
| `description` | `TEXT` | | Mô tả mục tiêu và luồng xử lý |
| `trigger_event` | `Enum` | **NOT NULL** | `ON_DOCUMENT_UPLOADED`, `ON_DOCUMENT_INDEXED`, `MANUAL_TRIGGER`, `SCHEDULED` |
| `is_active` | `BOOLEAN` | **NOT NULL, DEFAULT TRUE** | Cờ bật/tắt kích hoạt tự động |
| `config_schema` | `JSONB` | **NOT NULL, DEFAULT '{}'** | Cấu hình tham số các bước và điều kiện rẽ nhánh |
| `created_at` / `updated_at` | `TIMESTAMPTZ` | **NOT NULL** | Dấu thời gian tạo và cập nhật |

##### 12. `workflow_executions` (Lịch sử thực thi quy trình)
Ghi nhận tiến trình chạy thực tế, dữ liệu đầu vào và kết quả từng bước.

| Cột | Kiểu | Ràng Buộc | Ý Nghĩa Nghiệp Vụ |
| :--- | :--- | :--- | :--- |
| `id` | `VARCHAR(64)` | **PK** | Định danh phiên chạy quy trình |
| `workflow_id` | `VARCHAR(64)` | **FK** $\rightarrow$ `workflows(id)` | Quy trình mẫu được gọi |
| `document_id` | `VARCHAR(64)` | **FK** $\rightarrow$ `documents(id)` | Tài liệu mục tiêu đang xử lý |
| `status` | `Enum` | **NOT NULL, DEFAULT 'PENDING'** | `PENDING`, `RUNNING`, `WAITING_APPROVAL`, `COMPLETED`, `FAILED`, `CANCELLED` |
| `current_step` | `VARCHAR(100)` | | Tên bước đang thực thi hiện tại |
| `input_payload` | `JSONB` | **NOT NULL, DEFAULT '{}'** | Dữ liệu đầu vào của phiên chạy |
| `step_results` | `JSONB` | **NOT NULL, DEFAULT '{}'** | Dữ liệu đầu ra các bước đã hoàn tất |
| `error_message` | `TEXT` | | Chi tiết thông điệp lỗi nếu thất bại |
| `started_at` / `completed_at` | `TIMESTAMPTZ` | | Thời điểm bắt đầu và kết thúc |

---

#### D. Phân Hệ Tác Vụ Nghiệp Vụ & Phê Duyệt 2 Pha (`Phase1_Operations_HITL`)

##### 13. `action_approvals` (Chốt chặn phê duyệt Human-In-The-Loop 2 Pha)
Đảm bảo các hành động trọng yếu bắt buộc phải được người có thẩm quyền kiểm tra và xác nhận.

| Cột | Kiểu | Ràng Buộc | Ý Nghĩa Nghiệp Vụ |
| :--- | :--- | :--- | :--- |
| `id` | `VARCHAR(64)` | **PK** | Mã phiếu yêu cầu phê duyệt |
| `execution_id` | `VARCHAR(64)` | **FK** $\rightarrow$ `workflow_executions(id)` | Phiên workflow phát sinh yêu cầu |
| `action_type` | `VARCHAR(100)` | **NOT NULL** | Loại thao tác (e.g. `PUBLISH_DOCUMENT`, `PURGE_DOCUMENT`) |
| `status` | `Enum` | **NOT NULL, DEFAULT 'PENDING'** | `PENDING`, `APPROVED`, `REJECTED`, `COMMITTED`, `EXPIRED` |
| `idempotency_key` | `VARCHAR(128)` | **UNIQUE, NOT NULL** | Khóa chống thực thi trùng lặp giao dịch (Exactly-Once) |
| `preview_payload` | `JSONB` | **NOT NULL, DEFAULT '{}'** | Dữ liệu Diff Before/After hiển thị trực quan cho Manager |
| `execution_result`| `JSONB` | **NOT NULL, DEFAULT '{}'** | Kết quả sau khi commit hành động thành công |
| `requested_by_user_id`| `BIGINT` | **FK** $\rightarrow$ `users(id)` | Người khởi tạo yêu cầu thao tác |
| `reviewed_by_user_id` | `BIGINT` | **FK** $\rightarrow$ `users(id)` | Cấp quản lý thực hiện phê duyệt/từ chối |
| `review_notes` | `TEXT` | | Ý kiến nhận xét hoặc lý do bác bỏ bắt buộc |
| `requested_at` / `reviewed_at` / `committed_at` | `TIMESTAMPTZ` | | Dấu thời gian các mốc chuyển trạng thái 2 pha |

##### 14. `operation_tasks` (Tác vụ vận hành cần xử lý thủ công)
Quản lý các công việc nghiệp vụ phát sinh từ lỗi xử lý hoặc yêu cầu soát xét của nhân viên.

| Cột | Kiểu | Ràng Buộc | Ý Nghĩa Nghiệp Vụ |
| :--- | :--- | :--- | :--- |
| `id` | `VARCHAR(64)` | **PK** | Định danh tác vụ (UUID) |
| `title` | `VARCHAR(255)` | **NOT NULL** | Tiêu đề tóm tắt công việc |
| `description` | `TEXT` | | Hướng dẫn hoặc chi tiết yêu cầu |
| `status` | `Enum` | **NOT NULL, DEFAULT 'PENDING'** | `PENDING`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `priority` | `Enum` | **NOT NULL, DEFAULT 'MEDIUM'** | `LOW`, `MEDIUM`, `HIGH`, `URGENT` |
| `creator_id` | `BIGINT` | **FK** $\rightarrow$ `users(id)` | Người hoặc tiến trình tạo tác vụ |
| `assignee_id` | `BIGINT` | **FK** $\rightarrow$ `users(id)` | Nhân viên được giao trách nhiệm giải quyết |
| `document_id` | `VARCHAR(64)` | **FK** $\rightarrow$ `documents(id)` | Tài liệu liên quan trực tiếp |
| `execution_id` | `VARCHAR(64)` | **FK** $\rightarrow$ `workflow_executions(id)` | Phiên workflow liên quan |
| `metadata` | `JSONB` | **NOT NULL, DEFAULT '{}'** | Dữ liệu mở rộng (hạn chót SLA, nhãn phân loại) |
| `created_at` / `updated_at` | `TIMESTAMPTZ` | **NOT NULL** | Dấu thời gian khởi tạo và cập nhật |

---

#### E. Phân Hệ Kiểm Toán Bất Biến & Thông Báo (`Phase1_Audit_System`)

##### 15. `audit_logs` (Nhật ký kiểm toán an toàn thông tin)
Ghi nhận chuỗi nhật ký kiểm toán bất biến (Append-Only) phục vụ giám sát tuân thủ và điều tra số.

| Cột | Kiểu | Ràng Buộc | Ý Nghĩa Nghiệp Vụ |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | **PK** | Mã tăng tự động bản ghi log |
| `user_id` | `BIGINT` | **FK** $\rightarrow$ `users(id)` | Người thực hiện hành động (NULL nếu là system daemon) |
| `action` | `VARCHAR(100)` | **NOT NULL** | Động từ hành động (`AUTH_LOGIN`, `DOC_UPLOAD`, `DOC_DOWNLOAD_S3`) |
| `resource_type`| `VARCHAR(100)` | **NOT NULL** | Phân loại tài nguyên (`DOCUMENT`, `USER`, `APPROVAL`, `S3_OBJECT`) |
| `resource_id` | `VARCHAR(64)` | | ID định danh tài nguyên bị tác động |
| `ip_address` | `VARCHAR(45)` | | Địa chỉ IP máy khách (IPv4/IPv6) tại Edge Ingress |
| `user_agent` | `TEXT` | | Thông tin trình duyệt/thiết bị phục vụ pháp y số |
| `status` | `Enum` | **NOT NULL, DEFAULT 'SUCCESS'** | `SUCCESS`, `FAILED` |
| `details` | `JSONB` | **NOT NULL, DEFAULT '{}'** | Payload bất biến (tham số yêu cầu, lỗi, thay đổi diff) |
| `created_at` | `TIMESTAMPTZ` | **NOT NULL** | Thời điểm ghi log (nghiêm cấm sửa hoặc xóa) |

##### 16. `notifications` (Thông báo trong ứng dụng)
Thông báo trạng thái công việc và nhắc nhở phê duyệt tới người dùng.

| Cột | Kiểu | Ràng Buộc | Ý Nghĩa Nghiệp Vụ |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | **PK** | Định danh thông báo |
| `user_id` | `BIGINT` | **FK** $\rightarrow$ `users(id)` | Người dùng nhận thông báo |
| `title` | `VARCHAR(255)` | **NOT NULL** | Tiêu đề thông báo ngắn gọn |
| `message` | `TEXT` | **NOT NULL** | Nội dung chi tiết thông báo |
| `type` | `Enum` | **NOT NULL, DEFAULT 'INFO'** | `INFO`, `SUCCESS`, `WARNING`, `ACTION_REQUIRED` |
| `is_read` | `BOOLEAN` | **NOT NULL, DEFAULT FALSE** | Cờ trạng thái đã đọc |
| `reference_type`| `VARCHAR(100)` | | Loại đối tượng liên quan (`DOCUMENT`, `APPROVAL`, `TASK`) |
| `reference_id` | `VARCHAR(64)` | | ID đối tượng để điều hướng UI nhanh |
| `created_at` | `TIMESTAMPTZ` | **NOT NULL** | Thời điểm gửi thông báo |

---

### 2.2. Nhóm Phase 2: Pluggable AI Knowledge & RAG Extension

*(Các thực thể phục vụ phân đoạn văn bản, lưu trữ vector và hội thoại thông minh khi cắm Microservice AI vào hệ thống).*

##### 17. `document_chunks` (Phân đoạn văn bản & Chỉ mục Vector pgvector)
Lưu trữ các đoạn văn bản trích xuất (500–1000 tokens) kèm vector embedding 1536 chiều và dữ liệu FTS.

| Cột | Kiểu | Ràng Buộc | Ý Nghĩa Nghiệp Vụ |
| :--- | :--- | :--- | :--- |
| `id` | `VARCHAR(64)` | **PK** | Mã chunk duy nhất (`{doc_id}_v{ver}_c{idx}`) |
| `document_id` | `VARCHAR(64)` | **FK** $\rightarrow$ `documents(id)` | Tài liệu gốc |
| `document_version_id`| `BIGINT` | **FK** $\rightarrow$ `document_versions(id)` | Phiên bản tài liệu cụ thể của đoạn |
| `chunk_index` | `INT` | **NOT NULL, DEFAULT 0** | Số thứ tự đoạn trong tài liệu |
| `page_number` | `INT` | | Số trang gốc phục vụ đối chiếu trích dẫn |
| `content` | `TEXT` | **NOT NULL** | Nội dung văn bản trích xuất thô đã chuẩn hóa |
| `token_count` | `INT` | **NOT NULL, DEFAULT 0** | Số lượng token tính bằng tokenizer |
| `metadata` | `JSONB` | **GIN INDEX** | Vị trí trang, tiêu đề mục, bounding box |
| `embedding` | `vector(1536)` | **HNSW INDEX (`cosine`)** | Vector nhúng ngữ nghĩa phục vụ tìm kiếm tương đồng |
| `tsv` | `tsvector` | **GIN INDEX** | Dữ liệu tìm kiếm toàn văn Full-Text Search (FTS) |
| `created_at` | `TIMESTAMPTZ` | **NOT NULL** | Thời điểm sinh đoạn văn bản |

##### 18. `conversations` (Phiên hội thoại hỏi đáp)
Quản lý luồng tương tác và ngữ cảnh trao đổi giữa người dùng và AI Assistant.

| Cột | Kiểu | Ràng Buộc | Ý Nghĩa Nghiệp Vụ |
| :--- | :--- | :--- | :--- |
| `id` | `VARCHAR(64)` | **PK** | Định danh phiên chat |
| `user_id` | `BIGINT` | **FK** $\rightarrow$ `users(id)` | Chủ sở hữu phiên trao đổi |
| `title` | `VARCHAR(255)` | **NOT NULL, DEFAULT 'New Conversation'** | Tiêu đề phiên trò chuyện |
| `status` | `Enum` | **NOT NULL, DEFAULT 'ACTIVE'** | `ACTIVE`, `ARCHIVED`, `DELETED` |
| `created_at` / `updated_at` | `TIMESTAMPTZ` | **NOT NULL** | Thời điểm tạo và tương tác gần nhất |

##### 19. `conversation_messages` (Tin nhắn trong phiên trò chuyện)
Lịch sử tin nhắn, số lượng token tiêu thụ và đánh giá độ tin cậy câu trả lời.

| Cột | Kiểu | Ràng Buộc | Ý Nghĩa Nghiệp Vụ |
| :--- | :--- | :--- | :--- |
| `id` | `VARCHAR(64)` | **PK** | Định danh tin nhắn duy nhất |
| `conversation_id` | `VARCHAR(64)` | **FK** $\rightarrow$ `conversations(id)` | Phiên hội thoại cha |
| `role` | `Enum` | **NOT NULL** | `USER`, `ASSISTANT`, `SYSTEM`, `TOOL` |
| `content` | `TEXT` | **NOT NULL** | Nội dung văn bản tin nhắn |
| `confidence` | `Enum` | | Độ tin cậy AI: `LOW` (< 0.60 Safe Abstention), `MEDIUM`, `HIGH` |
| `prompt_tokens` / `completion_tokens` | `INT` | **NOT NULL, DEFAULT 0** | Lượng token ngữ cảnh và sinh câu trả lời |
| `created_at` | `TIMESTAMPTZ` | **NOT NULL** | Thời điểm gửi tin nhắn |

##### 20. `message_citations` (Bằng chứng trích dẫn nguồn cho câu trả lời)
Đối chiếu minh bạch câu trả lời của AI với đoạn văn bản và tài liệu gốc.

| Cột | Kiểu | Ràng Buộc | Ý Nghĩa Nghiệp Vụ |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | **PK** | Định danh bản ghi trích dẫn |
| `message_id` | `VARCHAR(64)` | **FK** $\rightarrow$ `conversation_messages(id)` | Tin nhắn AI chứa câu trả lời |
| `document_id` | `VARCHAR(64)` | **FK** $\rightarrow$ `documents(id)` | Tài liệu nguồn được trích dẫn |
| `chunk_id` | `VARCHAR(64)` | **FK** $\rightarrow$ `document_chunks(id)` | Đoạn chunk văn bản cụ thể chứng minh luận điểm |
| `relevance_score` | `FLOAT` | **NOT NULL, DEFAULT 0.0** | Điểm số tương đồng ngữ nghĩa / thứ hạng RRF |
| `snippet` | `TEXT` | | Đoạn trích dẫn nguyên văn hiển thị trên giao diện |
| `created_at` | `TIMESTAMPTZ` | **NOT NULL** | Thời điểm ghi nhận trích dẫn |
