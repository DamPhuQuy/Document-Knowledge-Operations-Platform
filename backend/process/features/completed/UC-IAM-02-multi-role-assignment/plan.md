# Implementation Plan: UC-IAM-02 Multi-Role Assignment & Permission Management

<implementation_plan task_id="UC-IAM-02" version="3.0" framework="RIPER-5">

<!-- PLAN PHASE. Plan artifacts only. No source code changes. -->
<plan_status>
  <phase>PLAN</phase>
  <mode>DELEGATED</mode>
  <plan_owner>@engineer</plan_owner>
  <last_updated>2026-09-13</last_updated>
</plan_status>

---

## 1. Scope Contract

<scope_contract>

### Allowed Files (White-list)
```text
# Domain Layer
backend/src/main/java/com/platform/app/iam/domain/exception/EmptyRolesException.java
backend/src/main/java/com/platform/app/iam/domain/exception/SelfRoleRevocationException.java
backend/src/main/java/com/platform/app/iam/domain/model/User.java

# Application Layer
backend/src/main/java/com/platform/app/iam/application/dto/UserRolesResponseDto.java
backend/src/main/java/com/platform/app/iam/application/dto/UserRolesUpdatedEvent.java
backend/src/main/java/com/platform/app/iam/application/ports/inbound/AssignRolesCommand.java
backend/src/main/java/com/platform/app/iam/application/ports/inbound/AssignRolesUseCase.java
backend/src/main/java/com/platform/app/iam/application/ports/inbound/GetUserRolesUseCase.java
backend/src/main/java/com/platform/app/iam/application/ports/outbound/RoleRepositoryPort.java
backend/src/main/java/com/platform/app/iam/application/ports/outbound/UserRepositoryPort.java
backend/src/main/java/com/platform/app/iam/application/services/AssignRolesService.java

# Infrastructure Layer (Secondary Persistence)
backend/src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/repository/SpringDataRoleRepository.java
backend/src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/adapter/RoleRepositoryAdapter.java
backend/src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/adapter/UserRepositoryAdapter.java

# Infrastructure Layer (Security & Primary REST)
backend/src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/filter/JwtAuthenticationFilter.java
backend/src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/config/SecurityConfig.java
backend/src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/UserRoleController.java
backend/src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AssignRolesRequest.java
backend/src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java

# Tests
backend/src/test/java/com/platform/app/iam/domain/model/UserTest.java
backend/src/test/java/com/platform/app/iam/application/services/AssignRolesServiceTest.java
backend/src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/adapter/RoleRepositoryAdapterTest.java
backend/src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/adapter/PersistenceAdaptersTest.java
backend/src/test/java/com/platform/app/iam/infrastructure/adapters/primary/rest/UserRoleControllerTest.java

# Task and state management
backend/process/features/active/UC-IAM-02-multi-role-assignment/*
```

### Forbidden Files (Anti-Escape Blacklist)
- Any files under `com/platform/app/iam/infrastructure/adapters/secondary/security/adapter/JwtTokenProviderAdapter.java` (reuse existing as-is).
- Database migrations under `src/main/resources/db/changelog/` (schema already contains `roles`, `user_roles`, `role_permissions`).
- Any project root configuration (`build.gradle`, `settings.gradle`, `Dockerfile`, CI workflows).

</scope_contract>

---

## 2. Execution Slices

<execution_slices>

### Slice 1: Domain Invariants & Exceptions
- **Goal**: Implement rich aggregate root logic on `User` for role assignment.
- **Tasks**:
  1. Create `EmptyRolesException` and `SelfRoleRevocationException` in `domain/exception/`.
  2. Add `assignRoles(Set<Role> newRoles, UUID operatorUserId)` method in `User.java`.
  3. Validate Rule B2: Throw `EmptyRolesException` if `newRoles == null || newRoles.isEmpty()`.
  4. Validate Alt Path 4a: Throw `SelfRoleRevocationException` if `this.id.equals(operatorUserId)` and new roles lack `ROLE_ADMIN`.
  5. Add unit tests in `UserTest.java` verifying normal role replacement, empty set rejection, and self-lockout prevention.
- **Verifier**: `./gradlew test --tests com.platform.app.iam.domain.model.UserTest`
- **Expected Evidence**: All unit tests in `UserTest` pass.
- **Rollback Point**: Revert edits to `User.java` and new domain exception files.

### Slice 2: Application Ports, DTOs & Service
- **Goal**: Define inbound/outbound ports and implement `AssignRolesService`.
- **Tasks**:
  1. Create `AssignRolesUseCase` and `GetUserRolesUseCase` inbound ports.
  2. Create `AssignRolesCommand`, `UserRolesResponseDto`, and `UserRolesUpdatedEvent`.
  3. Define `RoleRepositoryPort` (`findByIds`, `findByCode`, `findAll`).
  4. Update `UserRepositoryPort` to add `Optional<User> findById(UUID id)` and `User save(User user)`.
  5. Implement `AssignRolesService` (injects `UserRepositoryPort`, `RoleRepositoryPort`, `ApplicationEventPublisher`).
  6. Create `AssignRolesServiceTest` covering successful assignment, user not found, role not found, and event publication.
