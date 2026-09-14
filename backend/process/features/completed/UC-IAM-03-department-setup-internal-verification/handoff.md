# Handoff: UC-IAM-03 Department Setup & Internal Employee Verification

<handoff task_id="UC-IAM-03" version="2.0" framework="RIPER-5">

<!-- Final projection. Short. Do not duplicate research/plan/review artifacts. -->
<!-- Answer: What changed? Why? What proves it? What remains risky? -->

<handoff_status>
  <review_decision>PASS</review_decision>
  <review_artifact>process/features/active/UC-IAM-03-department-setup-internal-verification/review.md</review_artifact>
  <completed_date>2026-09-14</completed_date>
</handoff_status>

---

## 1. What Changed

<what_changed>
  Implemented organizational department configuration and internal employee verification management (UC-IAM-03) under Hexagonal / Clean Architecture.
</what_changed>

<main_changes>
  - `src/main/java/com/platform/app/iam/domain/model/Department.java` — Department domain model with Rule B2 uppercase alphanumeric code validation.
  - `src/main/java/com/platform/app/iam/domain/model/User.java` — Added `assignDepartment(UUID, boolean)` method.
  - `src/main/java/com/platform/app/iam/domain/exception/` — Added `DepartmentCodeConflictException`, `DepartmentNotFoundException`, and `InvalidDepartmentCodeException`.
  - `src/main/java/com/platform/app/iam/application/ports/inbound/` — Inbound use cases (`CreateDepartmentUseCase`, `UpdateDepartmentUseCase`, `GetDepartmentUseCase`, `AssignUserDepartmentUseCase`) and commands.
  - `src/main/java/com/platform/app/iam/application/ports/outbound/DepartmentRepositoryPort.java` — Outbound repository port.
  - `src/main/java/com/platform/app/iam/application/dto/` — Response DTOs and audit events (`DepartmentCreatedEvent`, `DepartmentUpdatedEvent`, `UserDepartmentAssignedEvent`) for UC-AUDIT-01.
  - `src/main/java/com/platform/app/iam/application/services/DepartmentService.java` — Core application service with duplicate code checking and transactional persistence.
  - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/` — JPA entity `DepartmentJpaEntity`, `SpringDataDepartmentRepository`, and `DepartmentRepositoryAdapter`.
  - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/` — REST controllers `DepartmentController` (`/api/v1/departments`) and `UserDepartmentController` (`/api/v1/users/{userId}/department`), plus request DTOs and error handlers in `RestExceptionHandler`.
  - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/entity/` — Removed `@UuidGenerator` on `RoleJpaEntity` and `UserJpaEntity` to allow assigned IDs and stabilize test persistence fixtures.
  - Tests — Comprehensive suite in `DepartmentTest`, `UserTest`, `DepartmentServiceTest`, `DepartmentRepositoryAdapterTest`, `DepartmentControllerTest`, and `UserDepartmentControllerTest`.
</main_changes>

---

## 2. Why

<why>
  UC-IAM-03 establishes fundamental organizational boundaries by allowing administrators to configure departments and classify users as internal vs external employees (`is_internal`), laying the prerequisite foundation for document data isolation (Rule B1).
</why>

---

## 3. What Proves It

<evidence>

| AC | Verifier | Result |
|---|---|---|
| AC-1 | `DepartmentControllerTest#createDepartment_Success` | PASS |
| AC-2 | `DepartmentTest#shouldRejectInvalidCodes`, `DepartmentControllerTest#createDepartment_InvalidCode` | PASS |
| AC-3 | `DepartmentControllerTest#createDepartment_DuplicateCode` | PASS |
| AC-4 | `DepartmentControllerTest#updateDepartment_Success` | PASS |
| AC-5 | `DepartmentControllerTest#getDepartmentById_Success`, `listDepartments_Success` | PASS |
| AC-6 | `UserDepartmentControllerTest#assignDepartment_Success` | PASS |
| AC-7 | `UserDepartmentControllerTest#assignDepartment_DepartmentNotFound` | PASS |
| AC-8 | `DepartmentServiceTest` ArgumentCaptor events | PASS |
| AC-9 | `DepartmentControllerTest#nonAdminAccess_Forbidden`, `UserDepartmentControllerTest#assignDepartment_NonAdminForbidden` | PASS |
| AC-10 | `./gradlew check` | PASS |

<!-- To reproduce: -->
```bash
./gradlew check
```

</evidence>

---

## 4. What Remains Risky

<residual_risk>
  - Rule B1 downstream access control (preventing non-internal users from reading `INTERNAL` or `RESTRICTED` documents) is verified here at the IAM data classification boundary and will be enforced at document operations in the Document Management module (`UC-DOC-*`).
</residual_risk>

---

## 5. Decisions & Assumptions

<decisions_and_assumptions>
  - Option A from DEC-IAM-03: Separated `DepartmentController` and `UserDepartmentController` to ensure RESTful resource semantics matching existing patterns.
  - Assigned ID strategy adopted across JPA entities to maintain domain purity and prevent Hibernate detached entity collisions.
</decisions_and_assumptions>

---

## 6. Next Action

<next_action>
  NONE. Feature implementation, verification, and audit trail events are 100% complete and tested.
</next_action>

</handoff>
