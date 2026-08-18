# architecture

## kiến trúc đã áp dụng

                                        +-----------------------------------------+
                                        |         com.platform.app                |
                                        +-----------------------------------------+
                                                             |
             +------------------------+----------------------+----------------------+------------------------+
             |                        |                                             |                        |
             v                        v                                             v                        v
    +------------------+    +--------------------+                       +--------------------+    +------------------+
    |      shared      |    |       system       |                       |        iam         |    |      ticket      |
    |  (Shared Kernel  |    | (System Diag /     |                       | (Identity & Access |    |   (Core Domain:  |
    |  & Cross-Cutting)|    |  Lean Capability)  |                       |  Lean Capability)  |    |   Tactical DDD)  |
    +------------------+    +--------------------+                       +--------------------+    +------------------+
                                                                                                             |
                                                                                 +---------------------------+---------------------------+
                                                                                 |                           |                           |
                                                                                 v                           v                           v
                                                                        +-----------------+         +-----------------+         +-----------------+
                                                                        |     domain      |         |   application   |         | infrastructure  |
                                                                        | (Aggregate Root,|         | (Use Cases, DTOs|         | & web adapters) |
                                                                        |  VOs, Events)   |         |  App Service)   |         |                 |
                                                                        +-----------------+         +-----------------+         +-----------------+

──────

### 1. Package by Business Capability (Package cấp cao nhất)

Thay vì chia theo các tầng kỹ thuật ngang (controllers, services, repositories) cho toàn dự án, package cấp 1 đại diện cho các Business Capabilities / Bounded Contexts:

• **shared**: Shared Kernel chứa các thành phần dùng chung (BaseEntity, AggregateRoot, DomainEvent, ApiResponse, GlobalExceptionHandler, SecurityConfig, JwtTokenProvider).
• **system**: Capability chẩn đoán và kiểm tra sức khỏe hệ thống (/api/v1/health).
• **iam**: Capability quản lý định danh & phân quyền (Identity & Access Management - đăng ký, đăng nhập, JWT tokens, profile).
• **ticket**: Core Domain quản lý vòng đời ticket hỗ trợ khách hàng, hội thoại, phân công và trạng thái.
──────

### 2. Selective Tactical DDD (Áp dụng DDD chiến thuật có chọn lọc)

Business Capability │ Độ phức tạp │ Mô hình áp dụng │ Lý do thiết kế
─────────────────────┼───────────────────┼────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────
system │ Rất thấp │ Thin / Pragmatic Controller │ Chỉ kiểm tra uptime và trạng thái service, không có logic nghiệp vụ.
iam │ Trung bình - thấp │ Pragmatic Service + Repository │ Nghiệp vụ xác thực & CRUD tài khoản đơn giản, không cần Aggregate Root phức tạp để tránh over-
│ │ │ engineering.
ticket │ Cao (Core Domain) │ Full Tactical DDD │ Nghiệp vụ trung tâm có nhiều ràng buộc (invariants), chuyển đổi trạng thái phức tạp, sự kiện miền và
│ │ │ phân quyền theo ngữ cảnh.
──────

### 3. Cấu trúc Tactical DDD trong Module ticket

• domain/ (Tầng Domain thuần túy, không phụ thuộc framework):
• Aggregate Root (Ticket.java): Đảm bảo toàn vẹn dữ liệu, kiểm soát các chuyển đổi trạng thái (create, assignTo, changeStatus, resolve, close, reopen, addMessage).
• Entity (TicketMessage.java): Thực thể nằm trong Aggregate boundary.
• Value Objects (TicketStatus.java, TicketPriority.java, TicketCategory.java): Bất biến, chứa logic kiểm tra chuyển đổi trạng thái (canTransitionTo).
• Domain Events (TicketCreatedEvent.java, TicketStatusChangedEvent.java, TicketAssignedEvent.java): Được phát ra qua Spring Data @DomainEvents.
• Domain Repository Interface (TicketRepository.java): Hợp đồng lưu trữ định nghĩa tại tầng Domain.
• application/ (Tầng Use Cases):
• **TicketApplicationService.java**: Điều phối các use case, quản lý giao dịch @Transactional và bảo mật.
• Commands & DTOs (CreateTicketCommand.java, AssignTicketCommand.java, UpdateTicketStatusCommand.java, TicketResponse.java).
• infrastructure/ (Outbound Persistence Adapter):
• **TicketRepositoryImpl.java**: Thực thi TicketRepository thông qua SpringDataJpaTicketRepository.
• web/ (Inbound REST Adapter):
• **TicketController.java**: REST endpoints hỗ trợ Swagger UI & Bearer Auth.

──────

### Bộ kiểm thử (Unit, Domain Invariant & E2E Integration)
