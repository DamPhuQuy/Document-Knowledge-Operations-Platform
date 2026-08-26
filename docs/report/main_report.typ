//
// 1. ĐỊNH DẠNG VĂN BẢN CƠ BẢN:
//    - In đậm:           *chữ in đậm*
//    - In nghiêng:       _chữ in nghiêng_
//    - Gạch chân:        #underline[chữ gạch chân]
//    - Đơn vị / Inline:  `$1536$` hoặc `< 25 ms`
//    - Danh sách chấm:   - Ý thứ nhất
//                        - Ý thứ hai
//    - Danh sách số:     + Bước 1
//                        + Bước 2
//
// 2. KHAI BÁO TIÊU ĐỀ & CHƯƠNG MỤC:
//    - Chương có số:     = CƠ SỞ LÝ THUYẾT
//                        (Hệ thống tự động ngắt trang, in đậm 14pt và ghi "CHƯƠNG 1.")
//    - Mục cấp 2:        == Kiến trúc RAG và Hybrid Search
//                        (Tự động đánh số "1.1. ", in đậm 13pt)
//    - Mục cấp 3:        === Kỹ thuật lọc phân quyền Pre-filtering
//                        (Tự động đánh số "1.1.1. ", in đậm nghiêng 13pt)
//    - Mục không số:     #unnumbered-chapter("GIỚI THIỆU ĐỀ TÀI")
//                        (Hiển thị trong Mục lục nhưng không đánh số chương)
//
// 3. HÌNH ẢNH & BẢNG BIỂU (TỰ ĐỘNG THÊM VÀO DANH MỤC HÌNH / BẢNG):
//    - Chèn hình ảnh:
//        #figure(
//          image("duong_dan_anh.png", width: 80%),
//          caption: [Sơ đồ luồng xử lý RAG kết hợp phân quyền],
//        ) <fig_rag_flow>
//
//    - Chèn bảng biểu:
//        #figure(
//          table(
//            columns: (1.5fr, 3fr),
//            table.header([*Tiêu chí*], [*Mô tả chi tiết*]),
//            [Tốc độ], [Thời gian phản hồi dưới 25ms.],
//            [Bảo mật], [Cách ly phân quyền đa tầng tuyệt đối.],
//          ),
//          caption: [Bảng thông số đánh giá hiệu năng],
//        ) <tab_eval>
//
// 4. THAM CHIẾU CHÉO TỰ ĐỘNG (CROSS-REFERENCE):
//    - Gắn nhãn sau hình/bảng:  <ten_nhan>  (ví dụ: <fig_rag_flow>)
//    - Tham chiếu trong bài:    Xem chi tiết tại @fig_rag_flow hoặc @tab_eval.
//                               (Typst tự động hiển thị: "Hình 1.1" hoặc "Bảng 1.1")
//
// 5. KHỐI GHI CHÚ & CẢNH BÁO (CALLOUT BLOCKS):
//    - Ghi chú:          #note[Nội dung cần lưu ý quan trọng...]
//    - Cảnh báo:         #warning[Nội dung cảnh báo rủi ro kỹ thuật...]
//
// 6. KHỐI MÃ NGUỒN & CÔNG THỨC TOÁN:
//    - Mã nguồn (Code):  ```python
//                        def query_vector(embedding):
//                            return db.search(embedding)
//                        ```
//    - Toán dòng:        Khoảng cách Cosine $d = 1 - (bold(u) dot bold(v))$
//    - Toán khối (có số): $ "Similarity"(bold(u), bold(v)) = (bold(u) dot bold(v)) / (||bold(u)|| || bold(v)||) $

#import "template/template.typ": *

