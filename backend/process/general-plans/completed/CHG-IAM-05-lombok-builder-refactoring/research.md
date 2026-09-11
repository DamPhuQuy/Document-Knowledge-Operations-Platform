# Research: CHG-IAM-05 Adopt Lombok @Builder Pattern for Object Creation

<research_context task_id="CHG-IAM-05" version="1.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. No source modifications. No implementation decisions. -->
<research_status>
  <phase>RESEARCH</phase>
  <mode>READ-ONLY</mode>
  <research_owner>@engineer</research_owner>
  <last_updated>2026-09-12</last_updated>
</research_status>

---

## 1. Current Behavior & Baseline Findings

<current_behavior>
  The codebase currently uses mixed object creation patterns: positional constructors via `new ClassName(...)`, record constructors, and partially adopted builders.

  ### Confirmed Findings:
  1. **Lombok Support & Readiness:** [CONFIRMED] `build.gradle` configures Lombok across both `compileOnly` / `annotationProcessor` and `testCompileOnly` / `testAnnotationProcessor`. Lombok `@Builder` is already operational in the project.
  2. **Domain Models Object Creation:** [CONFIRMED]
     - `User`: Has `@Builder` on the class, but private constructor `User(...)` taking `(UserId, String, String, String, DepartmentId, UserFlags, Set<Role>, AuditMetadata)`. Legacy tests and call sites attempting `new User(id, email, passwordHash, fullName, deptId, boolean, boolean, roles, Instant, Instant)` fail compilation because `UserFlags` and `AuditMetadata` replaced the primitive flags and timestamps, and `user.isEnabled()` / `user.isInternal()` helper methods were omitted.
     - `Role`: Has `@Builder` and a public all-args constructor.
     - `Permission`: Has `@Builder` and a public all-args constructor.
     - `RefreshToken`: Still uses manual `new RefreshToken(...)` constructor with 6 arguments in both factory method and persistence adapter.
  3. **JPA Entities:** [CONFIRMED]
     - `UserJpaEntity`: Has `@NoArgsConstructor` and `@AllArgsConstructor`, but lacks `@Builder`. Initialized fields (`private Set<RoleJpaEntity> roles = new HashSet<>();`) need `@Builder.Default` when builder is introduced to prevent null-collection states.
     - `RoleJpaEntity`: Lacks `@Builder`. Initialized `private Set<PermissionJpaEntity> permissions = new HashSet<>();` needs `@Builder.Default`.
     - `PermissionJpaEntity`: Lacks `@Builder`.
     - `RefreshTokenJpaEntity`: Lacks `@Builder`.
  4. **Persistence Adapters:** [CONFIRMED]
     - `UserRepositoryAdapter`: Already refactored to use `User.builder()`, `Role.builder()`, and `Permission.builder()`.
     - `RefreshTokenRepositoryAdapter`: Still constructs `new RefreshTokenJpaEntity(...)` and `new RefreshToken(...)` with positional constructors.
  5. **DTOs, Commands, and Events:** [CONFIRMED]
     - `UserProfileDto`: 7-parameter Java record instantiated via `new UserProfileDto(...)` in `LoginService`.
     - `UserLoginSuccessEvent` & `UserLoginFailedEvent`: Multi-parameter record events instantiated via `new` in `LoginService`.
     - `LoginCommand`: 4-parameter record instantiated via `new LoginCommand(...)` in `AuthController`.
     - `LoginRequest`: Record instantiated via `new LoginRequest(...)` in `AuthControllerTest`.
     - `ErrorResponse`: 5-parameter record instantiated in 5 error handler methods in `RestExceptionHandler` via `new ErrorResponse(...)`.
     - `JwtProperties`: Record instantiated in tests via `new JwtProperties(...)`.
  6. **Test Compilation Baseline:** [CONFIRMED] Running `./gradlew test` fails with 11 compilation errors due to outdated `new User(...)` calls in `UserTest`, `JwtTokenProviderAdapterTest`, `PersistenceAdaptersTest`, and `LoginServiceTest`, and missing `user.isEnabled()` / `user.isInternal()` delegate methods on `User`.

  ### Observed Invariants & Constraints:
  1. [OBSERVED] **JPA Entity Safety with `@Builder`:**
     - In JPA entities with default-initialized collection fields (`roles`, `permissions`), Lombok's `@Builder` will overwrite default initializers with `null` unless `@Builder.Default` is specified.
     - JPA entities must retain `@NoArgsConstructor` (for Hibernate proxies) and `@AllArgsConstructor` (required by Lombok when combining with `@Builder` or provided via package-private constructor).
  2. [OBSERVED] **Domain Aggregate Encapsulation:**
     - Aggregates (`User`, `Role`) must protect collection invariants (`Collections.unmodifiableSet(...)`) and enforce non-null validations. The builder must funnel through validation logic or enforce null-safety.
     - `User` must provide convenience query methods (`isEnabled()`, `isInternal()`, `getCreatedAt()`, `getUpdatedAt()`) delegating to `UserFlags` and `AuditMetadata`.
  3. [OBSERVED] **Lombok on Records:**
     - Lombok `@Builder` is supported on Java records since Lombok 1.18.20+. It generates a static builder that delegates to the canonical constructor, preserving record immutability and compact syntax.

  ### Hypothesized Scope:
  - Adding `@Builder` to: `RefreshToken`, `UserJpaEntity`, `RoleJpaEntity`, `PermissionJpaEntity`, `RefreshTokenJpaEntity`, `UserProfileDto`, `UserLoginSuccessEvent`, `UserLoginFailedEvent`, `LoginCommand`, `LoginRequest`, `ErrorResponse`, `JwtProperties`.
  - Adding backward-compatible accessors to `User`: `isEnabled()`, `isInternal()`, `getCreatedAt()`, `getUpdatedAt()`.
  - Refactoring instantiations in `RefreshTokenRepositoryAdapter`, `LoginService`, `AuthController`, and test fixtures to `.builder().build()`.