- **Verifier**: `./gradlew test --tests com.platform.app.iam.application.services.AssignRolesServiceTest`
- **Expected Evidence**: `AssignRolesServiceTest` passes cleanly.
- **Rollback Point**: Discard slice 2 files.

### Slice 3: Secondary Persistence Adapters
- **Goal**: Connect Application Outbound Ports to JPA repositories.
- **Tasks**:
  1. Create `SpringDataRoleRepository` extending `JpaRepository<RoleJpaEntity, UUID>` with `@Query` to fetch permissions eagerly.
  2. Create `RoleRepositoryAdapter` implementing `RoleRepositoryPort`.
  3. Update `UserRepositoryAdapter` to implement `findById` (fetching roles and permissions) and `save(User user)`.
  4. Create / update persistence integration tests in `PersistenceAdaptersTest`.
- **Verifier**: `./gradlew test --tests com.platform.app.iam.infrastructure.adapters.secondary.persistence.adapter.*`
- **Expected Evidence**: Repositories persist and query roles and user-role relations correctly.
- **Rollback Point**: Revert slice 3 files.

### Slice 4: Security & Primary REST Controller
- **Goal**: Implement `JwtAuthenticationFilter` and `UserRoleController` with RBAC authorization.
- **Tasks**:
  1. Create `JwtAuthenticationFilter` to parse bearer token, validate with `TokenProviderPort`, extract user claims, and set `UsernamePasswordAuthenticationToken` with authorities (`ROLE_*` and permission codes).
  2. Register `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter` in `SecurityConfig` and enable `@EnableMethodSecurity`.
  3. Create `UserRoleController` with:
     - `GET /api/v1/users/{userId}/roles` &rarr; returns `UserRolesResponseDto`
     - `PUT /api/v1/users/{userId}/roles` &rarr; accepts `AssignRolesRequest` and returns updated `UserRolesResponseDto`.
  4. Protect endpoints with `@PreAuthorize("hasAuthority('manage:users') or hasRole('ADMIN')")`.
  5. Update `RestExceptionHandler` to map `EmptyRolesException`, `SelfRoleRevocationException`, and IllegalArgumentException to HTTP 400.
  6. Create `UserRoleControllerTest` using `@WebMvcTest`.
- **Verifier**: `./gradlew test --tests com.platform.app.iam.infrastructure.adapters.primary.rest.UserRoleControllerTest`
- **Expected Evidence**: Controller returns 200 for valid requests, 400 for empty/self-lockout, 403 for unauthorized users.
- **Rollback Point**: Revert slice 4 files.

### Slice 5: Full Regression & Quality Audit
- **Goal**: Verify zero regression across the entire application and enforce code formatting.
- **Tasks**:
  1. Run full test suite: `./gradlew test`.
  2. Run code style and lint check: `./gradlew check`.
- **Verifier**: `./gradlew test check`
- **Expected Evidence**: 100% passing tests, spotlessCheck passes.
- **Rollback Point**: Fix formatting or test issues.

</execution_slices>

---

## 3. Verification Matrix

<verification_matrix>

| Slice | Verifier Command | Expected Evidence | Rollback Point |
|---|---|---|---|
| Slice 1 | `./gradlew test --tests com.platform.app.iam.domain.model.UserTest` | Invariant tests pass (B2, 4a) | Git checkout slice 1 files |
| Slice 2 | `./gradlew test --tests com.platform.app.iam.application.services.AssignRolesServiceTest` | Service orchestrates and publishes events | Git checkout slice 2 files |
| Slice 3 | `./gradlew test --tests com.platform.app.iam.infrastructure.adapters.secondary.persistence.adapter.*` | Roles and User persistence pass | Git checkout slice 3 files |
| Slice 4 | `./gradlew test --tests com.platform.app.iam.infrastructure.adapters.primary.rest.UserRoleControllerTest` | REST RBAC and endpoints return expected statuses | Git checkout slice 4 files |
| Slice 5 | `./gradlew test check` | 100% tests pass, Spotless check passes | Revert or fix diff |

</verification_matrix>

---

## 4. Gate G2 Sign-Off

<!-- Fast-track / DELEGATED mode: auto-certified -->
<gate_g2_signoff>
  <status>PASSED [AUTO: DELEGATED]</status>
  <certification>
    Plan conforms to Clean Architecture and strict scope contract. All 5 execution slices have verifiable criteria, explicit evidence, and rollback points.
  </certification>
  <signed_by>Antigravity [DELEGATED]</signed_by>
  <timestamp>2026-09-13</timestamp>
</gate_g2_signoff>

</implementation_plan>
