# Plan: PLAN-IAM-01 UC-IAM-01 User Login & JWT Session Lifecycle Management

<execution_plan task_id="UC-IAM-01" plan_id="PLAN-IAM-01" version="2.0" framework="RIPER-5">

<!-- PLAN PHASE. Plan artifacts only. No source-code changes. -->
<plan_status>
  <phase>PLAN</phase>
  <last_updated>2026-09-09</last_updated>
</plan_status>

---

## 1. Input Artifacts

<input_artifacts>
  <task_spec>[task.md](task.md)</task_spec>
  <research>[research.md](research.md)</research>
  <decision>[decision.md](decision.md) — DEC-IAM-01: Strict Clean Hexagonal Architecture per architecture-template.md, In-Memory Sliding Window Lockout Adapter, EventPublisherPort for Audit Decoupling</decision>
  <architecture>[Standard Architecture Template](../../../context/architecture/architecture-template.md)</architecture>
</input_artifacts>

---

## 2. Execution Constraints

<execution_constraints>
  <allowed_files>
    - `src/main/java/com/platform/app/iam/domain/**` — [Domain models and exceptions]
    - `src/main/java/com/platform/app/iam/application/**` — [Inbound/outbound ports, commands, handlers, DTOs]
    - `src/main/java/com/platform/app/iam/infrastructure/**` — [Primary and secondary adapters]
    - `src/test/java/com/platform/app/iam/**` — [Unit and integration test suites]
  </allowed_files>
  <forbidden_files>
    - `src/main/resources/db/changelog/**` — [Baseline Liquibase schema is immutable]
    - `src/main/java/com/platform/app/audit/**` — [UC-AUDIT-01 deferred]
    - `build.gradle` — [Dependencies already locked]
  </forbidden_files>
  <allowed_commands>
    - `./gradlew test`
    - `./gradlew test --tests "com.platform.app.iam.*"`
    - `./gradlew spotlessApply`
    - `./gradlew check`
  </allowed_commands>
  <restricted_operations>
    - No DB migrations or DDL changes.
    - No new third-party dependencies.
    - No framework annotations (JPA, Spring Security, Jackson) inside domain or application layers.
  </restricted_operations>
  <required_approvals>
    - Plan approval at Gate 2 before entering EXECUTE phase.
  </required_approvals>
</execution_constraints>

---

## 3. Slice Summary

<slice_summary>

| ID | Behavior | Boundary | Target Files | AC | Verifier | Risk | Mode | Rollback |
|---|---|---|---|---|---|---|---|---|
| S1 | Core Domain Models & Exceptions | Domain | `iam/domain/model/**`, `iam/domain/exception/**` | AC-1 | `./gradlew test --tests "com.platform.app.iam.domain.*"` | LOW | ATOMIC | `git checkout -- src/*/java/com/platform/app/iam/domain` |
| S2 | Application Layer (Ports, Commands, Handlers, DTOs) | Application | `iam/application/**` | AC-1, AC-6 | `./gradlew test --tests "com.platform.app.iam.application.*"` | LOW | ATOMIC | `git checkout -- src/*/java/com/platform/app/iam/application` |
| S3 | Secondary Security & Messaging Adapters | Infrastructure (Secondary) | `iam/infrastructure/adapters/secondary/security/**`, `iam/infrastructure/adapters/secondary/messaging/**` | AC-2, AC-3, AC-4, AC-5 | `./gradlew test --tests "com.platform.app.iam.infrastructure.adapters.secondary.security.*"` | LOW | ATOMIC | `git checkout -- src/*/java/com/platform/app/iam/infrastructure/adapters/secondary/security src/*/java/com/platform/app/iam/infrastructure/adapters/secondary/messaging` |
| S4 | Secondary Persistence Adapters (JPA Entities & Repos) | Infrastructure (Secondary) | `iam/infrastructure/adapters/secondary/persistence/**` | AC-1, AC-4 | `./gradlew test --tests "com.platform.app.iam.infrastructure.adapters.secondary.persistence.*"` | MEDIUM | ATOMIC | `git checkout -- src/*/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence` |
| S5 | Primary REST Adapter & Security Configuration | Infrastructure (Primary) | `iam/infrastructure/adapters/primary/rest/**`, `iam/infrastructure/adapters/secondary/security/SecurityConfig.java` | AC-7, AC-8 | `./gradlew test --tests "com.platform.app.iam.infrastructure.adapters.primary.rest.*"` | MEDIUM | ATOMIC | `git checkout -- src/*/java/com/platform/app/iam/infrastructure/adapters/primary/rest` |
| S6 | Full Verification & Spotless Code Quality | Full Suite | All touched files | AC-1..AC-8 | `./gradlew check` | LOW | ATOMIC | N/A |

