# Research: UC-IAM-03 Department Setup & Internal Employee Verification

<research_context task_id="UC-IAM-03" version="3.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. No source-code modifications. No implementation decisions. -->
<research_status>
  <phase>RESEARCH</phase>
  <mode>PAIR</mode>
  <research_owner>@engineer</research_owner>
  <last_updated>2026-09-14</last_updated>
</research_status>

---

## 1. Specification Analysis & Business Requirements

<business_spec source="docs/specs/business/use_cases/01_iam_organization.md#UC-IAM-03">
  - **Goal**: Configure organizational departments and manage user `department_id` and `is_internal` status to establish default data isolation boundaries.
  - **Actors**: System Administrator (`ROLE_ADMIN`) (primary), IAM Subsystem (secondary).
  - **Includes**: `UC-AUDIT-01` (Immutable Audit Trail Logging).
  - **Pre-Conditions**:
    1. Administrator is authenticated with `ROLE_ADMIN`.
  - **Post-Conditions**:
    1. Department record created or updated in `departments`.
    2. User's `department_id` and `is_internal` flag updated in `users`.
    3. Audit log entry committed via `UC-AUDIT-01`.
  - **Basic Path**:
    1. Administrator submits department details (code, name, description).
    2. System validates that department code is unique.
    3. System saves department record in `departments`.
    4. Administrator assigns users to the department and sets `is_internal = TRUE/FALSE`.
    5. System persists user updates and invokes `UC-AUDIT-01` to record the audit log.
  - **Alternative Paths**:
    - 2a. Duplicate department code: System returns HTTP 409 Conflict.
  - **Business Rules**:
    - **B1**: Users with `is_internal = FALSE` cannot access `INTERNAL` or `RESTRICTED` documents.
    - **B2**: Department code must be uppercase alphanumeric (e.g., `HR`, `FIN`, `IT`, `LEGAL`).
  - **Non-Functional Requirements**:
    - **NF1**: Database foreign key constraints must guarantee referential integrity (`users.department_id` references `departments.id`).
</business_spec>

---

## 2. Current Codebase State & Architectural Baseline

<codebase_baseline>

### A. Database Schema Baseline (`src/main/resources/db/changelog/changes/002-create-iam-tables.yaml`)
- Table `departments`:
  - `id` UUID PK (`pk_departments`)
  - `code` VARCHAR(50) NOT NULL UNIQUE (`uk_departments_code`)
  - `name` VARCHAR(255) NOT NULL
  - `description` TEXT
  - `created_at` TIMESTAMPTZ NOT NULL
  - `updated_at` TIMESTAMPTZ NOT NULL
  - Index `idx_departments_code` on `departments(code)`
- Table `users`:
  - `department_id` UUID with foreign key `fk_users_department` references `departments(id)` ON DELETE SET NULL
  - `is_internal` BOOLEAN NOT NULL DEFAULT true
- Table `audit_logs` (`004-create-audit-tables.yaml`):
  - Created to support append-only audit trail for `UC-AUDIT-01`.

### B. Domain Models (`src/main/java/com/platform/app/iam/domain/model/`)
- `User.java`:
  - Contains `UUID id`, `String email`, `String fullName`, `UUID departmentId`, `boolean enabled`, `boolean internal`, `Set<Role> roles`.
  - `departmentId` and `internal` are `final` with no mutation methods.
  - Needs a domain method (e.g. `assignDepartment(UUID departmentId, boolean internal)`) to update department and internal status while upholding domain encapsulation and updating `updatedAt`.
- `Department.java`:
  - Does NOT exist yet in `domain/model/`.
  - Needs to be created with properties (`id`, `code`, `name`, `description`, `createdAt`, `updatedAt`) and validation for Rule B2 (uppercase alphanumeric validation).

### C. Persistence Layer (`src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/`)
- `UserJpaEntity.java`:
  - Already contains `@Column(name = "department_id") private UUID departmentId;` and `@Column(name = "is_internal", nullable = false) private boolean isInternal;`.
