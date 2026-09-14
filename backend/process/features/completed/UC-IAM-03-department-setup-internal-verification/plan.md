# Plan: PLAN-IAM-03 Department Setup & Internal Employee Verification

<execution_plan task_id="UC-IAM-03" plan_id="PLAN-IAM-03" version="2.0" framework="RIPER-5">

<!-- PLAN PHASE. Plan artifacts only. No source-code changes. -->
<plan_status>
  <phase>PLAN</phase>
  <last_updated>2026-09-14</last_updated>
</plan_status>

---

## 1. Input Artifacts

<input_artifacts>
  <task_spec>process/features/active/UC-IAM-03-department-setup-internal-verification/task.md</task_spec>
  <research>process/features/active/UC-IAM-03-department-setup-internal-verification/research.md</research>
  <decision>process/features/active/UC-IAM-03-department-setup-internal-verification/decision.md — DEC-IAM-03, Option A</decision>
</input_artifacts>

---

## 2. Execution Constraints

<execution_constraints>
  <allowed_files>
    - `src/main/java/com/platform/app/iam/domain/model/Department.java`
    - `src/main/java/com/platform/app/iam/domain/model/User.java`
    - `src/main/java/com/platform/app/iam/domain/exception/DepartmentNotFoundException.java`
    - `src/main/java/com/platform/app/iam/domain/exception/DepartmentCodeConflictException.java`
    - `src/main/java/com/platform/app/iam/domain/exception/InvalidDepartmentCodeException.java`
    - `src/main/java/com/platform/app/iam/application/ports/inbound/CreateDepartmentUseCase.java`
    - `src/main/java/com/platform/app/iam/application/ports/inbound/UpdateDepartmentUseCase.java`
    - `src/main/java/com/platform/app/iam/application/ports/inbound/GetDepartmentUseCase.java`
    - `src/main/java/com/platform/app/iam/application/ports/inbound/AssignUserDepartmentUseCase.java`
    - `src/main/java/com/platform/app/iam/application/ports/inbound/CreateDepartmentCommand.java`
    - `src/main/java/com/platform/app/iam/application/ports/inbound/UpdateDepartmentCommand.java`
    - `src/main/java/com/platform/app/iam/application/ports/inbound/AssignUserDepartmentCommand.java`
    - `src/main/java/com/platform/app/iam/application/dto/DepartmentResponseDto.java`
    - `src/main/java/com/platform/app/iam/application/dto/UserDepartmentResponseDto.java`
    - `src/main/java/com/platform/app/iam/application/dto/DepartmentCreatedEvent.java`
    - `src/main/java/com/platform/app/iam/application/dto/DepartmentUpdatedEvent.java`
    - `src/main/java/com/platform/app/iam/application/dto/UserDepartmentAssignedEvent.java`
    - `src/main/java/com/platform/app/iam/application/ports/outbound/DepartmentRepositoryPort.java`
    - `src/main/java/com/platform/app/iam/application/services/DepartmentService.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/entity/DepartmentJpaEntity.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/entity/RoleJpaEntity.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/entity/UserJpaEntity.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/repository/SpringDataDepartmentRepository.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/adapter/DepartmentRepositoryAdapter.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/DepartmentController.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/UserDepartmentController.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/CreateDepartmentRequest.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/UpdateDepartmentRequest.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AssignUserDepartmentRequest.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java`
    - `src/test/java/com/platform/app/iam/domain/model/DepartmentTest.java`
    - `src/test/java/com/platform/app/iam/domain/model/UserTest.java`
    - `src/test/java/com/platform/app/iam/application/services/DepartmentServiceTest.java`
    - `src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/adapter/DepartmentRepositoryAdapterTest.java`
    - `src/test/java/com/platform/app/iam/infrastructure/adapters/primary/rest/DepartmentControllerTest.java`
    - `src/test/java/com/platform/app/iam/infrastructure/adapters/primary/rest/UserDepartmentControllerTest.java`
    - `process/features/active/UC-IAM-03-department-setup-internal-verification/*`
  </allowed_files>
  <forbidden_files>
    - `src/main/resources/db/changelog/**` — Existing schema already provisions `departments` and `users.department_id`; no DB migrations permitted.
    - `build.gradle` — No dependency modifications permitted.
  </forbidden_files>
  <allowed_commands>
    - `./gradlew compileJava`
    - `./gradlew test --tests ...`
    - `./gradlew test`
    - `./gradlew check`
  </allowed_commands>
  <restricted_operations>
    - No DB schema changes or migration file modifications.
    - No changes outside IAM bounded context.
  </restricted_operations>
  <required_approvals>
    - None (fast-track DELEGATED mode active).
  </required_approvals>
</execution_constraints>

---

