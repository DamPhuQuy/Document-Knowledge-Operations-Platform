# Decision: DEC-IAM-03 Department Setup & Internal Employee Verification Architecture

<technical_decision task_id="UC-IAM-03" dec_id="DEC-IAM-03" version="1.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. Present options. PAIR: engineer decides. DELEGATED: agent auto-selects optimal recommendation and advances. -->
<decision_status>
  <phase>INNOVATE</phase>
  <mode>DELEGATED</mode>
  <decision_owner>[AUTO: DELEGATED]</decision_owner>
  <last_updated>2026-09-14</last_updated>
</decision_status>

---

## 1. Context

<context>
  <task>process/features/active/UC-IAM-03-department-setup-internal-verification/task.md</task>
  <research_artifact>process/features/active/UC-IAM-03-department-setup-internal-verification/research.md</research_artifact>
  <constraints>
    - Clean Architecture & Hexagonal Ports/Adapters: Domain model must remain framework-free.
    - Business Rule B2: Department code must be uppercase alphanumeric (e.g., HR, FIN, IT, LEGAL).
    - Alternative Path 2a: Duplicate department code returns HTTP 409 Conflict.
    - Business Rule B1: Users with is_internal = FALSE are external users.
    - NF1: Referential integrity must be guaranteed (user assignment must validate department existence).
    - Integration with UC-AUDIT-01: Publish domain events via ApplicationEventPublisher.
  </constraints>
</context>

---

## 2. Decision Required

<decision_question>
  How should the Department Setup and User Department / Internal Verification features be structured across REST endpoints, domain models, entity ID strategies, and validation layers?
</decision_question>

---

## 3. Options

<options>

  <option id="A">
    <approach>
      - Hexagonal separation with dedicated REST endpoints:
        - `DepartmentController` managing `/api/v1/departments` (CRUD).
        - `UserDepartmentController` managing `/api/v1/users/{userId}/department` (aligning with `UserRoleController` at `/api/v1/users/{userId}/roles`).
      - Domain model `Department` enforces Rule B2 (uppercase alphanumeric validation) in domain constructor/factory; REST DTOs also apply `@Pattern(regexp = "^[A-Z0-9_]+$")` for fast-fail validation.
      - Domain model `User` introduces `assignDepartment(UUID departmentId, boolean isInternal)` encapsulating invariant updates.
      - Identifier strategy: Domain models generate/assign UUIDv7 or explicit UUIDs; JPA entities use assigned IDs (`@Id private UUID id;`) to avoid Hibernate detached entity conflicts when persisting pre-constructed instances.
      - Conflict handling: `DepartmentCodeConflictException` mapped to HTTP 409 in `RestExceptionHandler`.
      - Audit trail: `ApplicationEventPublisher` publishes `DepartmentCreatedEvent`, `DepartmentUpdatedEvent`, `UserDepartmentAssignedEvent`.
    </approach>
    <advantages>
      - Strict Clean Architecture and consistency with existing IAM patterns (`UserRoleController`, `AssignRolesService`).
      - Defense-in-depth validation at both REST boundary and domain core.
      - Eliminates Hibernate `PersistentObjectException` by letting domain own UUID assignment.
      - High cohesion and clear RESTful resource routing.
    </advantages>
    <disadvantages>
      - Slightly more classes (two controllers rather than packing everything into one).
    </disadvantages>
    <complexity>LOW</complexity>
    <compatibility>Fully backward compatible with existing IAM API and database schema.</compatibility>
    <concurrency_transaction_risk>Low. Unique index `uk_departments_code` guarantees serial consistency; transactions wrapped with `@Transactional`.</concurrency_transaction_risk>
    <testability>Excellent. Unit testable domain without Spring, mockable ports for application service, and WebMvc integration tests.</testability>
    <maintainability>High. Clean boundaries and clear separation of concerns.</maintainability>
  </option>

  <option id="B">
    <approach>
      - Monolithic controller: Put both department management and user assignment under `/api/v1/departments` (e.g. `/api/v1/departments/{id}/users/{userId}`).
      - Only validate regex at database constraint level or controller level without domain entity validation.
    </approach>
    <advantages>
      - Fewer controller files.
    </advantages>
    <disadvantages>
      - Violates RESTful semantics for user resource mutations (user state modified under `/departments`).
      - Inconsistent with `UserRoleController` which lives at `/api/v1/users/{userId}/...`.
      - Weak domain model (anemic domain).
    </disadvantages>
    <complexity>MEDIUM</complexity>
    <compatibility>Inconsistent with existing IAM API design.</compatibility>
    <concurrency_transaction_risk>Low.</concurrency_transaction_risk>
    <testability>Moderate.</testability>
    <maintainability>Moderate.</maintainability>
  </option>

  <option id="C">
    <approach>
      - Generic admin patch endpoint: Combine role assignment and department assignment into a single `/api/v1/admin/users/{userId}` PATCH endpoint.
    </approach>
    <advantages>
      - Single endpoint for user management.
    </advantages>
    <disadvantages>
      - Couples UC-IAM-02 (roles) with UC-IAM-03 (departments), violating single-responsibility principle.
      - Large request payload with partial null-checking complexity.
    </disadvantages>
    <complexity>HIGH</complexity>
    <compatibility>Breaks existing `PUT /api/v1/users/{userId}/roles` convention.</compatibility>
    <concurrency_transaction_risk>High lock contention when mutating multiple user aspects at once.</concurrency_transaction_risk>
    <testability>Complex.</testability>
    <maintainability>Low.</maintainability>
  </option>

