# Research: FEAT-TEST-01 Comprehensive Unit and Slice Testing Suite

<research_context task_id="FEAT-TEST-01" version="1.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. No source modifications. No implementation decisions. -->
<research_status>
  <phase>RESEARCH</phase>
  <mode>READ-ONLY</mode>
  <research_owner>@engineer</research_owner>
  <last_updated>2026-09-19</last_updated>
</research_status>

---

## 1. Current Behavior & Existing Implementation

<current_behavior>
  - **Quy mô dự án hiện tại:**
    - `src/main/java`: 175 files Java.
    - `src/test/java`: 38 files Java.
    - Gradle build & test: `./gradlew test` đang chạy pass 38 test suites với 0 lỗi (thời gian build ~29s).
  - **Kiến trúc ứng dụng (Clean / Hexagonal Architecture):**
    - **Domain Layer:** Chứa Entities/Aggregates (`User`, `Department`, `Document`, `AuditLog`), Value Objects/Enums (`AccessLevel`, `PermissionLevel`, `DocumentStatus`), Domain Exceptions.
    - **Application Layer:** Chứa Inbound Ports (Use Cases & Commands), Outbound Ports (Repository, Notification, Storage interfaces), Application Services (`LoginService`, `DocumentUploadService`, `AuditLoggingService`), và Event Listeners (`AuditEventListener`, `RegistrationOtpEventListener`).
    - **Infrastructure Layer:** Chứa Primary Adapters (REST Controllers, Global Exception Handler, DTOs) và Secondary Adapters (JPA Repositories & Adapters, S3 Object Storage Adapter, Redis OTP Adapter, JWT Token Provider, Security Filters).
  - **Hiện trạng kiểm thử theo phân tầng:**
    1. **Pure Interfaces (Inbound/Outbound Ports, Spring Data Interfaces) — 40 files:**
       - Là các Java `interface` thuần túy, không có default method phức tạp.
       - *Chiến lược:* Không tạo unit test độc lập cho interface thuần; các interface này được kiểm chứng qua `@Mock` trong Use Case tests và qua test của các Adapter thực thi chúng.
    2. **Application Services & Listeners — 13 files:**
       - Đã có test: 10 files (`AssignRolesServiceTest`, `DepartmentServiceTest`, `LoginServiceTest`, `VerifyOtpServiceTest`, `DocumentAclServiceTest`, `DocumentSoftDeleteServiceTest`, `DocumentUploadServiceTest`, `DocumentVersionServiceTest`, `AuditLoggingServiceTest`, `AuditEventListenerTest`).
       - Chưa có test: 3 files:
         * `RegisterService.java` (rất quan trọng: đăng ký user, mã hóa pass, tạo OTP Redis, publish event).
         * `DocumentMetadataService.java` (lưu metadata tài liệu và phiên bản mới).
         * `RegistrationOtpEventListener.java` (lắng nghe event và gửi email OTP).
    3. **REST Controllers & Handlers — 7 files:**
       - Đã có test WebMvcTest: 6 files (`AuthControllerTest`, `DepartmentControllerTest`, `UserDepartmentControllerTest`, `UserRoleControllerTest`, `DocumentControllerTest`, `AuditLogControllerTest`).
       - Chưa có test: `RestExceptionHandler.java` (xử lý tập trung các Exception, mapping HTTP status codes 400, 401, 403, 404, 409, 500).
    4. **Domain Models & Enums — 15 files:**
       - Đã có test: `DepartmentTest`, `RefreshTokenTest`, `UserTest`, `DocumentTest`, `DocumentVersionTest`, `AuditLogTest`, `AccessLevelTest`, `PermissionLevelTest`.
       - Chưa có test trực tiếp: `Role.java`, `Permission.java`, `DocumentDepartmentAccess.java`, `DocumentRoleAccess.java`, `DocumentUserAccess.java`, `AuditStatus.java`, `DocumentStatus.java`.
    5. **Secondary Adapters (Persistence, Security, Storage, Redis) — 22 files:**
       - Đã có test: `DepartmentRepositoryAdapterTest`, `PersistenceAdaptersTest` (chứa test gộp của User/Role/RefreshToken adapter), `DocumentAclRepositoryAdapterTest`, `DocumentRepositoryAdapterTest`, `S3ObjectStorageAdapterTest`, `AuditLogRepositoryAdapterTest`, `BCryptPasswordEncoderAdapterTest`, `InMemoryAccountLockoutAdapterTest`, `JwtTokenProviderAdapterTest`.
       - Chưa có test trực tiếp: `DocumentVersionRepositoryAdapter.java`, `AuditLogSpecification.java`, `RedisOtpAdapter.java`, `LoggingEmailNotificationAdapter.java`, `JwtAuthenticationFilter.java`, `SecurityConfig.java`.
    6. **DTOs, Records & Domain Exceptions — 50+ files:**
       - Hầu hết là Java records hoặc POJO Lombok immutable mang tính data carrier. Cần kiểm tra validation constraints (@NotBlank, @Email, @Size) và serialization/deserialization logic.