## 3. Slice Summary

<slice_summary>

| ID | Behavior | Boundary | Files | AC | Verifier | Risk | Mode | Rollback |
|---|---|---|---|---|---|---|---|---|
| S0 | Entity Generator Harness Calibration | Persistence / Harness | `RoleJpaEntity.java`, `UserJpaEntity.java` | Harness Health | `./gradlew test --tests "*PersistenceAdaptersTest"` | LOW | EXECUTE | git checkout |
| S1 | Department Domain Model & Invariants | Domain Layer | `Department.java`, `User.java`, exceptions, tests | AC-2, AC-6 | `./gradlew test --tests "*DepartmentTest" --tests "*UserTest"` | LOW | EXECUTE | git checkout |
| S2 | Persistence Port & Adapter | Persistence Layer | `DepartmentJpaEntity.java`, `DepartmentRepositoryPort.java`, `SpringDataDepartmentRepository.java`, `DepartmentRepositoryAdapter.java` | NF1, AC-1, AC-5 | `./gradlew test --tests "*DepartmentRepositoryAdapterTest"` | LOW | EXECUTE | git checkout |
| S3 | Application Services, Use Cases & Audit Events | Application Layer | Inbound ports, DTOs, events, `DepartmentService.java` | AC-1, AC-3, AC-4, AC-5, AC-6, AC-7, AC-8 | `./gradlew test --tests "*DepartmentServiceTest"` | MEDIUM | EXECUTE | git checkout |
| S4 | Primary REST Adapters & Exception Handling | Primary Web Layer | `DepartmentController.java`, `UserDepartmentController.java`, requests, `RestExceptionHandler.java` | AC-1, AC-2, AC-3, AC-4, AC-5, AC-6, AC-7, AC-9 | `./gradlew test --tests "*DepartmentControllerTest" --tests "*UserDepartmentControllerTest"` | MEDIUM | EXECUTE | git checkout |
| S5 | Full Build & Regression Harness Verification | Full Subsystem | Entire test suite | AC-10 | `./gradlew check` | LOW | EXECUTE | git checkout |

</slice_summary>

---

## 4. Slice Details

