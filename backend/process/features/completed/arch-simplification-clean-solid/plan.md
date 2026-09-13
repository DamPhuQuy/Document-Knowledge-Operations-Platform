# Implementation Plan: ARCH-CLEAN-SOLID-SIMPLIFY Streamline Clean Architecture & SOLID Principles

<plan_spec task_id="ARCH-CLEAN-SOLID-SIMPLIFY" version="2.0" framework="RIPER-5">

<!-- PLAN PHASE. Scope contract, slice breakdown, verification matrix. Auto-certify Gate G2 in DELEGATED mode. -->
<plan_status>
  <phase>PLAN</phase>
  <mode>DELEGATED</mode>
  <plan_owner>@engineer</plan_owner>
  <last_updated>2026-09-13</last_updated>
</plan_status>

---

## 1. Scope Contract

<scope_contract>
  <allowed_files>
    <!-- Domain Model updates & deletions -->
    - `src/main/java/com/platform/app/iam/domain/model/User.java`
    - `src/main/java/com/platform/app/iam/domain/model/Role.java`
    - `src/main/java/com/platform/app/iam/domain/model/RefreshToken.java`
    - `src/main/java/com/platform/app/iam/domain/model/UserId.java` (DELETE)
    - `src/main/java/com/platform/app/iam/domain/model/RoleId.java` (DELETE)
    - `src/main/java/com/platform/app/iam/domain/model/DepartmentId.java` (DELETE)
    - `src/main/java/com/platform/app/shared/domain/UserFlags.java` (DELETE)
    - `src/main/java/com/platform/app/shared/domain/AuditMetadata.java` (DELETE)

    <!-- Application Service & Outbound Ports updates & deletions -->
    - `src/main/java/com/platform/app/iam/application/services/LoginService.java`
    - `src/main/java/com/platform/app/iam/application/ports/outbound/PasswordEncoderPort.java` (DELETE)
    - `src/main/java/com/platform/app/iam/application/ports/outbound/EventPublisherPort.java` (DELETE)
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/adapter/BCryptPasswordEncoderAdapter.java` (DELETE)
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/messaging/SpringEventPublisherAdapter.java` (DELETE)

    <!-- Infrastructure & Security updates -->
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/config/SecurityConfig.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/adapter/JwtTokenProviderAdapter.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/adapter/UserRepositoryAdapter.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/adapter/RefreshTokenRepositoryAdapter.java`

    <!-- Tests -->
    - `src/test/java/com/platform/app/iam/domain/model/UserTest.java`
    - `src/test/java/com/platform/app/iam/domain/model/RefreshTokenTest.java`
    - `src/test/java/com/platform/app/iam/application/services/LoginServiceTest.java`
    - `src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/adapter/UserRepositoryAdapterTest.java`
    - `src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/adapter/RefreshTokenRepositoryAdapterTest.java`
    - `src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/security/adapter/JwtTokenProviderAdapterTest.java`
    - `src/test/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AuthControllerTest.java`
  </allowed_files>

  <forbidden_files>
    - `src/main/resources/db/changelog/**` (Do not alter DB schema migrations)
    - `build.gradle` (Dependencies remain constant)
    - `src/main/resources/application*.yaml`
  </forbidden_files>
</scope_contract>

---

## 2. Slice Breakdown

### Slice 1: Streamline Domain Models
- Update `User.java` to use native `UUID id`, `String departmentId`, `boolean enabled`, `boolean internal`, `Instant createdAt`, `Instant updatedAt`. Remove `UserFlags` and `AuditMetadata` wrapper records.
- Update `Role.java` to use native `UUID id`. Remove `RoleId.java`.
- Update `RefreshToken.java` to use native `UUID id`, `UUID userId`.
- Delete `UserId.java`, `RoleId.java`, `DepartmentId.java`, `UserFlags.java`, `AuditMetadata.java`.

### Slice 2: Adopt Framework-Standard Abstractions in Application Services & Security
- In `LoginService.java`, inject `org.springframework.security.crypto.password.PasswordEncoder` and `org.springframework.context.ApplicationEventPublisher` directly.
- Delete `PasswordEncoderPort.java`, `BCryptPasswordEncoderAdapter.java`, `EventPublisherPort.java`, `SpringEventPublisherAdapter.java`.
- In `SecurityConfig.java`, expose standard `@Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }`.

### Slice 3: Streamline Repositories & Token Adapters
- In `UserRepositoryAdapter.java`, simplify mapping directly from `UserJpaEntity` to `User` without nested record constructors.
- In `RefreshTokenRepositoryAdapter.java`, simplify mapping.
- In `JwtTokenProviderAdapter.java`, adjust claims generation to read `user.getId()` (UUID directly).

### Slice 4: Test Suite Synchronization & Full Verification
- Update unit tests (`LoginServiceTest`, `UserTest`, `RefreshTokenTest`, `UserRepositoryAdapterTest`, `JwtTokenProviderAdapterTest`) to match streamlined signatures.
- Execute `./gradlew test` and `./gradlew check` to verify 100% passing tests with zero regressions.

---

## 3. Verification Matrix

| Slice | Description | Verification Command | Expected Output |
|---|---|---|---|
| Slice 1 | Domain model simplification | `./gradlew compileJava` | Compiles or shows expected call-site diffs |
| Slice 2 | Ports & security streamlining | `./gradlew compileJava` | Compiles clean |
| Slice 3 | Persistence & JWT adapter updates | `./gradlew compileJava` | Compiles clean |
| Slice 4 | Test synchronization & full test run | `./gradlew test` | BUILD SUCCESSFUL (25/25 tests passing) |

---

## 4. Gate 2 Sign-Off

<gate_2_signoff status="PASS">
  [AUTO: DELEGATED] Gate 2 certified under autonomous fast-track mode. Advancing to EXECUTE phase.
</gate_2_signoff>

</plan_spec>
