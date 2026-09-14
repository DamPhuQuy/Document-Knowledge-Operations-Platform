# Task: UC-IAM-03 Department Setup & Internal Employee Verification

<task_spec version="3.0" framework="RIPER-5">

<!-- ════════════════════════════════════════════
     SECTION 0 — TASK CONTROL (master state record)
     ════════════════════════════════════════════ -->
<task_control>
  <status>COMPLETED</status>
  <spec_level>S3</spec_level>
  <priority>P1</priority>
  <risk>MEDIUM</risk>
  <estimated_story_points>3</estimated_story_points>
  <working_mode>DELEGATED</working_mode>
  <current_phase>COMPLETED</current_phase>
  <owner>@engineer</owner>
  <decision_owner>@engineer</decision_owner>
  <created>2026-09-14</created>
  <last_updated>2026-09-14</last_updated>
</task_control>

---

## 1. Specification (Pillar 1: Task / Spec)

<specification>
  <goal>
    Configure organizational departments and manage user department assignment and internal employee verification status (`is_internal`) to establish default data isolation boundaries and document access controls, backed by referential integrity and audit logging via UC-AUDIT-01.
  </goal>

  <current_behavior>
    - Liquibase migration `002-create-iam-tables.yaml` defines `departments` table (with columns `id`, `code` unique, `name`, `description`, `created_at`, `updated_at`) and `users.department_id` FK (SET NULL on delete) and `users.is_internal` (boolean, default true).
    - `UserJpaEntity` has fields `departmentId` and `isInternal`.
    - `User` domain model has immutable fields `departmentId` and `internal` with no domain methods to reassign department or toggle `is_internal`.
    - No `Department` domain model exists in `iam/domain/model/`.
    - No `DepartmentJpaEntity` exists in `iam/infrastructure/adapters/secondary/persistence/entity/`.
    - No `DepartmentRepositoryPort` or `SpringDataDepartmentRepository` exists.
    - No application use case, command, service, or REST controller exists for department setup or user department assignment.
    - Baseline test suite currently has 16 failing tests in existing controllers/adapters due to `@UuidGenerator` on `RoleJpaEntity` / `UserJpaEntity` conflicting with manually assigned test IDs in `persist()`.
  </current_behavior>

  <expected_behavior>
    - System Administrator authenticated with `ROLE_ADMIN` can create departments (`POST /api/v1/departments`) with unique code, name, and optional description.
    - System Administrator can update existing departments (`PUT /api/v1/departments/{id}`).
    - System Administrator can retrieve a department by ID (`GET /api/v1/departments/{id}`) or list all departments (`GET /api/v1/departments`).
    - Department code must be validated to be uppercase alphanumeric (Business Rule B2: e.g. `HR`, `FIN`, `IT`, `LEGAL`).
    - Attempting to register a department with an existing code returns HTTP 409 Conflict (Alternative Path 2a).
    - System Administrator authenticated with `ROLE_ADMIN` can assign a user to a department and set `is_internal` flag (`PUT /api/v1/users/{userId}/department`).
    - Referential integrity: Department assignment validates that target department exists (and foreign key constraint `fk_users_department` guarantees integrity at database layer).
    - Data isolation rule B1: `is_internal` status is managed as a foundational boundary (users with `is_internal = FALSE` are marked external, governing downstream document access rules).
    - Mutations publish domain events (`DepartmentCreatedEvent`, `DepartmentUpdatedEvent`, `UserDepartmentAssignedEvent`) for UC-AUDIT-01 immutable audit logging.
    - Endpoints require `ROLE_ADMIN` authentication; unauthorized requests receive HTTP 401 or 403.
  </expected_behavior>

  <actor_authorization>
    System Administrator authenticated with `ROLE_ADMIN`.
  </actor_authorization>

  <invariants>
    - Department code must be unique across all departments.
    - Department code must conform to uppercase alphanumeric format (e.g. `HR`, `FIN`, `IT`, `LEGAL`).
    - Foreign key constraints must guarantee referential integrity (`users.department_id` references `departments.id`).
    - Clean Architecture boundaries: Domain models must remain decoupled from JPA and Spring frameworks.
    - All department mutations and user assignments must be auditable via domain events compatible with `UC-AUDIT-01`.
  </invariants>

  <out_of_scope>
    - Document access enforcement logic (`UC-DOC-*` enforcing Rule B1 when downloading/viewing documents).
    - Soft delete / hard delete of departments and subsequent user reassignment workflows.
    - Hierarchical multi-tier department structures (sub-departments / parent department trees).
  </out_of_scope>

  <acceptance_criteria>
    - [x] AC-1: Administrator (`ROLE_ADMIN`) can create a new department (`POST /api/v1/departments`) returning HTTP 201 Created and response containing generated department ID, code, name, and timestamps.
    - [x] AC-2: Department code must be validated as uppercase alphanumeric (Rule B2). Non-conforming codes reject with HTTP 400 Bad Request.
    - [x] AC-3: Attempting to create a department with a duplicate code returns HTTP 409 Conflict (Alternative Path 2a).
    - [x] AC-4: Administrator can update department details (`PUT /api/v1/departments/{id}`) returning HTTP 200 OK, rejecting duplicate codes with HTTP 409 Conflict.
    - [x] AC-5: Administrator can retrieve department by ID (`GET /api/v1/departments/{id}`) and list departments (`GET /api/v1/departments`).
    - [x] AC-6: Administrator can assign a user to a department and configure `is_internal` (`PUT /api/v1/users/{userId}/department`), updating `users.department_id` and `users.is_internal`.
    - [x] AC-7: Assigning user to a non-existent department returns HTTP 404 Not Found (or 400 Bad Request) maintaining referential integrity (NF1).
    - [x] AC-8: Audit events (`DepartmentCreatedEvent`, `DepartmentUpdatedEvent`, `UserDepartmentAssignedEvent`) are published upon successful mutations to fulfill `UC-AUDIT-01`.
    - [x] AC-9: Non-admin users attempting to manage departments or assign user department receive HTTP 403 Forbidden.
    - [x] AC-10: Automated unit and integration tests pass covering domain rules, use case services, persistence adapters, and REST endpoints.
  </acceptance_criteria>

  <!-- Definition-of-Ready (DoR) Gate -->
  <definition_of_ready>
    - [x] Target outcome and acceptance criteria are verifiable without guessing.
    - [x] Invariants and <out_of_scope> boundaries are explicit.
    - [x] Open questions resolved or scheduled in decision.md (No speculative coding).
  </definition_of_ready>