<slices>

  <slice id="S0">
    <objective>Calibrate entity identifier generation on RoleJpaEntity and UserJpaEntity so that pre-assigned UUIDs in test fixtures do not trigger PersistentObjectException.</objective>
    <change>Adjust ID mapping on `RoleJpaEntity` and `UserJpaEntity` to allow assigned IDs.</change>
    <allowed_files>
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/entity/RoleJpaEntity.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/entity/UserJpaEntity.java`
    </allowed_files>
    <acceptance_criteria>
      - Baseline persistence tests and controller tests pass.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "com.platform.app.iam.infrastructure.adapters.secondary.persistence.adapter.PersistenceAdaptersTest" --tests "com.platform.app.iam.infrastructure.adapters.primary.rest.AuthControllerTest" --tests "com.platform.app.iam.infrastructure.adapters.primary.rest.UserRoleControllerTest"
      ```
    </verifier>
    <expected_evidence>All baseline tests pass successfully.</expected_evidence>
    <rollback_point>git checkout HEAD -- src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/entity/</rollback_point>
    <stop_conditions>
      - Regression in existing domain mappings.
    </stop_conditions>
  </slice>

  <slice id="S1">
    <objective>Implement Department domain model with Rule B2 uppercase alphanumeric validation and enrich User domain model with department assignment logic.</objective>
    <change>Create `Department.java`, domain exceptions, update `User.java`, and create unit tests.</change>
    <allowed_files>
      - `src/main/java/com/platform/app/iam/domain/model/Department.java`
      - `src/main/java/com/platform/app/iam/domain/model/User.java`
      - `src/main/java/com/platform/app/iam/domain/exception/DepartmentNotFoundException.java`
      - `src/main/java/com/platform/app/iam/domain/exception/DepartmentCodeConflictException.java`
      - `src/main/java/com/platform/app/iam/domain/exception/InvalidDepartmentCodeException.java`
      - `src/test/java/com/platform/app/iam/domain/model/DepartmentTest.java`
      - `src/test/java/com/platform/app/iam/domain/model/UserTest.java`
    </allowed_files>
    <acceptance_criteria>
      - AC-2: Department code validates format (`^[A-Z0-9_]+$`).
      - AC-6: User domain model updates `departmentId` and `internal` status with timestamp.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "com.platform.app.iam.domain.model.DepartmentTest" --tests "com.platform.app.iam.domain.model.UserTest"
      ```
    </verifier>
    <expected_evidence>Tests for Department and User domain invariants pass.</expected_evidence>
    <rollback_point>git checkout HEAD -- src/main/java/com/platform/app/iam/domain/ src/test/java/com/platform/app/iam/domain/</rollback_point>
    <stop_conditions>
      - Violation of domain purity.
    </stop_conditions>
  </slice>

  <slice id="S2">
    <objective>Implement Department JPA entity, Spring Data Department Repository, DepartmentRepositoryPort, and DepartmentRepositoryAdapter.</objective>
    <change>Create persistence entity, repository port, Spring Data repository, adapter, and integration test.</change>
    <allowed_files>
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/entity/DepartmentJpaEntity.java`
      - `src/main/java/com/platform/app/iam/application/ports/outbound/DepartmentRepositoryPort.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/repository/SpringDataDepartmentRepository.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/adapter/DepartmentRepositoryAdapter.java`
      - `src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/adapter/DepartmentRepositoryAdapterTest.java`
    </allowed_files>
    <acceptance_criteria>
      - NF1: Foreign key referential integrity supported.
      - AC-1, AC-5: Department save, findById, findByCode, existsByCode, and findAll function properly.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "com.platform.app.iam.infrastructure.adapters.secondary.persistence.adapter.DepartmentRepositoryAdapterTest"
      ```
    </verifier>
    <expected_evidence>Persistence tests pass confirming entity mapping and repository methods.</expected_evidence>
    <rollback_point>git checkout HEAD -- src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/ src/main/java/com/platform/app/iam/application/ports/outbound/</rollback_point>
    <stop_conditions>
      - SQL / mapping syntax errors.
    </stop_conditions>
  </slice>

  <slice id="S3">
    <objective>Implement Application Use Cases, Inbound Commands, DTOs, Audit Events, and DepartmentService.</objective>
    <change>Define inbound ports, DTOs, events, and implement DepartmentService with event publishing and duplicate code checks.</change>
    <allowed_files>
      - `src/main/java/com/platform/app/iam/application/ports/inbound/CreateDepartmentUseCase.java`
      - `src/main/java/com/platform/app/iam/application/ports/inbound/UpdateDepartmentUseCase.java`
      - `src/main/java/com/platform/app/iam/application/ports/inbound/GetDepartmentUseCase.java`
      - `src/main/java/com/platform/app/iam/application/ports/inbound/AssignUserDepartmentUseCase.java`
      - `src/main/java/com/platform/app/iam/application/ports/inbound/CreateDepartmentCommand.java`
      - `src/main/java/com/platform/app/iam/application/ports/inbound/UpdateDepartmentCommand.java`
      - `src/main/java/com/platform/app/iam/application/ports/inbound/AssignUserDepartmentCommand.java`
      - `src/main/java/com/platform/app/iam/application/dto/DepartmentResponseDto.java`
      - `src/main/java/com/platform/app/iam/application/dto/UserDepartmentResponseDto.java`
      - `src/main/java/com/platform/app/iam/application/dto/DepartmentCreatedEvent.java`
      - `src/main/java/com/platform/app/iam/application/dto/DepartmentUpdatedEvent.java`
      - `src/main/java/com/platform/app/iam/application/dto/UserDepartmentAssignedEvent.java`
      - `src/main/java/com/platform/app/iam/application/services/DepartmentService.java`
      - `src/test/java/com/platform/app/iam/application/services/DepartmentServiceTest.java`
    </allowed_files>
    <acceptance_criteria>
      - AC-1: Department creation service logic.
      - AC-3: Duplicate code throws DepartmentCodeConflictException.
      - AC-4: Department update logic.
      - AC-6: User assignment updates department and is_internal.
      - AC-7: Non-existent department throws DepartmentNotFoundException.
      - AC-8: Audit events published via eventPublisher.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "com.platform.app.iam.application.services.DepartmentServiceTest"
      ```
    </verifier>
    <expected_evidence>DepartmentServiceTest passes with 100% assertions satisfied.</expected_evidence>
    <rollback_point>git checkout HEAD -- src/main/java/com/platform/app/iam/application/</rollback_point>
    <stop_conditions>
      - Service failure or event publishing defect.
    </stop_conditions>
  </slice>

  <slice id="S4">
    <objective>Implement REST Controllers, Request DTOs, and RestExceptionHandler mappings for Department endpoints and User Department assignment.</objective>
    <change>Create DepartmentController, UserDepartmentController, request DTOs, and update RestExceptionHandler.</change>
    <allowed_files>
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/DepartmentController.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/UserDepartmentController.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/CreateDepartmentRequest.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/UpdateDepartmentRequest.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AssignUserDepartmentRequest.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java`
      - `src/test/java/com/platform/app/iam/infrastructure/adapters/primary/rest/DepartmentControllerTest.java`
      - `src/test/java/com/platform/app/iam/infrastructure/adapters/primary/rest/UserDepartmentControllerTest.java`
    </allowed_files>
    <acceptance_criteria>
      - AC-1: POST /api/v1/departments returns 201 Created.
      - AC-2: Invalid code returns 400 Bad Request.
      - AC-3: Duplicate code returns 409 Conflict.
      - AC-4: PUT /api/v1/departments/{id} returns 200 OK.
      - AC-5: GET /api/v1/departments/{id} returns 200 OK.
      - AC-6: PUT /api/v1/users/{userId}/department returns 200 OK.
      - AC-7: Missing department returns 404 Not Found.
      - AC-9: Non-admin caller receives 403 Forbidden.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "com.platform.app.iam.infrastructure.adapters.primary.rest.DepartmentControllerTest" --tests "com.platform.app.iam.infrastructure.adapters.primary.rest.UserDepartmentControllerTest"
      ```
    </verifier>
    <expected_evidence>All controller integration tests pass with correct status codes.</expected_evidence>
    <rollback_point>git checkout HEAD -- src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/</rollback_point>
    <stop_conditions>
      - Security or HTTP mapping failures.
    </stop_conditions>
  </slice>

  <slice id="S5">
    <objective>Run complete regression test suite and verify AC-10.</objective>
    <change>Ensure entire test suite passes.</change>
    <allowed_files>
      - `process/features/active/UC-IAM-03-department-setup-internal-verification/*`
    </allowed_files>
    <acceptance_criteria>
      - AC-10: All tests in the project pass cleanly.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew check
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with 0 failures.</expected_evidence>
    <rollback_point>git status inspection</rollback_point>
    <stop_conditions>
      - Failing regression tests.
    </stop_conditions>
  </slice>