</current_behavior>

---

## 2. Inventory & Classification of Candidate Files

<file_inventory>

| Target File | Current Instantiation Style | Proposed Improvement | Risk Level |
|---|---|---|---|
| `domain/model/User.java` | `@Builder` present, missing convenience delegates | Add `isEnabled()`, `isInternal()`, `getCreatedAt()`, `getUpdatedAt()` | LOW |
| `domain/model/RefreshToken.java` | `new RefreshToken(...)` | Add `@Builder` | LOW |
| `persistence/entity/UserJpaEntity.java` | `new UserJpaEntity(...)` (all-arg) | Add `@Builder`, `@Builder.Default` on `roles` | LOW |
| `persistence/entity/RoleJpaEntity.java` | `new RoleJpaEntity(...)` (all-arg) | Add `@Builder`, `@Builder.Default` on `permissions` | LOW |
| `persistence/entity/PermissionJpaEntity.java` | `new PermissionJpaEntity(...)` | Add `@Builder` | LOW |
| `persistence/entity/RefreshTokenJpaEntity.java` | `new RefreshTokenJpaEntity(...)` | Add `@Builder` | LOW |
| `persistence/adapter/RefreshTokenRepositoryAdapter.java` | `new RefreshTokenJpaEntity(...)` and `new RefreshToken(...)` | Use `.builder()` for entity & domain mapping | LOW |
| `application/dto/UserProfileDto.java` | `new UserProfileDto(...)` | Add `@Builder` | LOW |
| `application/dto/UserLoginSuccessEvent.java` | `new UserLoginSuccessEvent(...)` | Add `@Builder` | LOW |
| `application/dto/UserLoginFailedEvent.java` | `new UserLoginFailedEvent(...)` | Add `@Builder` | LOW |
| `application/ports/inbound/LoginCommand.java` | `new LoginCommand(...)` | Add `@Builder` | LOW |
| `infrastructure/.../ErrorResponse.java` | `new ErrorResponse(...)` | Add `@Builder` | LOW |
| `infrastructure/.../LoginRequest.java` | `new LoginRequest(...)` | Add `@Builder` | LOW |
| `infrastructure/.../JwtProperties.java` | `new JwtProperties(...)` | Add `@Builder` | LOW |
| `test/.../UserTest.java` | Broken `new User(...)`, `new Permission(...)`, `new Role(...)` | Migrate to fluent `.builder().build()` | LOW |
| `test/.../LoginServiceTest.java` | Broken `new User(...)` | Migrate to fluent `.builder().build()` | LOW |
| `test/.../AuthControllerTest.java` | `new UserJpaEntity(...)`, `new LoginRequest(...)` | Migrate to fluent `.builder().build()` | LOW |
| `test/.../JwtTokenProviderAdapterTest.java` | Broken `new User(...)` | Migrate to fluent `.builder().build()` | LOW |
| `test/.../PersistenceAdaptersTest.java` | Broken `isEnabled()`, `new UserJpaEntity(...)` | Migrate to fluent `.builder().build()` | LOW |

</file_inventory>

---

## 3. Execution Flow & Architecture Boundaries

<execution_flow>
  The refactoring is structural and ergonomic:
  - **Clean Architecture & DDD Invariants:** Domain models (`User`, `Role`, `Permission`, `RefreshToken`) retain pure Java domain rules and zero infrastructure/JPA dependencies.
  - **Encapsulation:** Aggregates protect their collections via unmodifiable views. Builders do not bypass validation or internal invariants.
  - **Compilation & Verification:** Migrating tests and adapters to builders ensures that future model evolution (e.g. adding metadata or flags) will not break tests or call sites reliant on fragile positional parameter lists.
</execution_flow>

---

## 4. Research Exit Criteria (Gate G0)

<research_exit_criteria>
  - [x] Baseline findings confirmed and documented.
  - [x] Codebase scan completed for candidate classes, entities, DTOs, records, and tests.
  - [x] Invariants (JPA `@Builder.Default`, aggregate encapsulation, record builder compatibility) identified.
  - [x] Target file inventory established.
  - [x] No unresolved research blockers.
</research_exit_criteria>

</research_context>
