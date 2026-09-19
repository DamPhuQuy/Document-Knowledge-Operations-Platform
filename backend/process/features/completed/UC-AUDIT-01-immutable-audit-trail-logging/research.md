# Research: UC-AUDIT-01 Immutable Audit Trail Logging

<research_context task_id="UC-AUDIT-01" version="1.0" framework="RIPER-5">

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
  - **Audit Subsystem (`com.platform.app.audit`):**
    - Currently, no package or classes exist under `backend/src/main/java/com/platform/app/audit`.
    - No domain model (`AuditLog`), JPA entity (`AuditLogJpaEntity`), repository, or REST controller exists.
  - **Database Schema Status (`db/changelog/`):**
    - The PostgreSQL table `audit_logs` has already been created via Liquibase changeset `004-create-audit-tables.yaml`.
    - Columns defined:
      - `id UUID PRIMARY KEY`
      - `user_id UUID REFERENCES users(id) ON DELETE SET NULL`
      - `action VARCHAR(100) NOT NULL`
      - `resource_type VARCHAR(100) NOT NULL`
      - `resource_id VARCHAR(64)`
      - `ip_address VARCHAR(45)`
      - `user_agent TEXT`
      - `status VARCHAR(50) NOT NULL DEFAULT 'SUCCESS'`
      - `details JSONB NOT NULL DEFAULT '{}'::jsonb`
      - `created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`
    - Indexes defined:
      - `idx_audit_logs_user_id` on `(user_id)`
      - `idx_audit_logs_resource` on `(resource_type, resource_id)`
      - `idx_audit_logs_created_at` on `(created_at)`
      - `idx_audit_logs_user_created` on `(user_id, created_at)`
  - **Existing Domain Events Catalog:**
    - Multiple application services already publish domain events via Spring's `ApplicationEventPublisher`:
      - IAM Events:
        - `UserLoginSuccessEvent(UUID userId, String email, String clientIp, String userAgent, Instant timestamp)`
        - `UserLoginFailedEvent(String email, String clientIp, String userAgent, String reason, Instant timestamp)`
        - `UserRegisteredOtpEvent(UUID userId, String email, String otpCode, Instant expiresAt)`
        - `UserRolesUpdatedEvent(UUID targetUserId, List<Long> roleIds, Instant timestamp)`
        - `DepartmentCreatedEvent(UUID departmentId, String code, String name, Instant timestamp)`
        - `DepartmentUpdatedEvent(UUID departmentId, String code, String name, Instant timestamp)`
        - `UserDepartmentAssignedEvent(UUID userId, UUID departmentId, boolean isInternal, Instant timestamp)`
      - Document Events:
        - `DocumentUploadedEvent(UUID documentId, String title, String originalFileName, String contentType, long fileSizeBytes, String checksumSha256, String storageKey, UUID departmentId, UUID uploadedByUserId, Instant timestamp)`
        - `DocumentVersionCreatedEvent(UUID documentId, UUID versionId, int versionNumber, String storageKey, long fileSizeBytes, String checksumSha256, UUID uploadedByUserId, Instant timestamp)`
        - `DocumentAclUpdatedEvent(UUID documentId, UUID updatedByUserId, AccessLevel accessLevel, int userGrantsCount, int departmentGrantsCount, int roleGrantsCount, Instant timestamp)`
        - `DocumentSoftDeletedEvent(UUID documentId, UUID deletedByUserId, Instant timestamp)`
    - None of these events are currently handled for audit trail persistence; there is only `RegistrationOtpEventListener` (which sends OTP emails via an email notification port).
  - **Security & Authorization Status:**
    - `AppApplication.java` has `@EnableAsync` enabled.
    - `SecurityConfig.java` enables method security (`@EnableMethodSecurity`).
    - Permission `read:audit_logs` is defined in `docs/specs/business/02_actor_matrix_and_rbac.md` and assigned to `ROLE_ADMIN` and `ROLE_LEGAL_AUDITOR`.
    - No endpoint currently exists for `GET /api/v1/audit-logs`.
</current_behavior>

---

## 2. Execution Flows

<execution_flow>