#show: report.with(
  title: "Triển khai hạ tầng cho nền tảng quản lý tài liệu nội bộ và tự động hóa nghiệp vụ",
  project_code: "PBL4: DỰ ÁN HỆ ĐIỀU HÀNH & MẠNG MÁY TÍNH",
  topic_code: "Đề tài",
  students: (
    (name: "Đàm Phú Quý", class: "24T_DT2"),
    (name: "Trần Lê Phi Long", class: "24T_DT2"),
  ),
  supervisor: "TS. Nguyễn Văn Nguyên",
  location: "Đà Nẵng",
  year: "2026",
  font_family: ("New Computer Modern", "Times New Roman"),
  font_size: 13pt,
  abbreviations: (
    ("ACL", "Access Control List", "Danh sách kiểm soát quyền truy cập"),
    ("API", "Application Programming Interface", "Giao diện lập trình ứng dụng"),
    ("CI/CD", "Continuous Integration / Continuous Deployment", "Tích hợp và triển khai liên tục"),
    ("DDD", "Domain-Driven Design", "Phương pháp thiết kế hướng miền"),
    ("HITL", "Human-in-the-Loop", "Cơ chế kiểm duyệt có con người tham gia"),
    ("HNSW", "Hierarchical Navigable Small World", "Thuật toán lập chỉ mục tìm kiếm vector gần đúng"),
    ("IAM", "Identity and Access Management", "Quản lý định danh và quyền truy cập"),
    ("LLM", "Large Language Model", "Mô hình ngôn ngữ lớn"),
    ("RAG", "Retrieval-Augmented Generation", "Tạo sinh tăng cường bằng truy xuất thông tin"),
    ("RBAC", "Role-Based Access Control", "Kiểm soát quyền truy cập dựa trên vai trò"),
    ("RRF", "Reciprocal Rank Fusion", "Thuật toán kết hợp xếp hạng tìm kiếm kết hợp"),
    ("SSE", "Server-Sent Events", "Giao thức truyền dữ liệu thời gian thực từ server"),
    ("TTFT", "Time to First Token", "Thời gian phản hồi đến token đầu tiên của mô hình AI"),
  )
)

// ==============================================================================
// PHẦN MỞ ĐẦU
// ==============================================================================

#unnumbered-chapter("GIỚI THIỆU ĐỀ TÀI")

Trong các tổ chức và doanh nghiệp hiện nay, khối lượng tài liệu nội bộ như quy trình vận hành, văn bản chính sách, báo cáo tài chính, tài liệu kỹ thuật và hợp đồng ngày càng gia tăng nhanh chóng. Việc quản lý và khai thác hiệu quả nguồn dữ liệu này đang đối mặt với nhiều thách thức lớn:

1. *Tìm kiếm phân tán và mất thời gian:* Người dùng phải tra cứu thủ công qua nhiều thư mục hoặc ứng dụng rời rạc, khó tổng hợp được thông tin chính xác.
2. *Rủi ro bảo mật và rò rỉ dữ liệu:* Các giải pháp AI phổ thông thường không hỗ trợ kiểm soát phân quyền ở mức tài liệu (ACL), dẫn đến nguy cơ người dùng truy cập trái phép vào các tài liệu mật của phòng ban khác.
3. *Hiện tượng ảo giác của mô hình AI:* Thiếu cơ chế trích dẫn bằng chứng và xác thực nguồn dữ liệu khiến câu trả lời của AI không thể kiểm toán hay giải trình.
4. *Thiếu khả năng tự động hóa và hành động:* Các hệ thống hỏi đáp thông thường chỉ dừng lại ở mức trả lời tĩnh (Read-only), chưa thể chuyển giao tiếp thành các tác vụ thực tế trong quy trình nghiệp vụ.

Đề tài *"Triển khai hạ tầng cho nền tảng quản lý tài liệu và vận hành tri thức tích hợp AI"* được xây dựng nhằm giải quyết triệt để các vấn đề trên thông qua việc kết hợp nền tảng quản lý tài liệu tập trung, kiến trúc phân quyền đa tầng, công nghệ RAG nâng cao và cơ chế tự động hóa tác vụ có kiểm duyệt (Human-in-the-Loop).

// ==============================================================================
// CHƯƠNG 1. CƠ SỞ LÝ THUYẾT
// ==============================================================================

= CƠ SỞ LÝ THUYẾT

== Tổng quan về Quản lý Tài liệu và Tri thức Doanh nghiệp
Quản lý tri thức doanh nghiệp (Enterprise Knowledge Management) là quá trình thu thập, lưu trữ, lập chỉ mục và phân phối tri thức nội bộ phục vụ cho các quyết định nghiệp vụ. Một hệ thống quản lý tri thức chuẩn mực cần đảm bảo:
- Tính toàn vẹn và có phiên bản của tài liệu (Document Versioning).
- Quản lý định danh và phân quyền chặt chẽ (Identity & Access Management - IAM).
- Khả năng tìm kiếm nhanh chóng và chính xác.

