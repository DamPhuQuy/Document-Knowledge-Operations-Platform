# Architecture

## Kiến trúc đã áp dụng

```text
                                        +-----------------------------------------+
                                        |         com.platform.app                |
                                        +-----------------------------------------+
                                                              |
             +------------------------+-----------------------+----------------------+--------------------------+
             |                        |                                              |                          |
             v                        v                                              v                          v
    +------------------+    +--------------------+                        +--------------------+    +--------------------+
    |      shared      |    |       system       |                        |        iam         |    |  module_template   |
    |  (Shared Kernel  |    | (System Diag /     |                        | (Identity & Access |    |   (Core Domain:    |
    |  & Cross-Cutting)|    |  Lean Capability)  |                        |  Lean Capability)  |    | Tactical DDD Arch) |
    +------------------+    +--------------------+                        +--------------------+    +--------------------+
                                                                                                                |
                                                    +----------------------------+------------------------------+----------------------------+
                                                    |                            |                              |                            |
                                                    v                            v                              v                            v
                                           +-----------------+          +-----------------+            +-----------------+          +-----------------+
                                           |     domain      |          |   application   |            | infrastructure  |          |       api       |
                                           | (Aggregate Root,|          | (Use Cases,     |            | (Persistence &  |          | (REST Adapter,  |
                                           |  VOs, Events)   |          |  App Services)  |            |  Outbound Ports)|          |  Web DTOs)      |
                                           +-----------------+          +-----------------+            +-----------------+          +-----------------+
```

---

### 1. Package by Business Capability (Package cấp cao nhất)

Thay vì chia theo các tầng kỹ thuật ngang (controllers, services, repositories) cho toàn dự án, package cấp 1 đại diện cho các Business Capabilities / Bounded Contexts:

- **shared**: Shared Kernel chứa các thành phần dùng chung (`BaseEntity`, `AggregateRoot`, `DomainEvent`, `ApiResponse`, `GlobalExceptionHandler`, `SecurityConfig`, `JwtTokenProvider`,...).
- **system**: Capability chẩn đoán và kiểm tra sức khỏe hệ thống (`/api/v1/health`).
- **iam**: Capability quản lý định danh & phân quyền (Identity & Access Management - đăng ký, đăng nhập, JWT tokens, profile người dùng).
- **ai**: Capability tích hợp dịch vụ AI Assistant & RAG (Microservice giao tiếp qua REST API / HTTP Client).
- **module_template**: Archetype chuẩn mẫu đại diện cho Core Domain phức tạp, định hình cấu trúc Tactical Domain-Driven Design (DDD) và Clean Architecture / Hexagonal Architecture.

---

### 2. Selective Tactical DDD (Áp dụng DDD chiến thuật có chọn lọc)

| Business Capability | Độ phức tạp | Mô hình áp dụng | Lý do thiết kế |
| :--- | :--- | :--- | :--- |
| **system** | Rất thấp | Thin / Pragmatic Controller | Chỉ kiểm tra uptime và trạng thái service, không có logic nghiệp vụ. |
| **iam** | Trung bình - thấp | Pragmatic Service + Repository | Nghiệp vụ xác thực & CRUD tài khoản cơ bản, không cần Aggregate Root phức tạp để tránh over-engineering. |
| **module_template** | Cao (Core Domain) | Full Tactical DDD & Ports/Adapters | Mẫu chuẩn cho nghiệp vụ trung tâm có nhiều ràng buộc (invariants), chuyển đổi trạng thái phức tạp, sự kiện miền (Domain Events) và phân tách rõ ràng các tầng. |

---

### 3. Cấu trúc Tactical DDD trong `module_template`

Cấu trúc thư mục chuẩn mẫu bên trong một module Core Domain phức tạp:

```text
com.platform.app.module_template/
├── domain/                      # Tầng Domain thuần túy (Core Business Logic - Zero Framework Dependencies)
│   ├── AggregateRoot.java       # Đảm bảo toàn vẹn dữ liệu, kiểm soát các invariants và chuyển đổi trạng thái
│   ├── Entity.java              # Các thực thể nội bộ có định danh riêng trong ranh giới Aggregate
│   ├── ValueObject.java         # Kiểu dữ liệu bất biến, đóng gói thuộc tính và logic kiểm tra tính hợp lệ
│   ├── DomainEvent.java         # Sự kiện miền bất biến thông báo các biến động trạng thái quan trọng
│   └── DomainRepository.java    # Outbound Port Contract (Interface) định nghĩa hành vi lưu trữ
├── application/                 # Tầng Use Cases & Điều phối luồng (Orchestration Layer)
│   ├── ApplicationService.java  # Điều phối use case, quản lý giao dịch (@Transactional) và ủy quyền ngữ cảnh
│   ├── Command/Query.java       # Input DTOs mang dữ liệu thực thi use case
│   └── ResponseDto.java         # Output DTOs an toàn, tách biệt hoàn toàn khỏi cấu trúc nội bộ của domain
├── infrastructure/              # Tầng Outbound Adapters (Hạ tầng kỹ thuật & Cơ sở dữ liệu)
│   ├── JpaRepository.java       # Spring Data JPA Repository giao tiếp PostgreSQL
│   ├── RepositoryImpl.java      # Hiện thực hóa Domain Repository Interface (DIP - Dependency Inversion)
│   └── ExternalAdapter.java     # Gateway kết nối hệ thống bên ngoài / Message Publisher
└── api/                         # Tầng Inbound Driving Adapters (Giao tiếp bên ngoài vào)
    ├── Controller.java          # REST Controllers xử lý routing HTTP, tham số, OpenAPI / Swagger metadata
    └── RequestPayload.java      # Web Request DTOs và xác thực đầu vào qua Bean Validation (@Valid)
```

#### Luồng phụ thuộc (Dependency Flow):
```text
  [ api ] ───────────────┐
                         ▼
  [ infrastructure ] ──► [ application ] ──► [ domain ] (Pure Core - Không phụ thuộc bên ngoài)
```

---

### 4. Bộ kiểm thử (Unit, Domain Invariant & E2E Integration)

- **Domain Unit Tests**: Kiểm tra các logic tính toán, bất biến trạng thái (Invariants) và Value Objects mà không cần khởi động Spring context hay Database.
- **Application Service Tests**: Sử dụng Mockito để mock Domain Repositories, xác thực việc điều phối use case, transaction và mapper.
- **API / Web Integration Tests**: Sử dụng `MockMvc` hoặc `@SpringBootTest` để kiểm tra validation HTTP, serialization và status codes.
- **Persistence Integration Tests**: Sử dụng `@DataJpaTest` hoặc Testcontainers PostgreSQL để xác thực Repository Impl và câu truy vấn database.
