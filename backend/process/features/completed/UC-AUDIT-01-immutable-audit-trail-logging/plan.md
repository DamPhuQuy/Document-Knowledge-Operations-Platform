# Plan: PLAN-AUDIT-01 UC-AUDIT-01 Immutable Audit Trail Logging

<execution_plan task_id="UC-AUDIT-01" plan_id="PLAN-AUDIT-01" version="1.0" framework="RIPER-5">

<!-- PLAN PHASE. Plan artifacts only. No source-code changes. -->
<plan_status>
  <phase>PLAN</phase>
  <last_updated>2026-09-19</last_updated>
</plan_status>

---

## 1. Input Artifacts

<input_artifacts>
  <task_spec>`process/features/active/UC-AUDIT-01-immutable-audit-trail-logging/task.md`</task_spec>
  <research>`process/features/active/UC-AUDIT-01-immutable-audit-trail-logging/research.md`</research>
  <decision>`process/features/active/UC-AUDIT-01-immutable-audit-trail-logging/decision.md` — DEC-AUDIT-01 (TransactionalEventListener + @Async, @JdbcTypeCode(SqlTypes.JSON), Spring Data Specification)</decision>
</input_artifacts>

---

## 2. Execution Constraints

<execution_constraints>
  <allowed_files>
    - `src/main/java/com/platform/app/audit/domain/model/AuditLog.java`
    - `src/main/java/com/platform/app/audit/domain/model/AuditStatus.java`
    - `src/main/java/com/platform/app/audit/application/dto/RecordAuditLogCommand.java`
    - `src/main/java/com/platform/app/audit/application/dto/AuditLogQueryFilter.java`
    - `src/main/java/com/platform/app/audit/application/dto/AuditLogResponseDto.java`
    - `src/main/java/com/platform/app/audit/application/ports/inbound/RecordAuditLogUseCase.java`
    - `src/main/java/com/platform/app/audit/application/ports/inbound/GetAuditLogsUseCase.java`
    - `src/main/java/com/platform/app/audit/application/ports/outbound/AuditLogRepositoryPort.java`
    - `src/main/java/com/platform/app/audit/application/services/AuditLoggingService.java`
    - `src/main/java/com/platform/app/audit/application/listener/AuditEventListener.java`
    - `src/main/java/com/platform/app/audit/infrastructure/adapters/secondary/persistence/entity/AuditLogJpaEntity.java`
    - `src/main/java/com/platform/app/audit/infrastructure/adapters/secondary/persistence/repository/SpringDataAuditLogRepository.java`
    - `src/main/java/com/platform/app/audit/infrastructure/adapters/secondary/persistence/specification/AuditLogSpecification.java`
    - `src/main/java/com/platform/app/audit/infrastructure/adapters/secondary/persistence/adapter/AuditLogRepositoryAdapter.java`
    - `src/main/java/com/platform/app/audit/infrastructure/adapters/primary/rest/AuditLogController.java`
    - `src/test/java/com/platform/app/audit/domain/model/AuditLogTest.java`
    - `src/test/java/com/platform/app/audit/application/services/AuditLoggingServiceTest.java`
    - `src/test/java/com/platform/app/audit/application/listener/AuditEventListenerTest.java`
    - `src/test/java/com/platform/app/audit/infrastructure/adapters/primary/rest/AuditLogControllerTest.java`
    - `src/test/java/com/platform/app/audit/infrastructure/adapters/secondary/persistence/adapter/AuditLogRepositoryAdapterTest.java`
  </allowed_files>
  <forbidden_files>
    - `src/main/resources/db/changelog/` (Table `audit_logs` is already created in `004-create-audit-tables.yaml`)
    - Existing files in `com.platform.app.iam` and `com.platform.app.document` (Existing domain events will be listened to without modifying publisher classes)
  </forbidden_files>
  <allowed_commands>
    - `./gradlew test`
    - `./gradlew compileJava`
    - `./gradlew check`
    - `git status`
    - `git diff`
  </allowed_commands>
  <restricted_operations>
    - Strictly APPEND-ONLY: No update or delete operations or endpoints on `audit_logs`.
    - No modification of existing Liquibase changesets.
  </restricted_operations>
  <required_approvals>
    - Gate 2 checklist approved before starting EXECUTE phase.
  </required_approvals>