### Flow 1: Asynchronous Event Ingestion & Immutable Persistence
```text
Domain Service (IAM / Document)
  → Performs mutation (e.g. upload, soft-delete, login, role assignment)
  → Emits domain event via ApplicationEventPublisher.publishEvent(event)
    → Spring Event Multicaster dispatches event
      → AuditEventListener (@Async, @TransactionalEventListener / @EventListener)
        ├── 1. Maps event to RecordAuditLogCommand
        │      (extracts userId, action, resourceType, resourceId, ip, userAgent, details)
        ├── 2. Invokes RecordAuditLogUseCase.recordAuditLog(command)
        │      └── AuditLoggingService builds immutable AuditLog domain model
        ├── 3. Persists through AuditLogRepositoryPort.save(auditLog)
        │      └── AuditLogRepositoryAdapter converts to AuditLogJpaEntity
        │      └── SpringDataAuditLogRepository inserts into PostgreSQL table `audit_logs`
        └── 4. Completes asynchronously with zero latency impact on HTTP caller
```

### Flow 2: Audit Trail Log Query (`GET /api/v1/audit-logs`)
```text
Client (Admin / Compliance Auditor)
  → GET /api/v1/audit-logs?userId=...&action=...&resourceType=...&startDate=...&endDate=...&page=0&size=20&sort=createdAt,desc
    → JwtAuthenticationFilter (validates JWT Bearer)
    → SecurityFilterChain
    → AuditLogController.getAuditLogs(queryRequest, pageable, authentication)
      ├── 1. Check authorization via @PreAuthorize:
      │      Requires: hasAuthority('read:audit_logs') OR hasRole('ADMIN') OR hasRole('LEGAL_AUDITOR')
      │      If unauthenticated -> HTTP 401 Unauthorized
      │      If unauthorized -> HTTP 403 Forbidden
      ├── 2. Map request parameters to AuditLogQueryFilter
      ├── 3. Invoke GetAuditLogsUseCase.getAuditLogs(filter, pageable)
      │      └── AuditLoggingService calls AuditLogRepositoryPort.findAll(filter, pageable)
      │      └── AuditLogRepositoryAdapter runs dynamic Criteria/Specification query against `audit_logs`
      ├── 4. Maps entities to Page<AuditLogResponseDto>
      └── 5. Returns HTTP 200 OK with paginated audit logs
```

</execution_flow>

---

## 3. Relevant Components

<components>

| File / Symbol | Role | Evidence | Confidence |
|---|---|---|---|
| `AuditLog.java` | Domain Aggregate / Model | Encapsulates immutable audit log entry | Confirmed |
| `AuditStatus.java` | Domain Value Object / Enum | `SUCCESS`, `FAILED` matching schema | Confirmed |
| `RecordAuditLogCommand.java` | Application Command DTO | Payload for recording an audit log entry | Confirmed |
| `AuditLogQueryFilter.java` | Application Query DTO | Criteria for querying audit logs (date range, user, action, resource, status) | Confirmed |
| `AuditLogResponseDto.java` | Application Response DTO | Read model returned to API clients | Confirmed |
| `RecordAuditLogUseCase.java` | Inbound Port | Command interface for audit logging | Confirmed |
| `GetAuditLogsUseCase.java` | Inbound Port | Query interface for searching audit logs | Confirmed |
| `AuditLogRepositoryPort.java` | Outbound Port | Contract for persisting and querying audit records | Confirmed |
| `AuditLoggingService.java` | Application Service | Implements use cases for recording and querying logs | Confirmed |
| `AuditEventListener.java` | Application Event Listener | Listens to all domain events asynchronously (`@Async`) | Confirmed |
| `AuditLogJpaEntity.java` | Persistence Entity | Maps PostgreSQL table `audit_logs` with JSONB `details` | Confirmed |
| `SpringDataAuditLogRepository.java` | Spring Data Repository | Executes JpaRepository and JpaSpecificationExecutor operations | Confirmed |
| `AuditLogRepositoryAdapter.java` | Persistence Adapter | Implements `AuditLogRepositoryPort` using JPA repository | Confirmed |
| `AuditLogController.java` | REST Controller | Exposes `GET /api/v1/audit-logs` protected by RBAC | Confirmed |

