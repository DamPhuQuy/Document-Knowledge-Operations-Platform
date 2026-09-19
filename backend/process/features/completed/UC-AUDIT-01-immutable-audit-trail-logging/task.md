# Task: [UC-AUDIT-01] Immutable Audit Trail Logging

<task_spec version="3.0" framework="RIPER-5">

<!-- ════════════════════════════════════════════
     SECTION 0 — TASK CONTROL (master state record)
     ════════════════════════════════════════════ -->
<task_control>
  <status>COMPLETED</status>  <!-- BACKLOG | ACTIVE | BLOCKED | PAUSED | REVIEW | COMPLETED | CANCELLED -->
  <spec_level>S3</spec_level>  <!-- S0=sketch | S1=defined | S2=verified | S3=locked -->
  <priority>P0</priority>  <!-- P0=urgent | P1=high | P2=normal | P3=low -->
  <risk>MEDIUM</risk>  <!-- LOW | MEDIUM | HIGH -->
  <estimated_story_points>3</estimated_story_points>  <!-- 1 SP ≈ 2-4 focused engineering hours -->
  <working_mode>DELEGATED</working_mode>  <!-- PAIR (default: halts at each gate for review) | DELEGATED (fast-track: continuous autonomous run) | MANUAL | DIAGNOSE-ONLY -->
  <current_phase>REVIEW</current_phase>  <!-- RESEARCH | INNOVATE | PLAN | EXECUTE | REVIEW -->
  <owner>@engineer</owner>
  <decision_owner>@engineer</decision_owner>
  <created>2026-09-19</created>
  <last_updated>2026-09-19</last_updated>
</task_control>

---

## 1. Specification (Pillar 1: Task / Spec)