- `UserRepositoryAdapter.java`:
  - Already maps `departmentId` and `isInternal` in `save(User user)` and `toDomain(UserJpaEntity entity)`.
- `DepartmentJpaEntity.java`:
  - Does NOT exist yet. Needs to be created mapping `departments` table.
- `SpringDataDepartmentRepository.java`:
  - Does NOT exist yet. Needs methods: `findByCodeIgnoreCase`, `existsByCodeIgnoreCase`, `existsByCodeIgnoreCaseAndIdNot`.
- `DepartmentRepositoryPort.java`:
  - Does NOT exist yet. Outbound port needed with methods for `save`, `findById`, `findByCode`, `existsByCode`, `findAll`.
- `DepartmentRepositoryAdapter.java`:
  - Does NOT exist yet. Needs to implement `DepartmentRepositoryPort`.

### D. Application Layer & Audit Logging (`UC-AUDIT-01`)
- In `UC-IAM-01` and `UC-IAM-02`, the IAM module publishes domain events (`UserLoginSuccessEvent`, `UserRolesUpdatedEvent`) via Spring's `ApplicationEventPublisher`.
- For `UC-IAM-03`, we need domain events:
  - `DepartmentCreatedEvent(UUID departmentId, String code, String name, UUID operatorUserId, Instant timestamp)`
  - `DepartmentUpdatedEvent(UUID departmentId, String code, String name, UUID operatorUserId, Instant timestamp)`
  - `UserDepartmentAssignedEvent(UUID targetUserId, UUID departmentId, boolean isInternal, UUID operatorUserId, Instant timestamp)`
- Inbound Use Case Ports:
  - `CreateDepartmentUseCase`, `UpdateDepartmentUseCase`, `GetDepartmentUseCase`, `AssignUserDepartmentUseCase`.
- Application Service:
  - `DepartmentService.java` implementing the use cases.

### E. Primary Adapters & Security (`src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/`)
- `RestExceptionHandler.java`:
  - Currently handles `MethodArgumentNotValidException` (400), `InvalidCredentialsException` (401), `AccountDisabledException` (403), `AccountLockedException` (423), `IllegalArgumentException` (400), `AccessDeniedException` (403).
  - Needs mapping for `DepartmentCodeConflictException` (409 Conflict) and `DepartmentNotFoundException` (404 Not Found).
- `SecurityConfig.java`:
  - Configured with `@EnableMethodSecurity`.
  - Administrator endpoints require `@PreAuthorize("hasRole('ADMIN')")`.

### F. Observed Test Harness Baseline State
- Running `./gradlew test` identified 16 failing tests across `AuthControllerTest`, `UserRoleControllerTest`, and `PersistenceAdaptersTest`.
- Cause: `RoleJpaEntity` and `UserJpaEntity` have `@UuidGenerator(style = UuidGenerator.Style.VERSION_7)` on their `@Id` field, while the test fixtures manually set `.id(UUID.randomUUID())` and then invoke `entityManager.persist()`. In Hibernate 6.5+, calling `persist()` with a pre-set ID on an entity configured with an identifier generator causes Hibernate to consider the entity detached, throwing `jakarta.persistence.EntityExistsException` / `PersistentObjectException: detached entity passed to persist`.
- This finding is documented under the evidence matrix as an observed harness baseline consideration.

</codebase_baseline>

---

## 3. Evidence Classification

<evidence_matrix>