</components>

---

## 4. Dependencies & Boundaries

<boundaries>
  <callers>
    - Internal: Spring ApplicationEventPublisher emitting IAM and Document domain events.
    - External: HTTP GET requests from Admins / Compliance Auditors to `/api/v1/audit-logs`.
  </callers>
  <callees>
    - PostgreSQL database (`audit_logs` table).
    - Jackson ObjectMapper (for JSONB serialization of `details`).
  </callees>
  <persistence>
    - Table `audit_logs` (PostgreSQL).
    - Strictly APPEND-ONLY: only `INSERT` and `SELECT` statements are permitted.
  </persistence>
  <external_systems>
    - None for MVP (future: cloud SIEM, S3 cold archive).
  </external_systems>
  <transaction_boundary>
    - Ingestion: Each asynchronous audit logging execution runs in its own independent transaction or `REQUIRES_NEW` to guarantee that main business transactions are not rolled back if audit logging encounters an issue, and vice versa.
    - Query: `@Transactional(readOnly = true)` for search operations.
  </transaction_boundary>
  <security_boundary>
    - Query API protected with `@PreAuthorize("hasAuthority('read:audit_logs') or hasAuthority('READ:AUDIT_LOGS') or hasRole('ADMIN') or hasRole('LEGAL_AUDITOR')")`.
    - No public or staff-level access permitted.
  </security_boundary>
</boundaries>

---

## 5. Existing Tests

<existing_tests>

| Test | Behavior covered | Gap |
|---|---|---|
| `DocumentControllerTest.java` | Upload, version, ACL, delete endpoints | Does not assert audit log persistence |
| `AuthControllerTest.java` | Login, OTP, registration endpoints | Does not assert audit log persistence |
| `DocumentAclServiceTest.java` | ACL business logic & event publishing | Verifies event publishing, but no listener test |
| `DocumentSoftDeleteServiceTest.java` | Soft delete logic & event publishing | Verifies event publishing, but no listener test |

</existing_tests>

---

## 6. Runtime / Configuration

<runtime_config>
  - **Spring Async:** `@EnableAsync` already enabled on `AppApplication.java`.
  - **PostgreSQL JSONB:** Hibernate 6/7 native `@JdbcTypeCode(SqlTypes.JSON)` or `@Column(columnDefinition = "jsonb")`.
  - **Liquibase:** Schema `004-create-audit-tables.yaml` already deployed; no schema modifications required.
</runtime_config>

---

## 7. Source-of-Truth Analysis

<source_of_truth_analysis>

| Source | Says | Authority | Conflict |
|---|---|---|---|
| `docs/specs/business/use_cases/03_audit_trail.md` | Defines UC-AUDIT-01: Immutable Audit Trail Logging, Basic Path, B1, B2, NF1 | High (Business Spec) | None |
| `docs/specs/business/02_actor_matrix_and_rbac.md` | SYS-04 Audit Subsystem; `read:audit_logs` allowed for ADMIN and LEGAL_AUDITOR | High (Actor Matrix) | None |
| `docs/specs/business/05_requirements_traceability_matrix.md` | Endpoint `GET /api/v1/audit-logs`, table `audit_logs`, P0 Must Have | High (Traceability) | None |
| `004-create-audit-tables.yaml` | Columns: id, user_id, action, resource_type, resource_id, ip_address, user_agent, status, details, created_at | High (Schema) | None |

</source_of_truth_analysis>

---

## 8. Evidence Classification

<evidence>
  <confirmed>
    - PostgreSQL table `audit_logs` exists with full schema in Liquibase changelog `004-create-audit-tables.yaml`.
    - Domain events (`DocumentUploadedEvent`, `DocumentVersionCreatedEvent`, `DocumentAclUpdatedEvent`, `DocumentSoftDeletedEvent`, `UserLoginSuccessEvent`, `UserLoginFailedEvent`, `UserRolesUpdatedEvent`, etc.) are already actively published in the codebase.
    - Security framework is configured with JWT authentication, role extraction, and method security.
    - `AppApplication` has `@EnableAsync` configured.
  </confirmed>

  <observed>
    - No existing Java code exists in `com.platform.app.audit`.
    - No query or mutation endpoints exist for audit logs.
  </observed>

  <hypothesized>
    - Handling domain events asynchronously using Spring `@Async` event listener will decouple audit persistence from the main request thread with minimal overhead.
  </hypothesized>