</execution_constraints>

---

## 3. Slice Summary

<slice_summary>

| ID | Behavior | Boundary | Files | AC | Verifier | Risk | Mode | Rollback |
|---|---|---|---|---|---|---|---|---|
| S1 | Domain Model & Persistence Layer | Domain & Persistence | `AuditLog.java`, `AuditStatus.java`, `AuditLogJpaEntity.java`, `SpringDataAuditLogRepository.java`, `AuditLogSpecification.java`, `AuditLogRepositoryPort.java`, `AuditLogRepositoryAdapter.java` | AC-1, AC-2, AC-3 | `./gradlew test --tests com.platform.app.audit.domain.*` | LOW | Write | `git checkout -- <files>` |
| S2 | Application Service & Query Port | Application Layer | `RecordAuditLogCommand.java`, `AuditLogQueryFilter.java`, `AuditLogResponseDto.java`, `RecordAuditLogUseCase.java`, `GetAuditLogsUseCase.java`, `AuditLoggingService.java` | AC-5 | `./gradlew test --tests com.platform.app.audit.application.services.*` | LOW | Write | `git checkout -- <files>` |
| S3 | Asynchronous Domain Event Listener | Application Listener | `AuditEventListener.java`, `AuditEventListenerTest.java` | AC-4 | `./gradlew test --tests com.platform.app.audit.application.listener.*` | LOW | Write | `git checkout -- <files>` |
| S4 | REST Controller & RBAC Security | Primary REST Adapter | `AuditLogController.java`, `AuditLogControllerTest.java` | AC-6, AC-7 | `./gradlew test --tests com.platform.app.audit.infrastructure.adapters.primary.rest.*` | LOW | Write | `git checkout -- <files>` |
| S5 | Regression & Quality Check | Full Subsystem | Full test harness | AC-8, AC-9 | `./gradlew check` & `./gradlew test` | LOW | Verification | None |

</slice_summary>

---

## 4. Slice Details

