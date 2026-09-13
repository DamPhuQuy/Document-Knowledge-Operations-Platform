# Task Handoff: UC-IAM-02 Multi-Role Assignment & Permission Management

<handoff_summary task_id="UC-IAM-02" version="1.0" framework="RIPER-5">

## 1. Summary of Changes
- **Domain Invariants**:
  - Implemented `assignRoles(Set<Role> newRoles, UUID operatorUserId)` in `User.java`.
  - Added `EmptyRolesException` enforcing **Rule B2** (every user must maintain $\ge 1$ active role).
  - Added `SelfRoleRevocationException` enforcing **Alternative Path 4a** (preventing administrators from revoking their own `ROLE_ADMIN` role).
- **Application Services & Ports**:
  - Added inbound ports `AssignRolesUseCase` and `GetUserRolesUseCase` with immutable command and DTOs.
  - Added outbound port `RoleRepositoryPort` (`findByIds`, `findByCode`, `findAll`) and updated `UserRepositoryPort` (`findById`, `save`).
  - Implemented `AssignRolesService` publishing `UserRolesUpdatedEvent` for asynchronous audit trail logging (`UC-AUDIT-01`).
- **Persistence Layer**:
  - Implemented `SpringDataRoleRepository` with eager permission fetching.
  - Implemented `RoleRepositoryAdapter` translating between JPA entities and clean `Role` domain models.
  - Updated `UserRepositoryAdapter` to persist updated roles in `user_roles` transactionally.
- **Security & REST API**:
  - Implemented `JwtAuthenticationFilter` reading bearer tokens and populating `SecurityContext` with authorities.
  - Enabled `@EnableMethodSecurity` in `SecurityConfig`.
  - Implemented `UserRoleController` with:
    - `GET /api/v1/users/{userId}/roles` (secured via `@PreAuthorize`)
    - `PUT /api/v1/users/{userId}/roles` (secured via `@PreAuthorize`)
  - Updated `RestExceptionHandler` to map domain exceptions cleanly to HTTP 400 Bad Request and 403 Forbidden.

## 2. Verification Evidence
- Unit tests: `UserTest` (6/6 passing), `AssignRolesServiceTest` (6/6 passing).
- Persistence integration tests: `PersistenceAdaptersTest` (4/4 passing).
- Web security & controller tests: `UserRoleControllerTest` (6/6 passing).
- Regression suite: `./gradlew test check` &rarr; **37/37 tests passing**, Spotless format check passing.

## 3. API Reference
- `GET /api/v1/users/{userId}/roles`
  - Headers: `Authorization: Bearer <token>`
  - Requires: `manage:users` or `ROLE_ADMIN`
  - Response: `200 OK` (`UserRolesResponseDto`)
- `PUT /api/v1/users/{userId}/roles`
  - Headers: `Authorization: Bearer <token>`, `Content-Type: application/json`
  - Requires: `manage:users` or `ROLE_ADMIN`
  - Request: `{"roleIds": ["<uuid>", ...]}`
  - Response: `200 OK` (`UserRolesResponseDto`) or `400 Bad Request`

</handoff_summary>
