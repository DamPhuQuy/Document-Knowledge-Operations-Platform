# Research: UC-IAM-02 Multi-Role Assignment & Permission Management

<research_context task_id="UC-IAM-02" version="3.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. No source-code modifications. No implementation decisions. -->
<research_status>
  <phase>RESEARCH</phase>
  <mode>PAIR</mode>
  <research_owner>@engineer</research_owner>
  <last_updated>2026-09-13</last_updated>
</research_status>

---

## 1. Specification Analysis & Business Requirements

<business_spec source="docs/specs/business/use_cases/01_iam_organization.md#UC-IAM-02">
  - **Goal**: Enable system administrators holding `ROLE_ADMIN` / possessing `manage:users` permission to assign multiple roles and manage fine-grained permissions for users.
  - **Actors**: System Administrator (`ROLE_ADMIN`) (Primary), IAM Subsystem (Secondary).
  - **Pre-Conditions**:
    1. Administrator is authenticated and possesses `manage:users` permission.
    2. Target user and assigned roles exist in the database.
  - **Post-Conditions**:
    1. Updated role associations are committed to `user_roles`.
    2. Target user's subsequent token refreshes inherit updated permissions.
    3. Audit log entry with action `ASSIGN_ROLES` is appended via `UC-AUDIT-01`.
  - **Key Flows**:
    - Query target user's current roles, department, and granted permissions.
    - Submit role replacement/update with a set of role identifiers/codes.
    - Validate that caller does not self-lockout by revoking their own `ROLE_ADMIN` role (HTTP 400 Bad Request).
    - Validate caller authorization (HTTP 403 Forbidden if missing `manage:users`).
    - Validate that user maintains at least one active role (HTTP 400 Bad Request if empty).
    - Persist associations in database transaction.
    - Publish audit event capturing before/after snapshots for `UC-AUDIT-01`.
  - **Business Rules**:
    - **B1**: Effective permissions equal the mathematical UNION of all permissions across all assigned roles.
    - **B2**: Every user must maintain at least one active role.
</business_spec>

---

## 2. Current Codebase State & Architectural Baseline

<codebase_baseline>

### A. Domain Models (`src/main/java/com/platform/app/iam/domain/model/`)
- `User.java`:
  - Contains `UUID id`, `String email`, `String fullName`, `UUID departmentId`, `boolean enabled`, `boolean internal`, `Set<Role> roles`.
  - Has `getAllPermissionCodes()` which performs:
    ```java
    roles.stream().flatMap(role -> role.getPermissionCodes().stream()).collect(Collectors.toSet());
    ```
    (Confirmed: Rule B1 is already mathematically supported in `User.java`).
  - Lacks mutator or domain method to update/replace roles with validation of Rule B2 (at least one role) and self-lockout check.
- `Role.java`:
  - Contains `UUID id`, `String code`, `String name`, `String description`, `Set<Permission> permissions`.
  - Has `getPermissionCodes()`.
- `Permission.java`:
  - Contains `UUID id`, `String code`, `String name`, `String module`, `String description`.

### B. Persistence Layer (`src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/`)
- Database Schema (`002-create-iam-tables.yaml`):
  - `roles`: `id` (UUID PK), `code` (VARCHAR(50) UK), `name`, `description`.
  - `permissions`: `id` (UUID PK), `code` (VARCHAR(100) UK), `name`, `module`.
  - `user_roles`: `user_id` (FK users), `role_id` (FK roles), composite PK `(user_id, role_id)`.
  - `role_permissions`: `role_id` (FK roles), `permission_id` (FK permissions), composite PK `(role_id, permission_id)`.
- Existing JPA Entities:
  - `UserJpaEntity.java`: `@ManyToMany` joined with `RoleJpaEntity` via `user_roles`.
  - `RoleJpaEntity.java`: `@ManyToMany` joined with `PermissionJpaEntity` via `role_permissions`.
  - `PermissionJpaEntity.java`: Maps `permissions` table.
- Existing Repositories:
  - `SpringDataUserRepository`: Contains `findByEmailIgnoreCaseWithRolesAndPermissions`. Does not have dedicated `findByIdWithRolesAndPermissions` (though `findById` exists from `JpaRepository`).
  - `SpringDataRoleRepository`: Does not exist yet. Needs to be created.
- Existing Repository Ports:
  - `UserRepositoryPort`: Only contains `Optional<User> findByEmail(String email)`. Needs `Optional<User> findById(UUID id)` and `User save(User user)`.
  - `RoleRepositoryPort`: Does not exist yet. Needs to be defined.