<slices>

  <slice id="S1">
    <objective>Create Domain Model and Persistence Adapter for `audit_logs`</objective>
    <change>
      - Create `AuditStatus` enum (`SUCCESS`, `FAILED`).
      - Create `AuditLog` domain model (immutable aggregate with builder, all final fields, zero mutation methods).
      - Create `AuditLogJpaEntity` mapping table `audit_logs` with `@JdbcTypeCode(SqlTypes.JSON)` for `details`.
      - Create `SpringDataAuditLogRepository` extending `JpaRepository` and `JpaSpecificationExecutor`.
      - Create `AuditLogSpecification` building dynamic predicates (`userId`, `action`, `resourceType`, `resourceId`, `status`, `startDate`, `endDate`).
      - Create `AuditLogRepositoryPort` and `AuditLogRepositoryAdapter` implementing save and findAll.
      - Add unit test for domain model and repository adapter.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/audit/domain/model/AuditLog.java`
      - `src/main/java/com/platform/app/audit/domain/model/AuditStatus.java`
      - `src/main/java/com/platform/app/audit/application/dto/AuditLogQueryFilter.java`
      - `src/main/java/com/platform/app/audit/application/ports/outbound/AuditLogRepositoryPort.java`
      - `src/main/java/com/platform/app/audit/infrastructure/adapters/secondary/persistence/entity/AuditLogJpaEntity.java`
      - `src/main/java/com/platform/app/audit/infrastructure/adapters/secondary/persistence/repository/SpringDataAuditLogRepository.java`
      - `src/main/java/com/platform/app/audit/infrastructure/adapters/secondary/persistence/specification/AuditLogSpecification.java`
      - `src/main/java/com/platform/app/audit/infrastructure/adapters/secondary/persistence/adapter/AuditLogRepositoryAdapter.java`
      - `src/test/java/com/platform/app/audit/domain/model/AuditLogTest.java`
      - `src/test/java/com/platform/app/audit/infrastructure/adapters/secondary/persistence/adapter/AuditLogRepositoryAdapterTest.java`
    </allowed_files>
    <acceptance_criteria>
      - AC-1: Domain model is immutable.
      - AC-2: JPA entity maps `audit_logs` columns and JSONB details.
      - AC-3: Repository adapter persists and queries audit entries with dynamic filters.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew compileJava
      ./gradlew test --tests com.platform.app.audit.domain.*
      ```
    </verifier>
    <expected_evidence>Build succeeds; domain model and adapter tests pass.</expected_evidence>
    <rollback_point>`git checkout -- backend/src/main/java/com/platform/app/audit backend/src/test/java/com/platform/app/audit`</rollback_point>
    <stop_conditions>Compilation errors or JSONB mapping failure.</stop_conditions>
  </slice>

  <slice id="S2">
    <objective>Create Inbound Ports and Application Service `AuditLoggingService`</objective>
    <change>
      - Create `RecordAuditLogCommand`, `AuditLogResponseDto`.
      - Create `RecordAuditLogUseCase` and `GetAuditLogsUseCase`.
      - Create `AuditLoggingService` implementing `recordAuditLog(command)` and `getAuditLogs(filter, pageable)`.
      - Unit test `AuditLoggingServiceTest`.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/audit/application/dto/RecordAuditLogCommand.java`
      - `src/main/java/com/platform/app/audit/application/dto/AuditLogResponseDto.java`
      - `src/main/java/com/platform/app/audit/application/ports/inbound/RecordAuditLogUseCase.java`
      - `src/main/java/com/platform/app/audit/application/ports/inbound/GetAuditLogsUseCase.java`
      - `src/main/java/com/platform/app/audit/application/services/AuditLoggingService.java`
      - `src/test/java/com/platform/app/audit/application/services/AuditLoggingServiceTest.java`
    </allowed_files>
    <acceptance_criteria>
      - AC-5: Inbound ports and application service handle log recording and querying.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests com.platform.app.audit.application.services.AuditLoggingServiceTest
      ```
    </verifier>
    <expected_evidence>All service unit tests pass.</expected_evidence>
    <rollback_point>`git checkout -- backend/src/main/java/com/platform/app/audit/application backend/src/test/java/com/platform/app/audit/application`</rollback_point>
    <stop_conditions>Service test failure.</stop_conditions>
  </slice>

  <slice id="S3">
    <objective>Create Asynchronous Event Listener `AuditEventListener`</objective>
    <change>
      - Create `AuditEventListener` with `@Async` and `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)`.
      - Map domain events:
        - `UserLoginSuccessEvent` -> Action: `LOGIN`, Resource: `USER`
        - `UserLoginFailedEvent` -> Action: `LOGIN_FAILED`, Resource: `USER` (status `FAILED`)
        - `UserRegisteredOtpEvent` -> Action: `REGISTER_OTP`, Resource: `USER`
        - `UserRolesUpdatedEvent` -> Action: `ASSIGN_ROLES`, Resource: `USER`
        - `DepartmentCreatedEvent` -> Action: `CREATE_DEPARTMENT`, Resource: `DEPARTMENT`
        - `DepartmentUpdatedEvent` -> Action: `UPDATE_DEPARTMENT`, Resource: `DEPARTMENT`
        - `UserDepartmentAssignedEvent` -> Action: `ASSIGN_DEPARTMENT`, Resource: `USER`
        - `DocumentUploadedEvent` -> Action: `UPLOAD_DOC`, Resource: `DOCUMENT`
        - `DocumentVersionCreatedEvent` -> Action: `CREATE_VERSION`, Resource: `DOCUMENT`
        - `DocumentAclUpdatedEvent` -> Action: `UPDATE_ACL`, Resource: `DOCUMENT`
        - `DocumentSoftDeletedEvent` -> Action: `DELETE_DOC`, Resource: `DOCUMENT`
      - Unit test `AuditEventListenerTest`.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/audit/application/listener/AuditEventListener.java`
      - `src/test/java/com/platform/app/audit/application/listener/AuditEventListenerTest.java`
    </allowed_files>
    <acceptance_criteria>
      - AC-4: Event listener handles all domain events asynchronously and triggers audit persistence.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests com.platform.app.audit.application.listener.AuditEventListenerTest
      ```
    </verifier>
    <expected_evidence>All listener event mappings verified with unit tests.</expected_evidence>
    <rollback_point>`git checkout -- backend/src/main/java/com/platform/app/audit/application/listener backend/src/test/java/com/platform/app/audit/application/listener`</rollback_point>
    <stop_conditions>Event mapping or listener failure.</stop_conditions>
  </slice>

  <slice id="S4">
    <objective>Create REST Controller `AuditLogController` and RBAC Security Tests</objective>
    <change>
      - Create `AuditLogController` exposing `GET /api/v1/audit-logs`.
      - Protected with `@PreAuthorize("hasAuthority('read:audit_logs') or hasAuthority('READ:AUDIT_LOGS') or hasRole('ADMIN') or hasRole('LEGAL_AUDITOR')")`.
      - Accepts query filters (`userId`, `action`, `resourceType`, `resourceId`, `status`, `startDate`, `endDate`) and `Pageable`.
      - Integration test `AuditLogControllerTest` testing:
        - 200 OK for ADMIN / LEGAL_AUDITOR.
        - 403 Forbidden for STAFF / unprivileged user.
        - 401 Unauthorized for unauthenticated requests.
        - Filter propagation.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/audit/infrastructure/adapters/primary/rest/AuditLogController.java`
      - `src/test/java/com/platform/app/audit/infrastructure/adapters/primary/rest/AuditLogControllerTest.java`
    </allowed_files>
    <acceptance_criteria>
      - AC-6: `GET /api/v1/audit-logs` returns paginated audit records.
      - AC-7: Endpoint is protected with RBAC (only admin and legal auditor).
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests com.platform.app.audit.infrastructure.adapters.primary.rest.AuditLogControllerTest
      ```
    </verifier>
    <expected_evidence>Controller mock MVC tests pass.</expected_evidence>
    <rollback_point>`git checkout -- backend/src/main/java/com/platform/app/audit/infrastructure/adapters/primary/rest backend/src/test/java/com/platform/app/audit/infrastructure/adapters/primary/rest`</rollback_point>
    <stop_conditions>Security or controller test failure.</stop_conditions>
  </slice>

  <slice id="S5">
    <objective>Execute Full Regression Test Suite and Code Quality Inspection</objective>
    <change>
      - Run `./gradlew test` across entire application.
      - Run `./gradlew check` (Spotless / code hygiene).
    </change>
    <allowed_files>
      - (No functional changes)
    </allowed_files>
    <acceptance_criteria>
      - AC-8: Full test suite passes.
      - AC-9: Zero regressions.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew check
      ./gradlew test
      ```
    </verifier>
    <expected_evidence>All tests green across iam, document, and audit packages.</expected_evidence>
    <rollback_point>Review git diff</rollback_point>
    <stop_conditions>Any test regression.</stop_conditions>
  </slice>

</slices>

---

## 5. Scope Contract

<scope_contract>
  <allowed_files>
    - `src/main/java/com/platform/app/audit/**`
    - `src/test/java/com/platform/app/audit/**`
  </allowed_files>
  <forbidden_files>
    - `src/main/java/com/platform/app/iam/**`
    - `src/main/java/com/platform/app/document/**`
    - `src/main/resources/**`
  </forbidden_files>
</scope_contract>

---

## 6. Verification Matrix

<verification_matrix>

| AC | Behavior | Test Class | Status |
|---|---|---|---|
| AC-1 | Domain model immutability | `AuditLogTest` | PLANNED |
| AC-2, AC-3 | JPA entity & repository adapter dynamic filter | `AuditLogRepositoryAdapterTest` | PLANNED |
| AC-4 | Asynchronous event listener mapping | `AuditEventListenerTest` | PLANNED |
| AC-5 | Application service use case methods | `AuditLoggingServiceTest` | PLANNED |
| AC-6, AC-7 | REST endpoint pagination & RBAC protection | `AuditLogControllerTest` | PLANNED |
| AC-8, AC-9 | Full regression test suite | `./gradlew test` | PLANNED |

</verification_matrix>

---

## 7. Gate 2 Checklist

<gate_2_checklist>
  - [x] Input artifacts complete (task.md, research.md, decision.md).
  - [x] Slices decomposed with defined objectives, verifiers, and rollback points.
  - [x] Scope contract (allowed/forbidden files) established.
  - [x] Verification matrix defined against all acceptance criteria.
  <approved_by>[PENDING APPROVAL: PAIR MODE]</approved_by>
  <approved_date></approved_date>
</gate_2_checklist>

</execution_plan>
