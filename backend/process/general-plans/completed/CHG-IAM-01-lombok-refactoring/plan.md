# Plan: PLAN-IAM-01 Lombok Refactoring for IAM Subsystem

<execution_plan task_id="CHG-IAM-01" plan_id="PLAN-IAM-01" version="2.0" framework="RIPER-5">

<!-- PLAN PHASE. Plan artifacts only. No source-code changes. -->
<plan_status>
  <phase>PLAN</phase>
  <last_updated>2026-09-11</last_updated>
</plan_status>

---

## 1. Input Artifacts

<input_artifacts>
  <task_spec>process/general-plans/active/CHG-IAM-01-lombok-refactoring/task.md</task_spec>
  <research>process/general-plans/active/CHG-IAM-01-lombok-refactoring/research.md</research>
  <decision>process/general-plans/active/CHG-IAM-01-lombok-refactoring/decision.md (DEC-IAM-01: Option A)</decision>
</input_artifacts>

---

## 2. Execution Constraints

<execution_constraints>
  <allowed_files>
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/UserJpaEntity.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/RoleJpaEntity.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/PermissionJpaEntity.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/RefreshTokenJpaEntity.java`
    - `src/main/java/com/platform/app/iam/application/services/LoginService.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AuthController.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/UserRepositoryAdapter.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/RefreshTokenRepositoryAdapter.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/messaging/SpringEventPublisherAdapter.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java`
    - `src/main/java/com/platform/app/iam/domain/model/User.java`
    - `src/main/java/com/platform/app/iam/domain/model/Role.java`
    - `src/main/java/com/platform/app/iam/domain/model/RefreshToken.java`
    - `src/main/java/com/platform/app/iam/domain/model/Permission.java`
  </allowed_files>
  <forbidden_files>
    - `src/main/resources/**` — (Configuration and Liquibase migrations must not be altered)
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/SecurityConfig.java` — (Security policy intact)
    - Java records: `UserId.java`, `RoleId.java`, `DepartmentId.java`, `AuthResponse.java`, `LoginRequest.java`, `ErrorResponse.java`, `UserProfileDto.java`, `AuthTokensDto.java`, `LoginCommand.java`, `UserLoginSuccessEvent.java`, `UserLoginFailedEvent.java`
    - Any file outside `src/main/java/com/platform/app/iam/`
  </forbidden_files>
  <allowed_commands>
    - `./gradlew test --tests <pattern>`
    - `./gradlew test`
    - `./gradlew spotlessCheck`
    - `./gradlew spotlessApply`
    - `./gradlew check`
    - `git diff` / `git status`
  </allowed_commands>
  <restricted_operations>
    - No changes to public API schemas or endpoints.
    - No changes to database schema or migrations.
    - No use of `@Data` on JPA entities.
    - No `@Setter` on domain models (`User`, `Role`).
  </restricted_operations>
  <required_approvals>
    - Gate 2 approval prior to entering EXECUTE phase.
  </required_approvals>
</execution_constraints>

---

## 3. Slice Summary

<slice_summary>

| ID | Behavior | Boundary | Files | AC | Verifier | Risk | Mode | Rollback |
|---|---|---|---|---|---|---|---|---|
| S1 | Refactor JPA Entities to safe Lombok annotations | Infrastructure Persistence | 4 files | AC-1 | `./gradlew test --tests "*PersistenceAdaptersTest*"` | LOW | READ-WRITE | `git checkout -- src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/*JpaEntity.java` |
| S2 | Refactor Spring DI & Exception Logging | Application & Infrastructure Adapters | 6 files | AC-2, AC-3 | `./gradlew test --tests "*LoginServiceTest*" && ./gradlew test --tests "*AuthControllerTest*"` | LOW | READ-WRITE | `git checkout -- src/main/java/com/platform/app/iam/application/services/LoginService.java ...` |
| S3 | Refactor Domain Entities with safe @Getter and @EqualsAndHashCode | Domain Models | 4 files | AC-4 | `./gradlew test --tests "*UserTest*" && ./gradlew test --tests "*RefreshTokenTest*"` | LOW | READ-WRITE | `git checkout -- src/main/java/com/platform/app/iam/domain/model/*.java` |
| S4 | Full test suite verification & Spotless formatting | Entire Subsystem | N/A | AC-5 | `./gradlew spotlessApply && ./gradlew check` | LOW | READ-WRITE | `git checkout .` |

</slice_summary>

---

## 4. Slice Details

<slices>

  <slice id="S1">
    <objective>Eliminate boilerplate in JPA entities using safe Lombok annotations without lazy-load risks</objective>
    <change>
      In `UserJpaEntity.java`, `RoleJpaEntity.java`, `PermissionJpaEntity.java`, `RefreshTokenJpaEntity.java`:
      - Annotate with `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`.
      - Annotate with `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` and mark `@Id` with `@EqualsAndHashCode.Include`.
      - Remove manual getters, setters, constructors, equals, and hashCode.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/UserJpaEntity.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/RoleJpaEntity.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/PermissionJpaEntity.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/RefreshTokenJpaEntity.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-1: JPA entities refactored with safe Lombok annotations; manual getters/setters/equals/hashCode removed.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "*PersistenceAdaptersTest*"
      ```
    </verifier>
    <expected_evidence>PersistenceAdaptersTest passes with all JPA queries and mappings functioning identically.</expected_evidence>
    <rollback_point>git checkout -- src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/*JpaEntity.java</rollback_point>
    <stop_conditions>
      - Any compilation failure or LazyInitializationException in tests.
    </stop_conditions>
  </slice>

  <slice id="S2">
    <objective>Simplify Spring Bean DI with @RequiredArgsConstructor and logging with @Slf4j</objective>
    <change>
      - Add `@RequiredArgsConstructor` to `LoginService.java`, `AuthController.java`, `UserRepositoryAdapter.java`, `RefreshTokenRepositoryAdapter.java`, `SpringEventPublisherAdapter.java`. Remove manual constructor boilerplate.
      - Add `@Slf4j` to `RestExceptionHandler.java`. Remove `private static final Logger log = LoggerFactory.getLogger(...)`.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/iam/application/services/LoginService.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AuthController.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/UserRepositoryAdapter.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/RefreshTokenRepositoryAdapter.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/messaging/SpringEventPublisherAdapter.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-2: Spring components/services/controllers/adapters refactored using `@RequiredArgsConstructor`.
      - [ ] AC-3: `RestExceptionHandler` refactored using `@Slf4j`.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "*LoginServiceTest*" && ./gradlew test --tests "*AuthControllerTest*"
      ```
    </verifier>
    <expected_evidence>Unit and controller slice tests pass with mock injections intact.</expected_evidence>
    <rollback_point>git checkout -- src/main/java/com/platform/app/iam/application/services/LoginService.java src/main/java/com/platform/app/iam/infrastructure/adapters/</rollback_point>
    <stop_conditions>
      - Spring context initialization errors or Mockito instantiation mismatch.
    </stop_conditions>
  </slice>

  <slice id="S3">
    <objective>Refactor Domain Model Entities with selective @Getter and @EqualsAndHashCode</objective>
    <change>
      In `User.java`, `Role.java`, `RefreshToken.java`, `Permission.java`:
      - Annotate with `@Getter` (for standard fields).
      - Annotate with `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` on entity ID or unique code.
      - Keep custom constructors, invariant validations (e.g. lowercase email, null checks), and custom methods returning unmodifiable collections (`getRoles()`, `getPermissions()`).
      - Remove redundant manual getters, equals, and hashCode.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/iam/domain/model/User.java`
      - `src/main/java/com/platform/app/iam/domain/model/Role.java`
      - `src/main/java/com/platform/app/iam/domain/model/RefreshToken.java`
      - `src/main/java/com/platform/app/iam/domain/model/Permission.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-4: Domain models refactored cleanly while preserving domain encapsulation and unmodifiable collection access.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "*UserTest*" && ./gradlew test --tests "*RefreshTokenTest*"
      ```
    </verifier>
    <expected_evidence>Domain unit tests pass, verifying invariants, unmodifiable collections, and entity equality.</expected_evidence>
    <rollback_point>git checkout -- src/main/java/com/platform/app/iam/domain/model/*.java</rollback_point>
    <stop_conditions>
      - Domain encapsulation breakage or collection mutability leakage.
    </stop_conditions>
  </slice>

  <slice id="S4">
    <objective>Enforce Spotless code formatting and execute complete test suite</objective>
    <change>
      - Run `./gradlew spotlessApply` to ensure standard code formatting.
      - Execute `./gradlew check` to verify all tests and static analysis.
    </change>
    <allowed_files>
      - (All permitted target files modified by formatting)
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-5: Spotless check and all tests pass with zero regressions.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew spotlessCheck && ./gradlew test
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with 100% tests green and spotless spotlessCheck clean.</expected_evidence>
    <rollback_point>git checkout .</rollback_point>
    <stop_conditions>
      - Any test failure or unresolvable formatting error.
    </stop_conditions>
  </slice>

</slices>

---

## 5. Scope Contract

<scope_contract>
  <allowed>
    - Refactoring target files in `src/main/java/com/platform/app/iam/` using safe Lombok annotations.
    - Applying Spotless formatter (`./gradlew spotlessApply`).
    - Running verification test commands via Gradle.
  </allowed>
  <forbidden>
    - No changes to Java records or DTO contracts.
    - No changes to security configurations or Liquibase migrations.
    - No changes to test files (`src/test/java/**`).
    - No changes to modules outside `iam`.
  </forbidden>
</scope_contract>

---

## 6. Verification Matrix

<verification_matrix>

| AC / Risk | Test / Command | Expected Evidence | Actual Result |
|---|---|---|---|
| AC-1 | `./gradlew test --tests "*PersistenceAdaptersTest*"` | JPA entity mapping & persistence tests pass | PASS (3 executed, 0 failures) |
| AC-2 | `./gradlew test --tests "*LoginServiceTest*"` | Service constructor injection tests pass | PASS (all assertions passed) |
| AC-3 | `./gradlew test --tests "*AuthControllerTest*"` | Controller & exception logging tests pass | PASS (all mock/mvc tests passed) |
| AC-4 | `./gradlew test --tests "*UserTest*" && ./gradlew test --tests "*RefreshTokenTest*"` | Domain logic & equality tests pass | PASS (domain invariants preserved) |
| AC-5 | `./gradlew spotlessCheck && ./gradlew test` | Full build and check passes | PASS (BUILD SUCCESSFUL, spotless clean) |

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
  <approved_date>2026-09-11</approved_date>
</gate>

</execution_plan>