</current_behavior>

---

## 2. Testing Framework & Technology Stack

<tech_stack>
  - **Java Version:** Java 25 (OpenJDK 64-Bit Server VM).
  - **Framework:** Spring Boot 4.0.7 / Spring Framework 7.
  - **Core Testing Libraries:**
    - `org.junit.jupiter:junit-jupiter` (JUnit 5: `@Test`, `@BeforeEach`, `@DisplayName`, `@Nested`, `@ParameterizedTest`).
    - `org.assertj:assertj-core` (AssertJ: `assertThat()`, `assertThatThrownBy()`).
    - `org.mockito:mockito-core` & `org.mockito:mockito-junit-jupiter` (`@Mock`, `@InjectMocks`, `when()`, `verify()`, `ArgumentCaptor`).
    - `org.springframework.boot:spring-boot-starter-test`
    - `org.springframework.boot:spring-boot-starter-webmvc-test` (Cung cấp `MockMvc`, `@WebMvcTest`).
    - `org.springframework.boot:spring-boot-starter-data-jpa-test` (Cung cấp `@DataJpaTest`).
    - `org.springframework.boot:spring-boot-starter-security-test` (Cung cấp `@WithMockUser`).
    - Database cho test: H2 In-Memory Database (`com.h2database:h2`).
</tech_stack>

---

## 3. Comprehensive 1:1 File Mapping & Gap Analysis

<mapping_matrix>

### Module 1: IAM Subsystem (`com.platform.app.iam`)