</specification>

---

## 2. Context Boundaries (Pillar 2: Context)

<context_boundaries>
  <target_files>
    - `src/main/java/com/platform/app/iam/domain/model/Department.java` — New Department domain model
    - `src/main/java/com/platform/app/iam/domain/model/User.java` — Department assignment domain logic
    - `src/main/java/com/platform/app/iam/domain/exception/DepartmentNotFoundException.java` — Exception for missing department
    - `src/main/java/com/platform/app/iam/domain/exception/DepartmentCodeConflictException.java` — Exception for duplicate code (409)
    - `src/main/java/com/platform/app/iam/domain/exception/InvalidDepartmentCodeException.java` — Exception for code format (400)
    - `src/main/java/com/platform/app/iam/application/ports/inbound/CreateDepartmentUseCase.java` — Inbound use case
    - `src/main/java/com/platform/app/iam/application/ports/inbound/UpdateDepartmentUseCase.java` — Inbound use case
    - `src/main/java/com/platform/app/iam/application/ports/inbound/GetDepartmentUseCase.java` — Inbound use case
    - `src/main/java/com/platform/app/iam/application/ports/inbound/AssignUserDepartmentUseCase.java` — Inbound use case
    - `src/main/java/com/platform/app/iam/application/ports/outbound/DepartmentRepositoryPort.java` — Outbound port
    - `src/main/java/com/platform/app/iam/application/dto/DepartmentResponseDto.java` — Output response DTO
    - `src/main/java/com/platform/app/iam/application/dto/UserDepartmentResponseDto.java` — Output response DTO
    - `src/main/java/com/platform/app/iam/application/dto/DepartmentCreatedEvent.java` — Audit event
    - `src/main/java/com/platform/app/iam/application/dto/DepartmentUpdatedEvent.java` — Audit event
    - `src/main/java/com/platform/app/iam/application/dto/UserDepartmentAssignedEvent.java` — Audit event
    - `src/main/java/com/platform/app/iam/application/services/DepartmentService.java` — Application service
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/entity/DepartmentJpaEntity.java` — JPA entity
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/repository/SpringDataDepartmentRepository.java` — Spring Data repository
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/adapter/DepartmentRepositoryAdapter.java` — Persistence adapter
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/DepartmentController.java` — REST endpoints for departments
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/UserDepartmentController.java` — REST endpoint for assigning user department
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java` — Map 409 and 404 responses
  </target_files>

  <context_groups>
    - planning
    - tests
    - iam
  </context_groups>

  <source_of_truth>
    <requirement>docs/specs/business/use_cases/01_iam_organization.md#UC-IAM-03</requirement>
    <architecture>docs/specs/database/modules/01_iam_organization.dbml</architecture>
    <existing_behavior>src/main/resources/db/changelog/changes/002-create-iam-tables.yaml</existing_behavior>
    <tests>src/test/java/com/platform/app/iam/</tests>
  </source_of_truth>
</context_boundaries>

---

## 3. Verification Strategy

<verification_strategy>