### C. Application & Security Layers
- Existing Application Services:
  - `LoginService.java`: Implements `LoginUseCase` for authentication (`UC-IAM-01`).
- Security Configuration (`SecurityConfig.java`):
  - Current configuration allows `/api/v1/auth/login`, Swagger, and Actuator as `permitAll()`.
  - All other endpoints are configured with `.anyRequest().authenticated()`.
  - Note: A `JwtAuthenticationFilter` or security context resolution is needed for protected endpoints to extract authenticated user credentials, roles, and permissions (e.g., `manage:users` or `ROLE_ADMIN`).

</codebase_baseline>

---

## 3. Evidence Classification

<evidence_matrix>

| Item | Finding / Assumption | Classification | Source / Verification |
|---|---|---|---|
| E-1 | Table `user_roles` supports many-to-many relationship with foreign keys | CONFIRMED | `002-create-iam-tables.yaml` line 217-255 |
| E-2 | `User` domain model computes permission union | CONFIRMED | `User.java` line 74-78 (`getAllPermissionCodes`) |
| E-3 | `UserRepositoryPort` has no `findById` or `save` methods | CONFIRMED | `UserRepositoryPort.java` line 6-8 |
| E-4 | `RoleRepositoryPort` and `SpringDataRoleRepository` do not exist | CONFIRMED | Directory search under `iam/` |
| E-5 | Audit log infrastructure table `audit_logs` is already created | CONFIRMED | `008-create-audit-and-notification-tables.yaml` |
| E-6 | Inbound ports for role management do not yet exist | CONFIRMED | Directory search under `iam/application/ports/inbound/` |
| E-7 | Inbound JWT authentication filter needs to populate SecurityContext | OBSERVED | `SecurityConfig.java` configures stateless session but has no `JwtAuthenticationFilter` registered |

</evidence_matrix>

---

## 4. Gap Analysis & Required Work

<gap_analysis>

1. **Domain Model Invariants**:
   - `User.java` needs a domain method to update roles: `assignRoles(Set<Role> newRoles, UUID operatorUserId)`.
   - Domain validation: Ensure `newRoles` is non-null and not empty (Rule B2).
   - Domain validation: If `this.id.equals(operatorUserId)`, verify that the new roles set still contains `ROLE_ADMIN` (Alternative Path 4a: anti-lockout).
   - Domain exception: `SelfRoleRevocationException` or `EmptyRolesException`.

2. **Application Ports**:
   - Inbound Port: `AssignRolesUseCase` (with `AssignRolesCommand`) and `GetUserRolesUseCase`.
   - Outbound Port: `RoleRepositoryPort` to find roles by IDs or codes, and finding all available roles.
   - Update `UserRepositoryPort`: Add `Optional<User> findById(UUID id)` and `User save(User user)`.
   - DTOs: `UserRolesResponseDto` (user ID, email, roles, permissions), `AssignRolesCommand` (targetUserId, roleIds/roleCodes, operatorUserId).
   - Event: `UserRolesUpdatedEvent` (targetUserId, oldRoleCodes, newRoleCodes, operatorUserId, timestamp).

3. **Infrastructure / Persistence**:
   - Create `SpringDataRoleRepository` with queries to fetch roles by IDs with permissions.
   - Create `RoleRepositoryAdapter` implementing `RoleRepositoryPort`.
   - Update `UserRepositoryAdapter` to implement `findById` (with fetched roles and permissions) and `save`.

4. **Primary Adapters & Security**:
   - Create `UserRoleController` exposing:
     - `GET /api/v1/users/{id}/roles`
     - `PUT /api/v1/users/{id}/roles`
   - Secure endpoints using `@PreAuthorize("hasAuthority('manage:users') or hasRole('ADMIN')")` or SecurityConfig matchers.
   - Implement / register `JwtAuthenticationFilter` so incoming requests with `Bearer <token>` establish a valid `Authentication` in `SecurityContextHolder`.

</gap_analysis>

---

## 5. Research Exit Criteria (Gate G0)

<research_exit_criteria>
  - [x] G0.1: Relevant specifications (`UC-IAM-02`) thoroughly examined and requirements mapped.
  - [x] G0.2: Existing domain models, JPA entities, database schema, and security setup analyzed.
  - [x] G0.3: Invariants, business rules (B1, B2, anti-lockout 4a), and authorization constraints identified.
  - [x] G0.4: Missing components (ports, adapters, services, controllers, filter) documented in gap analysis.
  - [x] G0.5: Evidence classified as Confirmed / Observed / Hypothesized.
</research_exit_criteria>

</research_context>