| Source File (`src/main/java/...`) | Loại thành phần | Test File Tương Ứng (`src/test/java/...`) | Trạng thái hiện tại | Chiến lược kiểm thử |
|---|---|---|---|---|
| `application/services/RegisterService.java` | Application Service | `iam/application/services/RegisterServiceTest.java` | **CHƯA CÓ** | Unit Test (Mockito): Test đăng ký thành công, trùng email, hashing pass, sinh OTP Redis, publish event |
| `application/services/LoginService.java` | Application Service | `iam/application/services/LoginServiceTest.java` | **ĐÃ CÓ** | Pass |
| `application/services/VerifyOtpService.java` | Application Service | `iam/application/services/VerifyOtpServiceTest.java` | **ĐÃ CÓ** | Pass |
| `application/services/DepartmentService.java` | Application Service | `iam/application/services/DepartmentServiceTest.java` | **ĐÃ CÓ** | Pass |
| `application/services/AssignRolesService.java` | Application Service | `iam/application/services/AssignRolesServiceTest.java` | **ĐÃ CÓ** | Pass |
| `application/listener/RegistrationOtpEventListener.java` | Application Listener | `iam/application/listener/RegistrationOtpEventListenerTest.java` | **CHƯA CÓ** | Unit Test (Mockito): Test nhận event và dispatch email thành công |
| `domain/model/User.java` | Domain Aggregate | `iam/domain/model/UserTest.java` | **ĐÃ CÓ** | Pass |
| `domain/model/Department.java` | Domain Model | `iam/domain/model/DepartmentTest.java` | **ĐÃ CÓ** | Pass |
| `domain/model/RefreshToken.java` | Domain Model | `iam/domain/model/RefreshTokenTest.java` | **ĐÃ CÓ** | Pass |
| `domain/model/Role.java` | Domain Model | `iam/domain/model/RoleTest.java` | **CHƯA CÓ** | Unit Test: Builder, permissions immutability, role equals/hashCode |
| `domain/model/Permission.java` | Domain Model | `iam/domain/model/PermissionTest.java` | **CHƯA CÓ** | Unit Test: Builder, getters, invariants |
| `infrastructure/adapters/primary/rest/AuthController.java` | REST Controller | `iam/infrastructure/adapters/primary/rest/AuthControllerTest.java` | **ĐÃ CÓ** | MockMvc Slice Test |
| `infrastructure/adapters/primary/rest/DepartmentController.java` | REST Controller | `iam/infrastructure/adapters/primary/rest/DepartmentControllerTest.java` | **ĐÃ CÓ** | MockMvc Slice Test |
| `infrastructure/adapters/primary/rest/UserDepartmentController.java` | REST Controller | `iam/infrastructure/adapters/primary/rest/UserDepartmentControllerTest.java` | **ĐÃ CÓ** | MockMvc Slice Test |
| `infrastructure/adapters/primary/rest/UserRoleController.java` | REST Controller | `iam/infrastructure/adapters/primary/rest/UserRoleControllerTest.java` | **ĐÃ CÓ** | MockMvc Slice Test |
| `infrastructure/adapters/primary/rest/RestExceptionHandler.java` | Controller Advice | `iam/infrastructure/adapters/primary/rest/RestExceptionHandlerTest.java` | **CHƯA CÓ** | MockMvc / Unit Test: Kiểm tra mapping từng exception ra status code chuẩn |
| `infrastructure/adapters/secondary/security/filter/JwtAuthenticationFilter.java` | Security Filter | `iam/infrastructure/adapters/secondary/security/filter/JwtAuthenticationFilterTest.java` | **CHƯA CÓ** | Unit Test (Mock HTTP Servlet): Token valid/invalid/missing header |
| `infrastructure/adapters/secondary/security/config/SecurityConfig.java` | Security Config | `iam/infrastructure/adapters/secondary/security/config/SecurityConfigTest.java` | **CHƯA CÓ** | Slice Test: Xác thực các endpoint public vs secured |
| `infrastructure/adapters/secondary/security/adapter/JwtTokenProviderAdapter.java` | Security Adapter | `iam/infrastructure/adapters/secondary/security/JwtTokenProviderAdapterTest.java` | **ĐÃ CÓ** (vị trí package khác chút) | Pass |
| `infrastructure/adapters/secondary/security/adapter/InMemoryAccountLockoutAdapter.java` | Security Adapter | `iam/infrastructure/adapters/secondary/security/InMemoryAccountLockoutAdapterTest.java` | **ĐÃ CÓ** | Pass |
| `infrastructure/adapters/secondary/redis/RedisOtpAdapter.java` | Secondary Adapter | `iam/infrastructure/adapters/secondary/redis/RedisOtpAdapterTest.java` | **CHƯA CÓ** | Unit Test (Mock RedisTemplate): save, get, delete, TTL |
| `infrastructure/adapters/secondary/notification/LoggingEmailNotificationAdapter.java` | Secondary Adapter | `iam/infrastructure/adapters/secondary/notification/LoggingEmailNotificationAdapterTest.java` | **CHƯA CÓ** | Unit Test: Gọi sendOtpEmail không throw exception |
| `infrastructure/adapters/secondary/persistence/adapter/DepartmentRepositoryAdapter.java` | Persistence Adapter | `iam/infrastructure/adapters/secondary/persistence/adapter/DepartmentRepositoryAdapterTest.java` | **ĐÃ CÓ** | DataJpa / Mock Test |
| `infrastructure/adapters/secondary/persistence/adapter/UserRepositoryAdapter.java` | Persistence Adapter | `iam/infrastructure/adapters/secondary/persistence/adapter/UserRepositoryAdapterTest.java` | **GHÉP TRONG PersistenceAdaptersTest** | Cần tách hoặc bổ sung test case riêng biệt chuyên sâu |
| `infrastructure/adapters/secondary/persistence/adapter/RoleRepositoryAdapter.java` | Persistence Adapter | `iam/infrastructure/adapters/secondary/persistence/adapter/RoleRepositoryAdapterTest.java` | **GHÉP TRONG PersistenceAdaptersTest** | Cần tách/bổ sung test case riêng |
| `infrastructure/adapters/secondary/persistence/adapter/RefreshTokenRepositoryAdapter.java` | Persistence Adapter | `iam/infrastructure/adapters/secondary/persistence/adapter/RefreshTokenRepositoryAdapterTest.java` | **GHÉP TRONG PersistenceAdaptersTest** | Cần tách/bổ sung test case riêng |

---

### Module 2: Document Subsystem (`com.platform.app.document`)