| AC / Risk | Evidence required | Verifier |
|---|---|---|
| AC-1 | 201 Created with persisted department payload | DepartmentControllerTest |
| AC-2 | 400 Bad Request on invalid department code | DepartmentTest / DepartmentControllerTest |
| AC-3 | 409 Conflict on duplicate code | DepartmentServiceTest / DepartmentControllerTest |
| AC-4 | 200 OK on department update | DepartmentControllerTest |
| AC-5 | 200 OK on get by id and list departments | DepartmentControllerTest |
| AC-6 | 200 OK on user department assignment with updated departmentId & isInternal | UserDepartmentControllerTest |
| AC-7 | 404 Not Found on invalid department ID | UserDepartmentControllerTest |
| AC-8 | Events published verified via ArgumentCaptor | DepartmentServiceTest |
| AC-9 | 403 Forbidden when invoked by non-admin | DepartmentControllerTest / UserDepartmentControllerTest |
| AC-10 | Full gradle build test passes | `./gradlew test` |

</verification_strategy>

---

## 4. Decisions

<decisions>
  <approved_decisions>
    <!-- Decisions recorded in decision.md -->
  </approved_decisions>

  <open_decisions>
    <!-- Decisions scheduled for INNOVATE phase -->
  </open_decisions>
</decisions>

---

## 5. RIPER-5 Execution Plan (Pillar 4: Loop)

<execution_plan>
  <phase name="Research" order="1">
    - [x] Ingest task spec, domain invariants, and out-of-scope boundaries.
    - [x] Read corresponding tests and port interfaces.
    - [x] Establish execution flow, boundaries, and source-of-truth conflicts.
    - [x] Produce/update `research.md` artifact.
    <gate id="G0" label="Research Complete">
      - [x] Current behavior understood and documented.
      - [x] Execution flow traced.
      - [x] No unresolved research blocker.
    </gate>
  </phase>

  <phase name="Innovate" order="2">
    <!-- READ-ONLY. Present 2-3 viable options. PAIR: engineer selects. DELEGATED: agent auto-selects optimal option. -->
    - [x] Generate 2–3 alternative approaches with trade-off matrix.
    - [x] Produce `decision.md` artifact.
    <gate id="G1" label="Gate 1 — Decision Approved">
      - [x] Options reviewed and trade-offs analyzed.
      - [x] Selected option recorded in `decision.md`.
      - [x] No blocking business/schema/security decision remains open.
      <approved_by>[AUTO: DELEGATED]</approved_by>
      <approved_date>2026-09-14</approved_date>
    </gate>
  </phase>

  <phase name="Plan" order="3">
    <!-- Plan artifacts only. No source-code changes. -->
    - [x] Decompose into vertical slices with verifiers and rollback points.
    - [x] Populate `plan.md` with scope contract and verification matrix.
    <gate id="G2" label="Gate 2 — Plan Approved">
      - [x] Every slice has a verifier.
      - [x] Allowed/forbidden file scope is defined.
      - [x] Rollback point defined per slice.
      - [x] Stop conditions defined.
      - [x] Plan approved.
      <approved_by>[AUTO: DELEGATED]</approved_by>
      <approved_date>2026-09-14</approved_date>
    </gate>
  </phase>

  <phase name="Execute" order="4">
    <!-- Read/write only within approved scope. One slice at a time. -->
    - [x] Implement each slice atomically.
    - [x] Run verifier after each slice.
    - [x] Inspect diff after each slice.
    - [x] Update `state.md` after each slice.
  </phase>

  <phase name="Review" order="5">
    <!-- READ-ONLY. May run verification commands. No code fixes during review. -->
    - [x] Review full diff, behavior, architecture, data, security, regression.
    - [x] Produce `review.md` with findings and verification matrix.
    <gate id="G3" label="Gate 3 — Review Passed">
      - [x] All AC verified with evidence.
      - [x] Residual risk accepted.
      - [x] Review decision: PASS.
      - [x] Ready for handoff.
      <approved_by>[AUTO: DELEGATED]</approved_by>
      <approved_date>2026-09-14</approved_date>
    </gate>
  </phase>
</execution_plan>

---

## 6. Guardrails & Escalation (Pillar 3 & 4: Harness)

<guardrails>
  <stop_conditions>
    - Missing business or policy decision.
    - Public API / DB schema change not declared in this spec.
    - New external dependency not declared in this spec.
    - Security policy change required.
    - Scope expansion beyond `<out_of_scope>`.
    - Retry budget exhausted on a recurring failure.
  </stop_conditions>

  <retry_budget max_attempts="3">
    Maximum 3 consecutive attempts per distinct failure symptom before halting.
    Never suppress errors with flags (e.g. `# type: ignore`, `eslint-disable`).
    If exhausted, log into `<open_decisions>` and halt.
  </retry_budget>
</guardrails>

</task_spec>