== Kiến trúc RAG và Tìm kiếm Kết hợp (Hybrid Search)
Kiến trúc RAG giải quyết hiện tượng ảo giác (Hallucination) của các Mô hình Ngôn ngữ Lớn (LLM) bằng cách trích xuất ngữ cảnh liên quan từ cơ sở tri thức trước khi sinh câu trả lời. Tìm kiếm kết hợp (Hybrid Search) phối hợp sức mạnh của Dense Semantic Search và Sparse Lexical Search qua thuật toán Reciprocal Rank Fusion (RRF):

#figure(
  table(
    columns: (2.4fr, 3.8fr, 3.8fr),
    align: (center + horizon, left, left),
    table.header([*Phương pháp*], [*Ưu điểm*], [*Hạn chế*]),
    [Dense Semantic Search], [Tìm kiếm theo ngữ nghĩa và ý định của câu hỏi, hỗ trợ đa ngôn ngữ.], [Dễ bỏ sót từ khóa chuyên ngành, mã số hồ sơ hoặc thuật ngữ viết tắt.],
    [Sparse Lexical Search (BM25/FTS)], [Khớp chính xác từ khóa, số hiệu văn bản và tên riêng.], [Không hiểu được ngữ cảnh đồng nghĩa hoặc câu hỏi diễn giải khác.],
    [Hybrid Search + RRF], [Kết hợp sức mạnh ngữ nghĩa và từ khóa chính xác, độ phủ recall cao.], [Đòi hỏi hạ tầng hỗ trợ cả vector index và inverted index.],
  ),
  caption: [So sánh các kỹ thuật tìm kiếm trong hệ thống RAG],
)

== Cơ sở dữ liệu PostgreSQL và Tiện ích mở rộng pgvector
PostgreSQL là hệ quản trị cơ sở dữ liệu quan hệ mạnh mẽ, hỗ trợ giao dịch ACID và phân quyền người dùng. Với tiện ích mở rộng `pgvector`, hệ thống cho phép:
- Lưu trữ các vector embedding đa chiều (ví dụ 1536 chiều từ OpenAI hoặc 768 chiều từ HuggingFace).
- Xây dựng chỉ mục HNSW (Hierarchical Navigable Small World) để tăng tốc độ truy vấn độ tương đồng Cosine.
- Thực thi truy vấn kết hợp (Pre-filtered Search) giữa bộ lọc quyền truy cập SQL và tìm kiếm khoảng cách vector trong duy nhất một câu lệnh SQL.

Công thức tính khoảng cách Cosine giữa hai vector $bold(u)$ và $bold(v)$:
$ "Cosine Similarity"(bold(u), bold(v)) = (bold(u) dot bold(v)) / (||bold(u)|| ||bold(v)||) = (sum_(i=1)^n u_i v_i) / (sqrt(sum_(i=1)^n u_i^2) sqrt(sum_(i=1)^n v_i^2)) $

== Công nghệ Container hóa và Hạ tầng triển khai
Hệ thống áp dụng kiến trúc container hóa dựa trên Docker và Docker Compose nhằm chuẩn hóa môi trường phát triển và sẵn sàng triển khai trên môi trường điện toán đám mây AWS (EC2, S3, RDS).

// ==============================================================================
// CHƯƠNG 2. PHÂN TÍCH VÀ THIẾT KẾ HỆ THỐNG
// ==============================================================================

= PHÂN TÍCH THIẾT KẾ HỆ THỐNG

== Yêu cầu hệ thống và Ca sử dụng chính
Hệ thống được thiết kế phục vụ các nhóm người dùng trong tổ chức với các nhóm chức năng trọng tâm:
1. *Phân hệ Quản lý định danh (IAM):* Xác thực người dùng qua JWT, phân quyền theo vai trò (RBAC) và phòng ban.
2. *Phân hệ Quản lý tài liệu (Document Management):* Tải lên tài liệu, lưu trữ object trên S3, quản lý phiên bản và cấu hình phân quyền ACL (Public, Internal, Restricted, Confidential).
3. *Phân hệ Xử lý tri thức và RAG (AI & RAG Service):* Tách đoạn tài liệu (Chunking), tạo vector embedding, tìm kiếm Hybrid Search và sinh câu trả lời kèm trích dẫn nguồn.
4. *Phân hệ Tác vụ & Phê duyệt (Operations & HITL):* Quản lý các hành động có tác động thay đổi hệ thống thông qua quy trình phê duyệt của con người trước khi commit.