<specification>
  <goal>
    Implement UC-AUDIT-01: Immutable Audit Trail Logging, creating an append-only audit subsystem in Bounded Context 3 (`com.platform.app.audit`) that asynchronously captures and records all security-sensitive events (authentication, role assignment, department management, document uploads, version creations, ACL modifications, and soft deletions) into the `audit_logs` PostgreSQL table, and exposes a secure, paginated, filtered audit log query endpoint (`GET /api/v1/audit-logs`) restricted to `ROLE_ADMIN`, `ROLE_LEGAL_AUDITOR`, or users with `read:audit_logs` authority.
  </goal>

  <current_behavior>
    - PostgreSQL schema changeset `004-create-audit-tables.yaml` exists and creates the `audit_logs` table with UUID `id`, `user_id`, `action`, `resource_type`, `resource_id`, `ip_address`, `user_agent`, `status`, `details` (JSONB), and `created_at`.
    - No Java classes, domain models, JPA entities, repositories, or services currently exist in `com.platform.app.audit`.
    - Domain events (`UserLoginSuccessEvent`, `UserLoginFailedEvent`, `UserRolesUpdatedEvent`, `DepartmentCreatedEvent`, `DocumentUploadedEvent`, `DocumentVersionCreatedEvent`, `DocumentAclUpdatedEvent`, `DocumentSoftDeletedEvent`) are already published across IAM and Document modules, but have no listener persisting them to `audit_logs`.
    - No REST API endpoint exists for querying audit logs (`GET /api/v1/audit-logs`).
  </current_behavior>

  <expected_behavior>
    - Dedicated clean architecture module `com.platform.app.audit` created with domain, application, and infrastructure layers.
    - Domain model `AuditLog` represents an immutable audit log entry.
    - Spring `@Async` event listener (`AuditEventListener`) subscribes to system-wide domain events and persists corresponding audit log entries with action, resource, user ID, client network metadata, and contextual details without blocking user requests.
    - Persistence layer (`AuditLogJpaEntity`, `SpringDataAuditLogRepository`, `AuditLogRepositoryAdapter`) supports saving audit log entries and executing dynamic filtered queries (date range, user ID, action, resource type, status) with pagination and sorting.
    - REST controller `AuditLogController` exposes `GET /api/v1/audit-logs`:
      - Accessible only to authenticated users with `ROLE_ADMIN`, `ROLE_LEGAL_AUDITOR`, or `read:audit_logs` authority.
      - Returns HTTP 401 Unauthorized for unauthenticated requests.
      - Returns HTTP 403 Forbidden for authenticated users lacking audit permissions.
      - Returns HTTP 200 OK with paginated `AuditLogResponseDto` results.
    - Invariant strictly enforced: Audit logs are APPEND-ONLY. No update or delete operations or endpoints are exposed.
  </expected_behavior>

  <actor_authorization>
    - Primary Actor: Audit Subsystem (`SYS-04`) via asynchronous event listener.
    - Secondary Actors: System Administrator (`ROLE_ADMIN`) and Compliance & Legal Auditor (`ROLE_LEGAL_AUDITOR`) for query operations.
    - Required Authority: `ROLE_ADMIN`, `ROLE_LEGAL_AUDITOR`, or `read:audit_logs`.
  </actor_authorization>

  <invariants>
    - B1: The `audit_logs` table is strictly APPEND-ONLY. No `UPDATE` or `DELETE` operations or API endpoints are permitted.
    - B2: Database roles assigned to application backends must not execute `DROP` or `TRUNCATE` on `audit_logs`.
    - NF1: Audit logging must execute asynchronously with zero latency penalty on main user request threads.
    - Clean Architecture: Domain model remains independent of database and framework concerns.
  </invariants>

  <out_of_scope>
    - Exporting audit logs to external SIEM tools or cold S3 storage (future enterprise phase).
    - Database-level read-only role provisioning or PostgreSQL row-level security (managed via infra / DBA scripts).
    - Frontend UI log viewer (managed in frontend workspace).
    - Modifying or deleting existing audit log records (strictly forbidden).
  </out_of_scope>

  <acceptance_criteria>
    - [ ] AC-1: Domain model `AuditLog` and value objects (`AuditStatus`, `AuditAction`) represent immutable audit entries.
    - [ ] AC-2: JPA entity `AuditLogJpaEntity` maps table `audit_logs` with JSONB `details` support.
    - [ ] AC-3: Repository adapter implements `AuditLogRepositoryPort` supporting append-only persistence and dynamic filtered queries (date range, user, action, resource).
    - [ ] AC-4: `AuditEventListener` listens asynchronously (`@Async`) to IAM and Document domain events and logs corresponding audit entries.
    - [ ] AC-5: Inbound ports `RecordAuditLogUseCase` and `GetAuditLogsUseCase` define application service boundaries.
    - [ ] AC-6: Expose `GET /api/v1/audit-logs` endpoint with pagination and filtering.
    - [ ] AC-7: Access to `GET /api/v1/audit-logs` is protected: only `ROLE_ADMIN`, `ROLE_LEGAL_AUDITOR`, or `read:audit_logs` can access; non-privileged users receive HTTP 403 Forbidden; unauthenticated receive HTTP 401 Unauthorized.
    - [ ] AC-8: Full unit and integration test coverage for services, repositories, event listeners, and controller.
    - [ ] AC-9: Build and existing tests pass with zero regressions (`./gradlew test`).
  </acceptance_criteria>

  <!-- Definition-of-Ready (DoR) Gate -->
  <definition_of_ready>
    - [x] Target outcome and acceptance criteria are verifiable without guessing.
    - [x] Invariants and <out_of_scope> boundaries are explicit.
    - [x] Open questions identified for INNOVATE phase in research.md.
  </definition_of_ready>
</specification>

---

## 2. Context Boundaries (Pillar 2: Context)

<context_boundaries>
  <target_files>
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
    - `src/main/java/com/platform/app/audit/infrastructure/adapters/secondary/persistence/adapter/AuditLogRepositoryAdapter.java`
    - `src/main/java/com/platform/app/audit/infrastructure/adapters/primary/rest/AuditLogController.java`
  </target_files>

  <context_groups>
    - `process/context/all-context.md`
    - `docs/specs/business/use_cases/03_audit_trail.md`
    - `docs/specs/business/02_actor_matrix_and_rbac.md`
    - `docs/specs/business/05_requirements_traceability_matrix.md`
    - `docs/specs/database/modules/07_audit_notifications.dbml`
    - `src/main/resources/db/changelog/changes/004-create-audit-tables.yaml`
  </context_groups>

  <source_of_truth>
    <requirement>`docs/specs/business/use_cases/03_audit_trail.md#UC-AUDIT-01`</requirement>
    <actor_rbac>`docs/specs/business/02_actor_matrix_and_rbac.md`</actor_rbac>
    <schema>`src/main/resources/db/changelog/changes/004-create-audit-tables.yaml`</schema>
  </source_of_truth>
