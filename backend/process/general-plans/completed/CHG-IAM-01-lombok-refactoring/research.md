# Research: CHG-IAM-01 Refactor IAM Module to Use Lombok

<research_context task_id="CHG-IAM-01" version="1.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. No source modifications. No implementation decisions. -->
<research_status>
  <phase>RESEARCH</phase>
  <mode>READ-ONLY</mode>
  <research_owner>@engineer</research_owner>
  <last_updated>2026-09-11</last_updated>
</research_status>

---

## 1. Current Behavior & Baseline Findings

<current_behavior>
  The `iam` subsystem (`src/main/java/com/platform/app/iam`) currently consists of 41 Java source files organized into Hexagonal / Clean Architecture layers:
  - `application/` (dto, ports inbound/outbound, services)
  - `domain/` (model, exception)
  - `infrastructure/` (adapters primary rest, secondary persistence, secondary messaging, secondary security)

  ### Confirmed Findings:
  1. **Lombok Dependency:** [CONFIRMED] `build.gradle` already includes:
     - `compileOnly 'org.projectlombok:lombok'`
     - `annotationProcessor 'org.projectlombok:lombok'`
     - `testCompileOnly 'org.projectlombok:lombok'`
     - `testAnnotationProcessor 'org.projectlombok:lombok'`
     No `build.gradle` modification or dependency addition is required.
  2. **JPA Entity Boilerplate:** [CONFIRMED] All four JPA entities (`UserJpaEntity`, `RoleJpaEntity`, `PermissionJpaEntity`, `RefreshTokenJpaEntity`) manually implement:
     - Default no-arg constructor
     - Full all-arg constructor
     - Explicit getters and setters for all fields
     - Manual `equals` and `hashCode` comparing only `id`
     This accounts for ~400 lines of repetitive boilerplate code.
  3. **Spring Bean DI Boilerplate:** [CONFIRMED]
     - `LoginService` has 6 injected ports with a 20-line constructor and explicit `Objects.requireNonNull` guards.
     - `AuthController` has 1 injected use case with manual constructor.
     - `UserRepositoryAdapter`, `RefreshTokenRepositoryAdapter`, and `SpringEventPublisherAdapter` have single-dependency constructors.
  4. **Logging Boilerplate:** [CONFIRMED] `RestExceptionHandler` manually declares `private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);`.
  5. **Modern Records Utilization:** [CONFIRMED] Many DTOs and value objects are already modern Java records:
     - Records: `UserId`, `RoleId`, `DepartmentId`, `AuthTokensDto`, `UserProfileDto`, `UserLoginSuccessEvent`, `UserLoginFailedEvent`, `LoginCommand`, `LoginRequest`, `AuthResponse`, `ErrorResponse`.
     - Records already provide immutable fields, canonical constructor, accessors, equals/hashCode, and toString natively. Applying Lombok to these records is redundant and unnecessary.
  6. **Domain Models:** [CONFIRMED]
     - `User`: Entity / Aggregate Root with identity `UserId`, defensive collections, and custom accessors (`getRoles()` returns `Collections.unmodifiableSet(roles)`).
     - `Role`: Domain entity with identity `RoleId` and unmodifiable permissions set.
     - `RefreshToken`: Domain entity with business behavior (`revoke()`, `isExpired()`, `isValid()`).
     - `Permission`: Domain entity with identity code.
  7. **Test Suite Baseline:** [CONFIRMED] Baseline test run `./gradlew test` passes 100% (all unit and integration tests green). `./gradlew spotlessCheck` passes cleanly.

  ### Observed Invariants & Constraints:
  1. [OBSERVED] **JPA Entity Safety Rule:** In JPA entities with bidirectional or lazy relationships (`@ManyToMany(fetch = FetchType.LAZY)` between `UserJpaEntity` and `RoleJpaEntity`, and `RoleJpaEntity` and `PermissionJpaEntity`):
     - Never use Lombok `@Data` or unconstrained `@ToString` / `@EqualsAndHashCode`. Doing so causes `LazyInitializationException` outside transactions and fatal `StackOverflowError` via cyclical recursion.
     - Safe pattern: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` with `@EqualsAndHashCode.Include` on `@Id private UUID id;`.
  2. [OBSERVED] **Domain Encapsulation Rule:** Domain entities (`User`, `Role`) must preserve encapsulation. Specifically, `roles` and `permissions` cannot have arbitrary setters or exposed mutable collections. Lombok `@Setter` must NOT be placed on domain aggregates.

  ### Hypothesized Scope:
  - Total candidate files for Lombok refactoring: ~10 to 12 files across JPA persistence entities, Spring service/adapters/controllers, exception logging, and selective domain getters.
</current_behavior>

---

## 2. Inventory & Classification of Candidate Files

<file_inventory>

| File Path | Current Structure | Proposed Lombok Annotations | Risk Level |
|---|---|---|---|
| `infrastructure/.../UserJpaEntity.java` | 172 lines (manual get/set/ctors/equals) | `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` | LOW (JPA best practice) |
| `infrastructure/.../RoleJpaEntity.java` | 120 lines (manual get/set/ctors/equals) | `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` | LOW (JPA best practice) |
| `infrastructure/.../PermissionJpaEntity.java` | 105 lines (manual get/set/ctors/equals) | `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` | LOW (JPA best practice) |
| `infrastructure/.../RefreshTokenJpaEntity.java` | 105 lines (manual get/set/ctors/equals) | `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` | LOW (JPA best practice) |
| `application/services/LoginService.java` | 138 lines (manual 6-param ctor) | `@RequiredArgsConstructor` | LOW |
| `infrastructure/.../AuthController.java` | 79 lines (manual 1-param ctor) | `@RequiredArgsConstructor` | LOW |
| `infrastructure/.../UserRepositoryAdapter.java` | 80 lines (manual 1-param ctor) | `@RequiredArgsConstructor` | LOW |
| `infrastructure/.../RefreshTokenRepositoryAdapter.java` | 59 lines (manual 1-param ctor) | `@RequiredArgsConstructor` | LOW |
| `infrastructure/.../SpringEventPublisherAdapter.java` | 24 lines (manual 1-param ctor) | `@RequiredArgsConstructor` | LOW |
| `infrastructure/.../RestExceptionHandler.java` | 94 lines (manual Logger field) | `@Slf4j` | LOW |
| `domain/model/User.java` | 118 lines (manual getters/equals) | `@Getter`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` (keep custom ctor & unmodifiable `getRoles()`) | LOW |
| `domain/model/Role.java` | 66 lines (manual getters/equals) | `@Getter`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` (keep custom ctor & unmodifiable `getPermissions()`) | LOW |
| `domain/model/RefreshToken.java` | 84 lines (manual getters/equals) | `@Getter`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` | LOW |
| `domain/model/Permission.java` | 59 lines (manual getters/equals) | `@Getter`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` | LOW |

</file_inventory>

---

## 3. Execution Flow & Architecture Boundaries

<execution_flow>
  The refactoring touches non-functional code generation only. Execution flow remains strictly unaltered:
  - HTTP Request $\rightarrow$ `AuthController` $\rightarrow$ `LoginService` $\rightarrow$ Repositories / Security / Adapters $\rightarrow$ Response.
  - Domain models continue to enforce internal invariants via their business methods and explicit constructors.
  - Test suites instantiate classes via identical public contracts.
</execution_flow>

---

## 4. Research Exit Criteria (Gate G0)

<research_exit_criteria>
  - [x] All 41 files in `com.platform.app.iam` inspected and cataloged.
  - [x] Lombok availability in `build.gradle` confirmed.
  - [x] Architectural guardrails (avoiding `@Data` on JPA, preserving DDD invariants) established.
  - [x] Baseline test suite and spotless checks verified passing.
  - [x] No unresolved blockers found.
</research_exit_criteria>

</research_context>