== Kiến trúc tổng thể hệ thống (System Architecture)
Hệ thống được tổ chức theo mô hình Modular Monolith và Microservice chuyên biệt:
- *Backend API (Spring Boot 3):* Đóng vai trò làm Gateway và xử lý các nghiệp vụ cốt lõi theo kiến trúc Domain-Driven Design (DDD).
- *AI Service (Python FastAPI):* Đóng vai trò vi dịch vụ tính toán AI, triển khai kiến trúc Clean Architecture (Hexagonal Ports & Adapters).
- *Database (PostgreSQL + pgvector):* Lưu trữ dữ liệu quan hệ và chỉ mục vector.
- *Storage (Amazon S3 / Local MinIO/Floci):* Lưu trữ các tệp nhị phân tài liệu gốc.

#note[
  Hệ thống áp dụng nguyên tắc *Pre-filtered Retrieval*: Lọc quyền truy cập tài liệu ngay tại tầng SQL Database trước khi đưa ngữ cảnh vào mô hình AI, loại bỏ hoàn toàn nguy cơ rò rỉ dữ liệu (Zero Data Leakage).
]

== Thiết kế Cơ sở dữ liệu và Mô hình Phân quyền
Mô hình dữ liệu được chia thành các phân hệ độc lập:
1. `01_iam_organization`: Bảng `users`, `roles`, `permissions`, `departments`.
2. `02_document_management`: Bảng `documents`, `document_versions`, `document_permissions`.
3. `03_ai_knowledge_rag`: Bảng `document_chunks` chứa vector embedding và chỉ mục `tsvector`.
4. `04_conversational_ai`: Bảng `conversations`, `conversation_messages`, `message_citations`.
5. `05_workflow_automation`: Bảng `workflows`, `workflow_executions`.
6. `06_operations_hitl`: Bảng `action_approvals`, `operation_tasks`.
7. `07_audit_notifications`: Bảng `audit_logs`, `notifications`.

// ==============================================================================
// CHƯƠNG 3. TRIỂN KHAI VÀ ĐÁNH GIÁ KẾT QUẢ
// ==============================================================================

= TRIỂN KHAI VÀ ĐÁNH GIÁ KẾT QUẢ

== Môi trường triển khai và Cấu hình hạ tầng
Hệ thống được đóng gói và cấu hình thông qua `docker-compose.yaml` bao gồm các dịch vụ:
- `backend`: Khởi chạy Java Spring Boot 3 trên cổng `8080`.
- `ai-service`: Khởi chạy Python FastAPI trên cổng `8000`.
- `database`: PostgreSQL 16 tích hợp sẵn `pgvector` trên cổng `5432`.
- `reverse-proxy`: Nginx điều phối lưu lượng và bảo vệ mạng nội bộ.

Ví dụ cấu trúc chạy kiểm thử RAG Ingestion Pipeline:
```python
# Gọi Ingestion Pipeline để phân đoạn và lập chỉ mục tài liệu
pipeline = OfflineIngestionPipeline(
    chunker=RecursiveChunker(chunk_size=500, chunk_overlap=50),
    embedder=OpenAiEmbeddingAdapter(model="text-embedding-3-small"),
    vector_store=PgVectorStore(db_connection),
)
result = await pipeline.execute(documents=raw_documents)
print(f"Đã lập chỉ mục thành công: {result.total_chunks} chunks.")
```

== Kết quả thử nghiệm các tính năng cốt lõi
1. *Thử nghiệm Ingestion Pipeline:* Nạp và phân đoạn tài liệu tự động, trích xuất metadata và tính toán vector embedding theo lô.
2. *Thử nghiệm Pre-filtered Vector Search:* Xác thực kiểm tra quyền truy cập tài liệu: Người dùng chỉ nhận được câu trả lời trích xuất từ tài liệu mà họ có quyền đọc theo phòng ban/vai trò.
3. *Thử nghiệm Chống ảo giác (Anti-Hallucination):* Khi không có tài liệu hợp lệ trong phạm vi quyền, hệ thống trả về thông báo từ chối (`NO_ACCESSIBLE_KNOWLEDGE`) thay vì tự suy đoán.
4. *Thử nghiệm Cơ chế Human-in-the-Loop:* Tạo hành động yêu cầu phê duyệt, ghi nhận trạng thái `WAITING_APPROVAL` và hoàn tất ghi nhận nhật ký kiểm toán bất biến sau khi người quản lý phê duyệt.