| Source File (`src/main/java/...`) | Loại thành phần | Test File Tương Ứng (`src/test/java/...`) | Trạng thái hiện tại | Chiến lược kiểm thử |
|---|---|---|---|---|
| `application/services/DocumentMetadataService.java` | Application Service | `document/application/services/DocumentMetadataServiceTest.java` | **CHƯA CÓ** | Unit Test (Mockito): Test persistMetadata, persistVersionMetadata |
| `application/services/DocumentUploadService.java` | Application Service | `document/application/services/DocumentUploadServiceTest.java` | **ĐÃ CÓ** | Pass |
| `application/services/DocumentVersionService.java` | Application Service | `document/application/services/DocumentVersionServiceTest.java` | **ĐÃ CÓ** | Pass |
| `application/services/DocumentAclService.java` | Application Service | `document/application/services/DocumentAclServiceTest.java` | **ĐÃ CÓ** | Pass |
| `application/services/DocumentSoftDeleteService.java` | Application Service | `document/application/services/DocumentSoftDeleteServiceTest.java` | **ĐÃ CÓ** | Pass |
| `domain/model/Document.java` | Domain Aggregate | `document/domain/model/DocumentTest.java` | **ĐÃ CÓ** | Pass |
| `domain/model/DocumentVersion.java` | Domain Model | `document/domain/model/DocumentVersionTest.java` | **ĐÃ CÓ** | Pass |
| `domain/model/AccessLevel.java` | Domain Enum | `document/domain/model/AccessLevelTest.java` | **ĐÃ CÓ** | Pass |
| `domain/model/PermissionLevel.java` | Domain Enum | `document/domain/model/PermissionLevelTest.java` | **ĐÃ CÓ** | Pass |
| `domain/model/DocumentStatus.java` | Domain Enum | `document/domain/model/DocumentStatusTest.java` | **CHƯA CÓ** | Unit Test: Enum values, name, transitions |
| `domain/model/DocumentUserAccess.java` | Domain Model | `document/domain/model/DocumentUserAccessTest.java` | **CHƯA CÓ** | Unit Test: Model invariants, equals, builder |
| `domain/model/DocumentDepartmentAccess.java` | Domain Model | `document/domain/model/DocumentDepartmentAccessTest.java` | **CHƯA CÓ** | Unit Test: Model invariants, equals, builder |
| `domain/model/DocumentRoleAccess.java` | Domain Model | `document/domain/model/DocumentRoleAccessTest.java` | **CHƯA CÓ** | Unit Test: Model invariants, equals, builder |
| `infrastructure/adapters/primary/rest/DocumentController.java` | REST Controller | `document/infrastructure/adapters/primary/rest/DocumentControllerTest.java` | **ĐÃ CÓ** | MockMvc Slice Test |
| `infrastructure/adapters/secondary/persistence/adapter/DocumentRepositoryAdapter.java` | Persistence Adapter | `document/infrastructure/adapters/secondary/persistence/adapter/DocumentRepositoryAdapterTest.java` | **ĐÃ CÓ** | DataJpa / Mock Test |
| `infrastructure/adapters/secondary/persistence/adapter/DocumentAclRepositoryAdapter.java` | Persistence Adapter | `document/infrastructure/adapters/secondary/persistence/adapter/DocumentAclRepositoryAdapterTest.java` | **ĐÃ CÓ** | DataJpa / Mock Test |
| `infrastructure/adapters/secondary/persistence/adapter/DocumentVersionRepositoryAdapter.java` | Persistence Adapter | `document/infrastructure/adapters/secondary/persistence/adapter/DocumentVersionRepositoryAdapterTest.java` | **CHƯA CÓ** | Unit/Slice Test: Mapping entity <-> domain, findByDocumentId |
| `infrastructure/adapters/secondary/storage/adapter/S3ObjectStorageAdapter.java` | Secondary Storage | `document/infrastructure/adapters/secondary/storage/adapter/S3ObjectStorageAdapterTest.java` | **ĐÃ CÓ** | Pass |
| `infrastructure/adapters/secondary/storage/config/S3StorageProperties.java` | Configuration | `document/infrastructure/adapters/secondary/storage/config/S3StoragePropertiesTest.java` | **CHƯA CÓ** | Unit Test: Getter/Setter/Builder properties |

---

### Module 3: Audit Subsystem (`com.platform.app.audit`)