| Item | Finding / Assumption | Classification | Source / Verification |
|---|---|---|---|
| E-1 | Table `departments` exists with unique constraint `uk_departments_code` and index `idx_departments_code` | CONFIRMED | `002-create-iam-tables.yaml` line 3-48 |
| E-2 | Table `users` has `department_id` referencing `departments(id)` with ON DELETE SET NULL, and `is_internal` boolean | CONFIRMED | `002-create-iam-tables.yaml` line 80-95 |
| E-3 | `UserJpaEntity` and `UserRepositoryAdapter` already support `departmentId` and `isInternal` mapping | CONFIRMED | `UserJpaEntity.java` line 49-56, `UserRepositoryAdapter.java` line 80-82 |
| E-4 | `Department` domain model, JPA entity, repository port, and service do not yet exist | CONFIRMED | Grep / file search under `iam/` |
| E-5 | Rule B2 mandates uppercase alphanumeric format (e.g., `HR`, `FIN`, `IT`, `LEGAL`) | CONFIRMED | `01_iam_organization.md#UC-IAM-03` line 118 |
| E-6 | Alternative Path 2a explicitly specifies HTTP 409 Conflict on duplicate department code | CONFIRMED | `01_iam_organization.md#UC-IAM-03` line 115 |
| E-7 | Audit logging in IAM is implemented via `ApplicationEventPublisher` publishing domain events | CONFIRMED | `LoginService.java`, `AssignRolesService.java` |
| E-8 | Baseline test failures in existing tests caused by `persist()` on `@UuidGenerator` entities with pre-set UUIDs | OBSERVED | `./gradlew test` output (`task-86.log`) |

</evidence_matrix>

---

## 4. Execution Flow Analysis

<execution_flow>

### Flow A: Department Creation & Retrieval
1. `POST /api/v1/departments` received with `CreateDepartmentRequest(code, name, description)`.
2. Spring Security checks caller holds `ROLE_ADMIN` (403 if unauthorized, 401 if unauthenticated).
3. Payload validation ensures `code` matches regex `^[A-Z0-9_]+$` (Rule B2) and `name` is non-blank (400 if invalid).
4. `DepartmentService` checks if `code` already exists via `departmentRepositoryPort.existsByCode(code)`. If exists, throws `DepartmentCodeConflictException` -> HTTP 409 Conflict.
5. Domain model `Department` constructed and saved via `departmentRepositoryPort.save(department)`.
6. `DepartmentCreatedEvent` published via `ApplicationEventPublisher` (UC-AUDIT-01).
7. Returns HTTP 201 Created with `DepartmentResponseDto`.

### Flow B: Department Update
1. `PUT /api/v1/departments/{id}` received with `UpdateDepartmentRequest(code, name, description)`.
2. Department queried by ID (404 Not Found if missing).
3. Code format validated (400 Bad Request if invalid format).
4. Check if new code conflicts with another department (`existsByCodeIgnoreCaseAndIdNot`). If so, throws 409 Conflict.
5. Department details updated, saved to database.
6. `DepartmentUpdatedEvent` published (UC-AUDIT-01).
7. Returns HTTP 200 OK with `DepartmentResponseDto`.

### Flow C: User Department & Internal Employee Verification
1. `PUT /api/v1/users/{userId}/department` received with `AssignUserDepartmentRequest(departmentId, isInternal)`.
2. Caller authorized (`ROLE_ADMIN`).
3. User queried via `userRepositoryPort.findById(userId)` (404 Not Found if missing).
4. If `departmentId` is not null, department queried via `departmentRepositoryPort.findById(departmentId)` (404 Not Found if department does not exist, upholding NF1 referential integrity).
5. User domain model updated: `user.assignDepartment(departmentId, isInternal)`.
6. User persisted via `userRepositoryPort.save(user)`.
7. `UserDepartmentAssignedEvent` published capturing `targetUserId`, `departmentId`, `isInternal`, and `operatorUserId`.
8. Returns HTTP 200 OK with updated profile / user department details.

</execution_flow>

---

## 5. Research Exit Criteria (Gate G0)

<gate_g0_checklist>
  - [x] All requirements and business rules (B1, B2, NF1) from `UC-IAM-03` understood and mapped.
  - [x] Database schema verified against Liquibase changelogs.
  - [x] Existing domain and adapter architecture inspected.
  - [x] Execution flows for department management and user assignment traced.
  - [x] Baseline test harness state and failure cause observed and documented.
  - [x] No open blockers preventing advancement to INNOVATE phase.
</gate_g0_checklist>

</research_context>