</slices>

---

## 5. Scope Contract

<scope_contract>
  <allowed>
    - Creation of new Department domain model, ports, services, JPA entities, adapters, DTOs, events, and controllers.
    - Addition of `assignDepartment` to `User.java`.
    - Addition of exception handlers in `RestExceptionHandler.java`.
    - Calibration of `@Id` in `RoleJpaEntity.java` and `UserJpaEntity.java` for test fixture compatibility.
    - Creation of corresponding unit and integration tests.
    - Updating process tracking artifacts (`task.md`, `state.md`, `review.md`, `handoff.md`).
  </allowed>
  <forbidden>
    - No changes to Liquibase changelogs (`src/main/resources/db/changelog/**`).
    - No changes to build configurations (`build.gradle`, `settings.gradle`).
    - No modifications to document or audit bounded contexts.
  </forbidden>
</scope_contract>

---

## 6. Verification Matrix

<verification_matrix>

| AC / Risk | Test / Command | Expected Evidence | Strictness | Actual Result |
|---|---|---|---|---|
| AC-1 | `DepartmentControllerTest#createDepartment_Success` | HTTP 201 with body | hard-mandatory | PENDING |
| AC-2 | `DepartmentTest#invalidCodeThrowsException`, `DepartmentControllerTest#createDepartment_InvalidCode` | HTTP 400 | hard-mandatory | PENDING |
| AC-3 | `DepartmentControllerTest#createDepartment_DuplicateCode` | HTTP 409 | hard-mandatory | PENDING |
| AC-4 | `DepartmentControllerTest#updateDepartment_Success` | HTTP 200 | hard-mandatory | PENDING |
| AC-5 | `DepartmentControllerTest#getDepartmentById_Success`, `listDepartments_Success` | HTTP 200 | hard-mandatory | PENDING |
| AC-6 | `UserDepartmentControllerTest#assignDepartment_Success` | HTTP 200 with updated fields | hard-mandatory | PENDING |
| AC-7 | `UserDepartmentControllerTest#assignDepartment_DepartmentNotFound` | HTTP 404 | hard-mandatory | PENDING |
| AC-8 | `DepartmentServiceTest#publishesAuditEvents` | Events captured | hard-mandatory | PENDING |
| AC-9 | `DepartmentControllerTest#unauthorizedAccessForbidden` | HTTP 403 | hard-mandatory | PENDING |
| AC-10 | `./gradlew check` | BUILD SUCCESSFUL | hard-mandatory | PENDING |

</verification_matrix>

---

## Gate 2 — Plan Approved

<gate id="G2">
  - [x] Every slice has a defined verifier.
  - [x] Scope contract (allowed / forbidden) approved.
  - [x] Enforcement strictness assigned per AC/verification item.
  - [x] Stop conditions defined per slice.
  - [x] Rollback point defined per slice.
  - [x] Allowed commands listed.
  - [x] Plan approved.
  <approved_by>[AUTO: DELEGATED]</approved_by>
  <approved_date>2026-09-14</approved_date>
</gate>

</execution_plan>