</options>

---

## 4. Trade-off Matrix

<tradeoff_matrix>

| Criterion | Option A | Option B | Option C |
|---|:---:|:---:|:---:|
| Compatibility | 5 | 3 | 2 |
| Complexity | 5 | 4 | 2 |
| Risk | 5 | 3 | 2 |
| Testability | 5 | 4 | 3 |
| Maintainability | 5 | 3 | 2 |

<!-- Score: 1 (poor) → 5 (excellent) -->
</tradeoff_matrix>

---

## 5. Recommendation

<recommendation>
  Adopt **Option A**:
  1. Follow Hexagonal Architecture with `DepartmentController` (`/api/v1/departments`) and `UserDepartmentController` (`/api/v1/users/{userId}/department`).
  2. Implement domain model `Department` with code validation (regex `^[A-Z0-9_]+$`) and add `assignDepartment(UUID, boolean)` to `User`.
  3. Map `DepartmentCodeConflictException` to HTTP 409 Conflict and `DepartmentNotFoundException` to HTTP 404 Not Found in `RestExceptionHandler`.
  4. Use assigned ID strategy in JPA entity so domain model controls UUID instantiation consistently.
  5. Publish domain events for audit logging (`DepartmentCreatedEvent`, `DepartmentUpdatedEvent`, `UserDepartmentAssignedEvent`).
</recommendation>

---

## 6. Implementation Decision

<!-- PAIR mode: Completed by engineer before Gate 1 passes.
     DELEGATED / Fast-Track mode: Agent automatically populates Recommendation
     into <selected_option>, documents rationale, signs Gate 1 with [AUTO: DELEGATED], and proceeds. -->
<engineer_decision>
  <selected_option>Option A</selected_option>
  <rationale>
    [AUTO: DELEGATED] Option A maintains structural fidelity with the existing Clean Architecture patterns established in UC-IAM-01 and UC-IAM-02. It provides optimal testability, clean REST resource routing, robust domain invariant enforcement for B2 and NF1, and direct support for UC-AUDIT-01 event publication.
  </rationale>
  <rejected_alternatives>
    - Option B: Rejected due to inconsistent REST semantics and anemic domain model.
    - Option C: Rejected due to coupling unrelated use cases and increased transactional complexity.
  </rejected_alternatives>
</engineer_decision>

---

## 7. Constraints Created by This Decision

<constraints_created>
  - `Department` code must match `^[A-Z0-9_]+$` and be uppercase.
  - Endpoints require `@PreAuthorize("hasRole('ADMIN')")`.
  - Duplicate code throws `DepartmentCodeConflictException` resulting in HTTP 409.
  - Missing department during assignment throws `DepartmentNotFoundException` resulting in HTTP 404.
  - Assigned ID strategy must be used on `DepartmentJpaEntity` (and calibrated on `RoleJpaEntity` / `UserJpaEntity` if needed for test harness health).
</constraints_created>

---

## 8. Evidence Still Required

<evidence_required>
  - Verification that `PUT /api/v1/users/{userId}/department` updates both `department_id` and `is_internal` in database.
  - Verification that duplicate department codes return HTTP 409.
  - Verification that audit events are properly published.
</evidence_required>

---

## Gate 1 — Decision Approved

<gate id="G1">
  - [x] All options presented and trade-offs analyzed.
  - [x] Selected option recorded above.
  - [x] No blocking business / schema / security decision remains open.
  <approved_by>[AUTO: DELEGATED]</approved_by>
  <approved_date>2026-09-14</approved_date>
</gate>

</technical_decision>