| Source File (`src/main/java/...`) | Loại thành phần | Test File Tương Ứng (`src/test/java/...`) | Trạng thái hiện tại | Chiến lược kiểm thử |
|---|---|---|---|---|
| `application/services/AuditLoggingService.java` | Application Service | `audit/application/services/AuditLoggingServiceTest.java` | **ĐÃ CÓ** | Pass |
| `application/listener/AuditEventListener.java` | Application Listener | `audit/application/listener/AuditEventListenerTest.java` | **ĐÃ CÓ** | Pass |
| `domain/model/AuditLog.java` | Domain Aggregate | `audit/domain/model/AuditLogTest.java` | **ĐÃ CÓ** | Pass |
| `domain/model/AuditStatus.java` | Domain Enum | `audit/domain/model/AuditStatusTest.java` | **CHƯA CÓ** | Unit Test: Enum constants |
| `infrastructure/adapters/primary/rest/AuditLogController.java` | REST Controller | `audit/infrastructure/adapters/primary/rest/AuditLogControllerTest.java` | **ĐÃ CÓ** | MockMvc Slice Test |
| `infrastructure/adapters/secondary/persistence/adapter/AuditLogRepositoryAdapter.java` | Persistence Adapter | `audit/infrastructure/adapters/secondary/persistence/adapter/AuditLogRepositoryAdapterTest.java` | **ĐÃ CÓ** | DataJpa Test |
| `infrastructure/adapters/secondary/persistence/specification/AuditLogSpecification.java` | JPA Specification | `audit/infrastructure/adapters/secondary/persistence/specification/AuditLogSpecificationTest.java` | **CHƯA CÓ** | DataJpa Test: Lọc theo user, action, date range, resource |

---

### Module 4: Shared & Infrastructure (`com.platform.app.shared`)

| Source File (`src/main/java/...`) | Loại thành phần | Test File Tương Ứng (`src/test/java/...`) | Trạng thái hiện tại | Chiến lược kiểm thử |
|---|---|---|---|---|
| `shared/util/IdGenerator.java` | Utility | `shared/util/IdGeneratorTest.java` | **ĐÃ CÓ** | Pass |
| `shared/infrastructure/logging/TraceIdFilter.java` | Web Filter | `shared/infrastructure/logging/TraceIdFilterTest.java` | **ĐÃ CÓ** | Pass |
| `shared/infrastructure/logging/LoggingConstants.java` | Constants | `shared/infrastructure/logging/LoggingConstantsTest.java` | **CHƯA CÓ** | Unit Test: Verify constant strings |

</mapping_matrix>

---

## 4. Research Observations & Findings

<findings>
  1. **Tách biệt rõ rệt giữa Pure Unit Tests và Spring Context Tests:**
     - Các bài test hiện tại trong dự án như `DepartmentServiceTest` và `AuditLoggingServiceTest` sử dụng thuần `@ExtendWith(MockitoExtension.class)`. Điều này giúp tốc độ thực thi rất nhanh (chỉ mất ~10-20ms mỗi test).
     - Các REST Controller sử dụng `@WebMvcTest` kết hợp `@AutoConfigureMockMvc(addFilters = false)` hoặc `@WithMockUser`.
  2. **Các khoảng trống kiểm thử lớn (Test Coverage Gaps):**
     - **Gap 1 (Critical):** `RegisterService` chưa được kiểm thử. Đây là luồng quan trọng của IAM bao gồm sinh mật khẩu, kiểm tra email trùng lặp, tạo mã OTP ngẫu nhiên 6 chữ số và bắn domain event.
     - **Gap 2 (Critical):** `DocumentMetadataService` chưa được kiểm thử trực tiếp dù là component cốt lõi phụ trách transaction lưu trữ document & version.
     - **Gap 3 (Security & Infrastructure):** `JwtAuthenticationFilter` và `RestExceptionHandler` chưa có test riêng biệt để bảo đảm các request lỗi hoặc token không hợp lệ trả về đúng cấu trúc `ErrorResponse`.
     - **Gap 4 (Secondary Adapters):** `RedisOtpAdapter`, `LoggingEmailNotificationAdapter`, `DocumentVersionRepositoryAdapter`, `AuditLogSpecification`.
     - **Gap 5 (Domain Models):** Các entities quan trọng như `Role`, `Permission`, các model ACL (`DocumentUserAccess`, `DocumentDepartmentAccess`, `DocumentRoleAccess`) chưa có unit test độc lập để bảo đảm các invariant và builder contract.
</findings>

---

## 5. Gate G0 Exit Criteria Checklist

<gate_g0>
  - [x] Đã quét toàn bộ 175 files trong `src/main/java` và phân loại theo vai trò kiến trúc.
  - [x] Đã thiết lập ma trận đối chiếu 1:1 giữa các file `src/main` và các file `src/test`.
  - [x] Đã xác định rõ các interface thuần túy không cần test riêng và các lớp cần kiểm thử 100%.
  - [x] Đã nắm vững công nghệ test của hệ thống: JUnit 5, AssertJ, Mockito, Spring Boot 4.0.7 Test Slices, H2 database.
  - [x] Không còn khúc mắc nghiên cứu tồn đọng.
</gate_g0>

</research_context>