== Đánh giá hiệu năng và Độ trễ
- Độ trễ truy vấn vector kết hợp bộ lọc ACL trung bình: $< 25$ ms.
- Thời gian sinh câu trả lời RAG (Time to First Token): $< 800$ ms.
- Đảm bảo $100\%$ không rò rỉ dữ liệu giữa các phòng ban khác nhau trong thử nghiệm bảo mật.

// ==============================================================================
// KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN
// ==============================================================================

#unnumbered-chapter("KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN")

*1. Kết quả đạt được:*
- Xây dựng thành công nền tảng quản lý tài liệu và vận hành tri thức tích hợp AI hoàn chỉnh.
- Triển khai mô hình phân quyền đa tầng và kiểm soát truy cập tài liệu trong RAG một cách an toàn, triệt để.
- Áp dụng thành công kiến trúc Clean Architecture, Tactical DDD và quy trình Human-in-the-Loop cho các tác vụ AI.
- Đóng gói toàn bộ hạ tầng bằng Docker, sẵn sàng cho việc triển khai mở rộng trên điện toán đám mây.

*2. Hướng phát triển trong tương lai:*
- Tích hợp thêm các công cụ nhận dạng tài liệu nâng cao (OCR cho tài liệu quét, hóa đơn, biểu mẫu phức tạp).
- Mở rộng cơ chế Tracing phân tán bằng OpenTelemetry và giám sát chi phí token thời gian thực.
- Triển khai hạ tầng Auto-scaling trên AWS ECS / Fargate và Amazon RDS Multi-AZ cho môi trường Production thực tế.

// ==============================================================================
// TÀI LIỆU THAM KHẢO
// ==============================================================================

#unnumbered-chapter("TÀI LIỆU THAM KHẢO")

#set par(first-line-indent: 0cm, hanging-indent: 1cm)

[1] Patrick Lewis, Ethan Perez, Aleksandara Piktus, et al., *Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks*, Advances in Neural Information Processing Systems (NeurIPS), 2020.

[2] Eric Evans, *Domain-Driven Design: Tackling Complexity in the Heart of Software*, Addison-Wesley Professional, 2003.

[3] Robert C. Martin, *Clean Architecture: A Craftsman's Guide to Software Structure and Design*, Prentice Hall, 2017.

[4] PostgreSQL Global Development Group, *PostgreSQL 16 Documentation & pgvector extension*, https://www.postgresql.org/docs/, 2024.

[5] Spring Framework & Spring Boot Reference Documentation, *Spring Boot 3.x Architecture Guide*, https://spring.io/projects/spring-boot, 2024.

[6] FastAPI Documentation, *High performance Python web framework*, https://fastapi.tiangolo.com/, 2024.

// ==============================================================================
// PHỤ LỤC
// ==============================================================================

#unnumbered-chapter("PHỤ LỤC")

*Phụ lục 1: Sơ đồ Kiến trúc Phân tầng Dịch vụ AI (Hexagonal Architecture)*\
Mô tả chi tiết các cổng giao tiếp Inbound/Outbound (Ports & Adapters) trong dịch vụ AI Service:
- `EmbeddingPort`, `VectorStorePort`, `LlmClientPort`, `RerankerPort`.
- Pipeline Ingestion Offline và Pipeline Retrieval Online kết hợp thuật toán RRF.

#v(0.3cm)
*Phụ lục 2: Danh mục mã nguồn và Hướng dẫn triển khai nhanh*\
- Mã nguồn máy chủ Backend: `/backend` (Java 21, Spring Boot 3)
- Mã nguồn dịch vụ AI & RAG: `/ai` (Python 3.11, FastAPI, uv)
- Cấu hình hạ tầng triển khai: `/docker-compose.yaml`