</evidence>

---

## 9. Assumptions & Uncertainty

<assumptions>
  - **Asynchronous Execution (Low Uncertainty):** Spring's standard `@Async` listener over task executor provides sufficient throughput and decoupling for MVP.
  - **JSONB Details (Low Uncertainty):** Using `Map<String, Object>` serialized to JSONB accommodates varied event payloads (before/after changes, metadata, error reasons).
  - **Dynamic Query Filtering (Low Uncertainty):** Spring Data JPA `Specification<AuditLogJpaEntity>` provides flexible dynamic SQL predicate construction for date ranges, user IDs, actions, and resource types.
</assumptions>

---

## 10. Impacted Files

<impacted_files>
  - `src/main/java/com/platform/app/audit/domain/model/AuditLog.java` — Core domain entity representing an audit log
  - `src/main/java/com/platform/app/audit/domain/model/AuditStatus.java` — Audit status enum (`SUCCESS`, `FAILED`)
  - `src/main/java/com/platform/app/audit/application/dto/RecordAuditLogCommand.java` — DTO for recording audit events
  - `src/main/java/com/platform/app/audit/application/dto/AuditLogQueryFilter.java` — DTO for search/filtering parameters
  - `src/main/java/com/platform/app/audit/application/dto/AuditLogResponseDto.java` — Read model DTO for API responses
  - `src/main/java/com/platform/app/audit/application/ports/inbound/RecordAuditLogUseCase.java` — Inbound use case port
  - `src/main/java/com/platform/app/audit/application/ports/inbound/GetAuditLogsUseCase.java` — Inbound query port
  - `src/main/java/com/platform/app/audit/application/ports/outbound/AuditLogRepositoryPort.java` — Outbound repository port
  - `src/main/java/com/platform/app/audit/application/services/AuditLoggingService.java` — Service orchestrating use cases
  - `src/main/java/com/platform/app/audit/application/listener/AuditEventListener.java` — Asynchronous event listener
  - `src/main/java/com/platform/app/audit/infrastructure/adapters/secondary/persistence/entity/AuditLogJpaEntity.java` — JPA entity
  - `src/main/java/com/platform/app/audit/infrastructure/adapters/secondary/persistence/repository/SpringDataAuditLogRepository.java` — Spring Data JPA repository with JpaSpecificationExecutor
  - `src/main/java/com/platform/app/audit/infrastructure/adapters/secondary/persistence/adapter/AuditLogRepositoryAdapter.java` — Adapter implementing outbound port
  - `src/main/java/com/platform/app/audit/infrastructure/adapters/primary/rest/AuditLogController.java` — REST controller for `GET /api/v1/audit-logs`
  - Tests under `src/test/java/com/platform/app/audit/`
</impacted_files>

---

## 11. Open Decisions

<open_decisions>
  - **Decision 1:** Event handling mechanism: Standard `@EventListener` with `@Async` vs `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.
  - **Decision 2:** JSONB mapping approach: `@JdbcTypeCode(SqlTypes.JSON)` with Jackson `Map<String, Object>` vs JSON string column.
  - **Decision 3:** Dynamic filtering strategy: Spring Data `Specification` (Criteria API) vs QueryDSL vs native SQL.
</open_decisions>

---

## 12. Research Exit Criteria

<research_exit_criteria>
  - [x] Current behavior understood and documented.
  - [x] Execution flow traced end-to-end.
  - [x] Relevant boundaries identified (callers, callees, persistence, security).
  - [x] Source-of-truth conflicts identified or confirmed absent.
  - [x] Impacted files identified.
  - [x] Evidence classified (Confirmed / Observed / Hypothesized).
  - [x] No unresolved research blocker.
</research_exit_criteria>

</research_context>