</slice_summary>

---

## 4. Slice Details

<slices>

  <slice id="S1">
    <objective>Implement pure DDD Domain Models, Value Objects, and Domain Exceptions for IAM</objective>
    <change>
      - Create models in `com.platform.app.iam.domain.model`:
        - Value objects: `UserId`, `RoleId`, `DepartmentId`.
        - Entities: `User`, `Role`, `Permission`, `RefreshToken`.
      - Create domain exceptions in `com.platform.app.iam.domain.exception`:
        - `AccountDisabledException`, `AccountLockedException`, `InvalidCredentialsException`.
      - Add unit tests verifying domain entity encapsulation, status check, and immutability.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/iam/domain/model/**`
      - `src/main/java/com/platform/app/iam/domain/exception/**`
      - `src/test/java/com/platform/app/iam/domain/**`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-1: Domain models created for User, Role, Permission, RefreshToken following Clean Architecture.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "com.platform.app.iam.domain.*"
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with all domain model tests passing.</expected_evidence>
    <rollback_point>`git checkout -- src/main/java/com/platform/app/iam/domain src/test/java/com/platform/app/iam/domain`</rollback_point>
    <stop_conditions>
      - Dependency or framework leakage into domain layer.
    </stop_conditions>
  </slice>

  <slice id="S2">
    <objective>Implement Application Layer with Inbound/Outbound Ports, Use Cases, Commands, and Handlers</objective>
    <change>
      - Create inbound port: `com.platform.app.iam.application.ports.inbound.LoginUseCase`.
      - Create outbound ports:
        - `UserRepositoryPort`, `RefreshTokenRepositoryPort`, `PasswordEncoderPort`, `TokenProviderPort`, `AccountLockoutPort`, `EventPublisherPort`.
      - Create command & service:
        - `com.platform.app.iam.application.ports.inbound.LoginCommand`.
        - `com.platform.app.iam.application.services.LoginService` (implements `LoginUseCase`).
      - Create DTOs:
        - `com.platform.app.iam.application.dto.AuthTokensDto`, `UserProfileDto`.
      - Create domain events:
        - `com.platform.app.iam.application.dto.UserLoginSuccessEvent`, `UserLoginFailedEvent`.
      - Add unit tests using Mockito on outbound ports covering all scenarios (success, wrong password, account disabled, account locked).
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/iam/application/**`
      - `src/test/java/com/platform/app/iam/application/**`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-1: Application ports and use cases established.
      - [ ] AC-6: Login domain events published via EventPublisherPort.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "com.platform.app.iam.application.*"
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with all application use case tests passing.</expected_evidence>
    <rollback_point>`git checkout -- src/main/java/com/platform/app/iam/application src/test/java/com/platform/app/iam/application`</rollback_point>
    <stop_conditions>
      - Framework dependency leakage in Application layer.
    </stop_conditions>
  </slice>

  <slice id="S3">
    <objective>Implement Secondary Security and Messaging Adapters</objective>
    <change>
      - Create `BCryptPasswordEncoderAdapter` implementing `PasswordEncoderPort` with strength 12.
      - Create `JwtTokenProviderAdapter` implementing `TokenProviderPort` with JJWT 0.12.6, signing, claims injection, and 64-byte secure random refresh token generation.
      - Create `InMemoryAccountLockoutAdapter` implementing `AccountLockoutPort` with sliding window cache (5 attempts / 15 min) and mockable Clock.
      - Create `SpringEventPublisherAdapter` implementing `EventPublisherPort` using Spring's `ApplicationEventPublisher`.
      - Add comprehensive unit tests verifying token claims, BCrypt work factor, sliding window lockout behavior, and event publication.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/**`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/messaging/**`
      - `src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/security/**`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-2: BCrypt strength 12 encoder adapter.
      - [ ] AC-3: JJWT token provider with required claims.
      - [ ] AC-4: Secure opaque refresh token generation.
      - [ ] AC-5: In-memory sliding-window lockout adapter.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "com.platform.app.iam.infrastructure.adapters.secondary.security.*"
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with security and lockout tests passing.</expected_evidence>
    <rollback_point>`git checkout -- src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/messaging`</rollback_point>
    <stop_conditions>
      - JJWT API failure or incompatible BCrypt configuration.
    </stop_conditions>
  </slice>

  <slice id="S4">
    <objective>Implement Secondary Persistence Adapters (JPA Entities, Spring Data Repositories, Adapters)</objective>
    <change>
      - Create JPA entities matching `002-create-iam-tables.yaml`:
        - `UserJpaEntity`, `RoleJpaEntity`, `PermissionJpaEntity`, `RefreshTokenJpaEntity`.
      - Create Spring Data JPA interfaces:
        - `SpringDataUserRepository`, `SpringDataRefreshTokenRepository`.
      - Create persistence adapters implementing outbound ports:
        - `UserRepositoryAdapter` implementing `UserRepositoryPort`.
        - `RefreshTokenRepositoryAdapter` implementing `RefreshTokenRepositoryPort`.
      - Add integration tests with H2 verifying user lookup by email (with eager role/permission joins) and refresh token save/query.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/**`
      - `src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/**`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-1: Persistence adapters implement domain ports.
      - [ ] AC-4: Refresh token persisted with 30-day lifetime and revocation flag.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "com.platform.app.iam.infrastructure.adapters.secondary.persistence.*"
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with JPA integration tests passing on H2.</expected_evidence>
    <rollback_point>`git checkout -- src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence`</rollback_point>
    <stop_conditions>
      - JPA mapping conflict with baseline Liquibase schema.
    </stop_conditions>
  </slice>

  <slice id="S5">
    <objective>Implement Primary REST Adapter, Spring SecurityConfig & Integration Tests</objective>
    <change>
      - Create primary REST adapter in `com.platform.app.iam.infrastructure.adapters.primary.rest`:
        - `AuthController` exposing `POST /api/v1/auth/login`, invoking `LoginUseCase` inbound port, extracting IP/User-Agent.
        - `LoginRequest` (validation annotations `@Email`, `@NotBlank`).
        - `AuthResponse`, `UserProfileResponse`.
        - `RestExceptionHandler` mapping domain exceptions to HTTP responses (400, 401, 403, 423).
      - Create `SecurityConfig` in `com.platform.app.iam.infrastructure.adapters.secondary.security`:
        - Configures `SecurityFilterChain` permitting `/api/v1/auth/login`, `/swagger-ui/**`, `/v3/api-docs/**`, disabling CSRF/session for stateless REST.
      - Add WebMvc MockMvc tests covering 200, 400, 401, 403, 423 paths and event verification.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/**`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/SecurityConfig.java`
      - `src/test/java/com/platform/app/iam/infrastructure/adapters/primary/rest/**`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-7: REST endpoint `POST /api/v1/auth/login` handles all specified HTTP statuses.
      - [ ] AC-8: End-to-end integration tests verify authentication, token issuance, and error handling.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "com.platform.app.iam.infrastructure.adapters.primary.rest.*"
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with WebMvc MockMvc tests passing.</expected_evidence>
    <rollback_point>`git checkout -- src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/SecurityConfig.java`</rollback_point>
    <stop_conditions>
      - SecurityFilterChain blocking authentication endpoint or filter loop.
    </stop_conditions>
  </slice>

  <slice id="S6">
    <objective>Run Full Build, Spotless Code Quality Verification, and Test Suite</objective>
    <change>
      - Run `./gradlew spotlessApply` to enforce Google Java Format.
      - Run `./gradlew check` (compile, tests, spotlessCheck).
    </change>
    <allowed_files>
      - All allowed files from S1–S5.
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-1 through AC-8: All acceptance criteria verified with automated test evidence.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew check
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with 100% passing tests and spotless compliance.</expected_evidence>
    <rollback_point>N/A</rollback_point>
    <stop_conditions>
      - Check failure or lint regression.
    </stop_conditions>
  </slice>

</slices>

---

## 5. Scope Contract

<scope_contract>
  <allowed>
    - Java code and tests strictly under `src/main/java/com/platform/app/iam/**` and `src/test/java/com/platform/app/iam/**`.
  </allowed>
  <forbidden>
    - Any changes to Liquibase changelog files in `src/main/resources/db/changelog/**`.
    - Any changes to `build.gradle` or Gradle wrapper files.
    - Implementation of UC-AUDIT-01 database persistence or audit REST controller.
  </forbidden>
</scope_contract>

---

## 6. Verification Matrix

<verification_matrix>

| AC / Risk | Test / Command | Expected Evidence | Actual Result |
|---|---|---|---|
| AC-1 | `./gradlew test --tests "com.platform.app.iam.domain.*"` | Domain models and value objects pass unit tests | PENDING |
| AC-2 | `./gradlew test --tests "com.platform.app.iam.infrastructure.adapters.secondary.security.BCryptPasswordEncoderAdapterTest"` | BCrypt strength 12 verified | PENDING |
| AC-3 | `./gradlew test --tests "com.platform.app.iam.infrastructure.adapters.secondary.security.JwtTokenProviderAdapterTest"` | JWT claims (userId, departmentId, roleIds, isInternal, permissions) parsed and verified | PENDING |
| AC-4 | `./gradlew test --tests "com.platform.app.iam.infrastructure.adapters.secondary.persistence.RefreshTokenRepositoryAdapterTest"` | Refresh token saved with 30-day expiry and queried | PENDING |
| AC-5 | `./gradlew test --tests "com.platform.app.iam.infrastructure.adapters.secondary.security.InMemoryAccountLockoutAdapterTest"` | 5 failed attempts locks for 15 min, success resets | PENDING |
| AC-6 | `./gradlew test --tests "com.platform.app.iam.application.services.LoginServiceTest"` | Login domain events verified published via EventPublisherPort | PENDING |
| AC-7 | `./gradlew test --tests "com.platform.app.iam.infrastructure.adapters.primary.rest.AuthControllerTest"` | 200 OK, 400 Bad Request, 401 Unauthorized, 403 Forbidden, 423 Locked | PENDING |
| AC-8 | `./gradlew check` | All unit/integration tests pass and Spotless formatting valid | PENDING |

</verification_matrix>

---

## Gate 2 — Plan Approved

<gate id="G2">
  - [x] Every slice has a defined verifier.
  - [x] Scope contract (allowed / forbidden) approved.
  - [x] Stop conditions defined per slice.
  - [x] Rollback point defined per slice.
  - [x] Allowed commands listed.
  - [x] Plan approved.
  <approved_by>@engineer</approved_by>
  <approved_date>2026-09-09</approved_date>
</gate>

</execution_plan>