</context_boundaries>

---

## 3. Verification Strategy

<verification_strategy>

| AC / Risk | Evidence required | Verifier |
|---|---|---|
| AC-1, AC-2 | Domain model & JPA entity map all fields cleanly | Unit tests |
| AC-3 | Repository adapter saves and queries with dynamic filters | Repository integration test |
| AC-4 | Asynchronous listener receives domain events and records audit logs | Listener unit test |
| AC-5, AC-6 | `GET /api/v1/audit-logs` returns filtered paginated logs | MockMvc controller test |
| AC-7 | Security checks: 401 unauthenticated, 403 unauthorized, 200 for admin/auditor | MockMvc security test |
| AC-8, AC-9 | Clean execution of test suite with zero regressions | `./gradlew test` |

</verification_strategy>

---

## 4. Decisions

<decisions>
  <approved_decisions>
    <!-- Recorded during INNOVATE phase -->
  </approved_decisions>

  <open_decisions>
    <!-- Scheduled for INNOVATE phase:
         1. Event handling mechanism: Standard Spring @EventListener + @Async vs @TransactionalEventListener(phase = AFTER_COMMIT).
         2. JSONB mapping approach: @JdbcTypeCode(SqlTypes.JSON) on String/Map vs custom Jackson converter.
         3. Dynamic filtering technique: Spring Data JPA Specifications vs QueryDSL / custom CriteriaBuilder. -->
  </open_decisions>
</decisions>

---

## 5. RIPER-5 Execution Plan (Pillar 4: Loop)

<execution_plan>
  <phase name="Research" order="1">
    - [x] Ingest task spec, domain invariants, and out-of-scope boundaries.
    - [x] Read database schemas, Liquibase migration `004-create-audit-tables.yaml`, and domain event catalog.
    - [x] Establish execution flows, boundaries, and source-of-truth analysis.
    - [x] Produce `research.md` artifact.
    <gate id="G0" label="Research Complete">
      - [x] Current behavior understood and documented.
      - [x] Execution flow traced.
      - [x] Source-of-truth analysis completed with zero unresolved conflicts.
      - [x] No unresolved research blocker.
      <approved_by>@engineer</approved_by>
      <approved_date>2026-09-19</approved_date>
    </gate>
  </phase>

  <phase name="Innovate" order="2">
    - [x] Generate 2–3 alternative approaches with trade-off matrix.
    - [x] Produce `decision.md` artifact.
    <gate id="G1" label="Gate 1 — Decision Approved">
      - [x] Options reviewed and trade-offs analyzed.
      - [x] Selected option recorded in `decision.md`.
      - [x] No blocking decision remains open.
      <approved_by>@engineer</approved_by>
      <approved_date>2026-09-19</approved_date>
    </gate>
  </phase>

  <phase name="Plan" order="3">
    - [x] Decompose into vertical slices with verifiers and rollback points.
    - [x] Populate `plan.md` with scope contract and verification matrix.
    <gate id="G2" label="Gate 2 — Plan Approved">
      - [x] Every slice has a verifier.
      - [x] Allowed/forbidden file scope is defined.
      - [x] Rollback point defined per slice.
      - [x] Stop conditions defined.
      - [x] Plan approved.
      <approved_by>@engineer</approved_by>
      <approved_date>2026-09-19</approved_date>
    </gate>
  </phase>

  <phase name="Execute" order="4">
    - [x] Implement each slice atomically.
    - [x] Run verifier after each slice.
    - [x] Inspect diff after each slice.
    - [x] Update `state.md` with evidence after each slice.
    <gate id="Execute Complete" label="All Slices Passed">
      - [x] All slices verified.
      - [x] Zero test regressions.
      - [x] Allowed files scope strictly maintained.
    </gate>
  </phase>

  <phase name="Review" order="5">
    - [x] Review implementation against invariants and acceptance criteria.
    - [x] Run `./gradlew check` and `./gradlew test`.
    - [x] Produce `review.md` artifact.
    <gate id="G3" label="Gate 3 — Review Passed">
      - [x] All tests passing.
      - [x] Clean git diff, no debug logs or leftover files.
      - [x] Review gate signed.
      <approved_by>[AUTO: DELEGATED]</approved_by>
      <approved_date>2026-09-19</approved_date>
    </gate>
  </phase>
</execution_plan>

</task_spec>
