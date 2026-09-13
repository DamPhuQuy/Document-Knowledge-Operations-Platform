# Task: UC-IAM-02 Multi-Role Assignment & Permission Management

<task_spec version="3.0" framework="RIPER-5">

<!-- ════════════════════════════════════════════
     SECTION 0 — TASK CONTROL (master state record)
     ════════════════════════════════════════════ -->
<task_control>
  <status>COMPLETED</status>
  <spec_level>S3</spec_level>
  <priority>P1</priority>
  <risk>LOW</risk>
  <estimated_story_points>2</estimated_story_points>
  <working_mode>DELEGATED</working_mode>
  <current_phase>COMPLETED</current_phase>
  <owner>@engineer</owner>
  <decision_owner>@engineer</decision_owner>
  <created>2026-09-13</created>
  <last_updated>2026-09-13</last_updated>
</task_control>

---

## 1. Specification (Pillar 1: Task / Spec)

<specification>
  <goal>
    Enable system administrators (holding `ROLE_ADMIN` / `manage:users` permission) to view assigned roles and permissions for any user, and to update user role assignments transactionally, with domain validation preventing empty role sets and administrative self-lockout, recording audit events via UC-AUDIT-01.
  </goal>

  <current_behavior>
    - User and Role domain models exist (`User.java`, `Role.java`, `Permission.java`).
    - `User.getAllPermissionCodes()` computes the mathematical union of permissions across assigned roles.
    - Database schema `user_roles` supports many-to-many relationship between users and roles.
    - `UserRepositoryPort` only defines `findByEmail(String email)`.
    - No endpoint or application service exists to retrieve user roles or reassign roles to a user.
    - No `RoleRepositoryPort` or `SpringDataRoleRepository` exists to query active roles by ID or code.
  </current_behavior>

  <expected_behavior>
    - System administrators can query a user's current roles, department, and effective permissions (`GET /api/v1/users/{userId}/roles`).
    - System administrators can replace/update a user's assigned roles (`PUT /api/v1/users/{userId}/roles`).
    - Business Rule B1: Effective permissions equal the mathematical UNION of all permissions across assigned roles.
    - Business Rule B2: Every user must maintain at least one active role (HTTP 400 Bad Request if roles set is empty).
    - Alternative Path 4a: Self-lockout prevention: An administrator cannot revoke their own `ROLE_ADMIN` role (HTTP 400 Bad Request if an admin attempts to remove `ROLE_ADMIN` from themselves).
    - Alternative Path 6a: Non-admin or caller without `manage:users` permission receives HTTP 403 Forbidden.
    - Role mutations trigger an audit event (e.g., `UserRolesUpdatedEvent`) capturing before/after role IDs to support UC-AUDIT-01 audit logging.
  </expected_behavior>

  <actor_authorization>
    System Administrator authenticated with `ROLE_ADMIN` and/or `manage:users` permission.
  </actor_authorization>

  <invariants>
    - Effective permissions must always equal the union of permissions of all assigned roles.
    - Every user must have at least one active role assigned at all times.
    - An administrator cannot revoke `ROLE_ADMIN` from their own account.
    - All role updates must execute in an atomic transaction (`@Transactional`).
    - Clean Architecture boundaries: Domain must remain framework-free; JPA entities must not leak into Application/Domain layers.
  </invariants>

  <out_of_scope>
    - Department assignment (`UC-IAM-03`).
    - Dynamic permission creation or modification (roles/permissions catalog is statically seeded or managed separately).
    - Real-time WebSocket notifications.
  </out_of_scope>

  <acceptance_criteria>
    - [x] AC-1: Administrator can retrieve user roles and effective permissions (`GET /api/v1/users/{userId}/roles`) returning HTTP 200 with role codes and permissions.
    - [x] AC-2: Administrator can assign a valid set of roles to a user (`PUT /api/v1/users/{userId}/roles`) returning HTTP 200 with updated roles and effective permissions.
    - [x] AC-3: Request with empty role set is rejected with HTTP 400 Bad Request (Business Rule B2: At least one active role required).
    - [x] AC-4: Administrator attempting to remove `ROLE_ADMIN` from their own account is rejected with HTTP 400 Bad Request (Alternative Path 4a: Anti-lockout guardrail).
    - [x] AC-5: User without `manage:users` / `ROLE_ADMIN` attempting role assignment receives HTTP 403 Forbidden.
    - [x] AC-6: `UserRolesUpdatedEvent` is published on successful role assignment with before and after state snapshots.
    - [x] AC-7: Automated tests (unit tests for domain invariants & service, web integration test for controller, repository tests) pass with 100% success.
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
    - `src/main/java/com/platform/app/iam/domain/model/User.java` — Add role reassignment domain methods and invariant validations
    - `src/main/java/com/platform/app/iam/application/ports/inbound/AssignRolesUseCase.java` — Inbound port for role assignment
    - `src/main/java/com/platform/app/iam/application/ports/inbound/GetUserRolesUseCase.java` — Inbound port for viewing user roles
    - `src/main/java/com/platform/app/iam/application/ports/inbound/AssignRolesCommand.java` — Input command DTO
    - `src/main/java/com/platform/app/iam/application/dto/UserRolesResponseDto.java` — Output response DTO
    - `src/main/java/com/platform/app/iam/application/dto/UserRolesUpdatedEvent.java` — Audit event for role update
    - `src/main/java/com/platform/app/iam/application/ports/outbound/UserRepositoryPort.java` — Add `findById(UUID id)` and `save(User user)`
    - `src/main/java/com/platform/app/iam/application/ports/outbound/RoleRepositoryPort.java` — Outbound port to fetch roles by IDs/codes
    - `src/main/java/com/platform/app/iam/application/services/AssignRolesService.java` — Application service implementing use cases
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/repository/SpringDataRoleRepository.java` — Spring Data JPA repository for roles
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/adapter/RoleRepositoryAdapter.java` — Secondary adapter implementing RoleRepositoryPort
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/adapter/UserRepositoryAdapter.java` — Implement `findById` and `save`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/UserRoleController.java` — REST endpoints for user roles
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/config/SecurityConfig.java` — Security authorization rules / JWT filter wiring
  </target_files>

  <context_groups>
    - planning
    - tests
    - protocols
    - domain-specific-group: IAM_Organization
  </context_groups>

  <source_of_truth>
    <requirement>docs/specs/business/use_cases/01_iam_organization.md (UC-IAM-02)</requirement>
    <architecture>docs/specs/business/MVP.md, backend/process/context/architecture/architecture-template.md</architecture>
    <existing_behavior>backend/src/test/java/com/platform/app/iam/application/services/LoginServiceTest.java</existing_behavior>
    <tests>backend/src/test/java/com/platform/app/iam/</tests>
  </source_of_truth>
</context_boundaries>

---

## 3. Verification Strategy

<verification_strategy>

| AC / Risk | Evidence required | Verifier |
|---|---|---|
| AC-1: Get User Roles | HTTP 200 with list of roles and union of permissions | Controller / Integration test |
| AC-2: Assign Roles | HTTP 200 with updated roles and persisted user_roles | Integration test & Service test |
| AC-3: Empty Role Set | HTTP 400 Bad Request when role list is empty | Domain unit test & Controller test |
| AC-4: Self-Lockout Prevention | HTTP 400 Bad Request when admin removes ROLE_ADMIN from self | Domain / Service unit test |
| AC-5: Unauthorized Caller | HTTP 403 Forbidden when caller lacks manage:users | Controller security test |
| AC-6: Audit Event | Event published with before/after role snapshots | Spring Event listener verification test |
| AC-7: Regression & Lint | All tests pass, Spotless check passes | `./gradlew test check` |

</verification_strategy>

</task_spec>
