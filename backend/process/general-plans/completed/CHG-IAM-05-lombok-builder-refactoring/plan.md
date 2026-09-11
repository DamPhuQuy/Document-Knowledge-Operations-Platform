# Plan: PLAN-CHG-IAM-05 Adopt Lombok @Builder Pattern for Object Creation

<execution_plan task_id="CHG-IAM-05" plan_id="PLAN-CHG-IAM-05" version="2.0" framework="RIPER-5">

<!-- PLAN PHASE. Plan artifacts only. No source-code changes. -->
<plan_status>
  <phase>PLAN</phase>
  <last_updated>2026-09-12</last_updated>
</plan_status>

---

## 1. Input Artifacts

<input_artifacts>
  <task_spec>process/general-plans/active/CHG-IAM-05-lombok-builder-refactoring/task.md</task_spec>
  <research>process/general-plans/active/CHG-IAM-05-lombok-builder-refactoring/research.md</research>
  <decision>process/general-plans/active/CHG-IAM-05-lombok-builder-refactoring/decision.md (DEC-CHG-IAM-05, Option A)</decision>
</input_artifacts>

---

## 2. Execution Constraints

<execution_constraints>
  <allowed_files>
    - `src/main/java/com/platform/app/iam/domain/model/User.java`
    - `src/main/java/com/platform/app/iam/domain/model/RefreshToken.java`
    - `src/main/java/com/platform/app/iam/domain/model/Role.java`
    - `src/main/java/com/platform/app/iam/domain/model/Permission.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/entity/UserJpaEntity.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/entity/RoleJpaEntity.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/entity/PermissionJpaEntity.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/entity/RefreshTokenJpaEntity.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/adapter/RefreshTokenRepositoryAdapter.java`
    - `src/main/java/com/platform/app/iam/application/dto/UserProfileDto.java`
    - `src/main/java/com/platform/app/iam/application/dto/UserLoginSuccessEvent.java`
    - `src/main/java/com/platform/app/iam/application/dto/UserLoginFailedEvent.java`
    - `src/main/java/com/platform/app/iam/application/ports/inbound/LoginCommand.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/ErrorResponse.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/LoginRequest.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/config/JwtProperties.java`
    - `src/main/java/com/platform/app/iam/application/services/LoginService.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AuthController.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java`
    - `src/test/java/com/platform/app/iam/domain/model/UserTest.java`
    - `src/test/java/com/platform/app/iam/application/services/LoginServiceTest.java`
    - `src/test/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AuthControllerTest.java`
    - `src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/security/JwtTokenProviderAdapterTest.java`
    - `src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/adapter/PersistenceAdaptersTest.java`
  </allowed_files>
  <forbidden_files>
    - `src/main/resources/**`
    - `build.gradle`
    - Database migrations / Liquibase changelogs
  </forbidden_files>
  <allowed_commands>
    - `./gradlew compileJava`
    - `./gradlew compileTestJava`
    - `./gradlew test`
    - `./gradlew spotlessApply`
    - `./gradlew spotlessCheck`
  </allowed_commands>
  <restricted_operations>
    - No schema modifications.
    - No changes to public API response format.
  </restricted_operations>
  <required_approvals>
    - None (DELEGATED mode active).
  </required_approvals>
</execution_constraints>

---

## 3. Slice Summary

<slice_summary>

| ID | Behavior | Boundary | Files | AC | Verifier | Risk | Mode | Rollback |
|---|---|---|---|---|---|---|---|---|
| S1 | Domain Models & Convenience Delegates | Domain Model | `User.java`, `RefreshToken.java` | AC-1 | `./gradlew compileJava` | LOW | R/W | `git checkout -- <files>` |
| S2 | JPA Entities & Persistence Adapters | Persistence | `*JpaEntity.java`, `RefreshTokenRepositoryAdapter.java` | AC-1, AC-2 | `./gradlew compileJava` | LOW | R/W | `git checkout -- <files>` |
| S3 | Application Records & Usages | Application / REST | DTOs, events, commands, `LoginService.java`, `AuthController.java`, `RestExceptionHandler.java` | AC-1, AC-3 | `./gradlew compileJava` | LOW | R/W | `git checkout -- <files>` |
| S4 | Test Suite Fixture Migration | Tests | `UserTest.java`, `LoginServiceTest.java`, `AuthControllerTest.java`, `JwtTokenProviderAdapterTest.java`, `PersistenceAdaptersTest.java` | AC-4, AC-5 | `./gradlew test` && `./gradlew spotlessCheck` | LOW | R/W | `git checkout -- <files>` |

</slice_summary>

---

## 4. Slice Details

<slices>

  <slice id="S1">
    <objective>Enhance domain models with @Builder and provide backward-compatible convenience methods on User</objective>
    <change>
      - Add `@Builder` to `RefreshToken.java`.
      - Add convenience delegate methods on `User.java`: `isEnabled()`, `isInternal()`, `getCreatedAt()`, `getUpdatedAt()`.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/iam/domain/model/User.java`
      - `src/main/java/com/platform/app/iam/domain/model/RefreshToken.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-1: Domain models support `@Builder` and expose required getters.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew compileJava
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL</expected_evidence>
    <rollback_point>git checkout -- src/main/java/com/platform/app/iam/domain/model/</rollback_point>
    <stop_conditions>
      - Lombok compilation error in domain layer.
    </stop_conditions>
  </slice>

  <slice id="S2">
    <objective>Add @Builder to JPA entities and refactor RefreshTokenRepositoryAdapter</objective>
    <change>
      - Add `@Builder` to `UserJpaEntity` (`@Builder.Default` on `roles`), `RoleJpaEntity` (`@Builder.Default` on `permissions`), `PermissionJpaEntity`, and `RefreshTokenJpaEntity`.
      - Update `RefreshTokenRepositoryAdapter` to use `RefreshTokenJpaEntity.builder()` and `RefreshToken.builder()`.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/entity/UserJpaEntity.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/entity/RoleJpaEntity.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/entity/PermissionJpaEntity.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/entity/RefreshTokenJpaEntity.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/adapter/RefreshTokenRepositoryAdapter.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-1: JPA entities support `@Builder` safely.
      - [ ] AC-2: RefreshTokenRepositoryAdapter maps via fluent builders.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew compileJava
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL</expected_evidence>
    <rollback_point>git checkout -- src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/</rollback_point>
    <stop_conditions>
      - JPA mapping or compilation errors.
    </stop_conditions>
  </slice>

  <slice id="S3">
    <objective>Annotate multi-parameter records with @Builder and update application services/handlers</objective>
    <change>
      - Add `@Builder` to `UserProfileDto`, `UserLoginSuccessEvent`, `UserLoginFailedEvent`, `LoginCommand`, `LoginRequest`, `ErrorResponse`, `JwtProperties`.
      - Refactor `LoginService.java`, `AuthController.java`, `RestExceptionHandler.java` to instantiate them using `.builder()...build()`.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/iam/application/dto/UserProfileDto.java`
      - `src/main/java/com/platform/app/iam/application/dto/UserLoginSuccessEvent.java`
      - `src/main/java/com/platform/app/iam/application/dto/UserLoginFailedEvent.java`
      - `src/main/java/com/platform/app/iam/application/ports/inbound/LoginCommand.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/ErrorResponse.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/LoginRequest.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/config/JwtProperties.java`
      - `src/main/java/com/platform/app/iam/application/services/LoginService.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AuthController.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-1: Records have `@Builder`.
      - [ ] AC-3: Services and handlers construct events/DTOs with builders.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew compileJava
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL</expected_evidence>
    <rollback_point>git checkout -- src/main/java/com/platform/app/iam/application/ src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/</rollback_point>
    <stop_conditions>
      - Compilation failure on record builder generation.
    </stop_conditions>
  </slice>

  <slice id="S4">
    <objective>Migrate test fixtures to builder pattern and verify full suite</objective>
    <change>
      - Update `UserTest.java`, `LoginServiceTest.java`, `AuthControllerTest.java`, `JwtTokenProviderAdapterTest.java`, and `PersistenceAdaptersTest.java` to build fixtures using `.builder()`.
      - Run full test suite and code style formatter.
    </change>
    <allowed_files>
      - `src/test/java/com/platform/app/iam/domain/model/UserTest.java`
      - `src/test/java/com/platform/app/iam/application/services/LoginServiceTest.java`
      - `src/test/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AuthControllerTest.java`
      - `src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/security/JwtTokenProviderAdapterTest.java`
      - `src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/adapter/PersistenceAdaptersTest.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-4: Test fixtures migrated to builder patterns.
      - [ ] AC-5: `./gradlew test` and `./gradlew spotlessCheck` pass with 0 failures.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test && ./gradlew spotlessCheck
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with all tests passing</expected_evidence>
    <rollback_point>git checkout -- src/test/java/</rollback_point>
    <stop_conditions>
      - Failing unit or integration test assertions.
    </stop_conditions>
  </slice>

</slices>

---

## 5. Scope Contract

<scope_contract>
  <allowed>
    - Java source files and test files in `src/main/java` and `src/test/java` specified in allowed_files.
  </allowed>
  <forbidden>
    - Database migrations / Liquibase changelogs.
    - Gradle build scripts (`build.gradle`, `settings.gradle`).
    - Core domain business logic altering authentication or authorization decisions.
  </forbidden>
</scope_contract>

---

## 6. Verification Matrix

<verification_matrix>

| AC / Risk | Test / Command | Expected Evidence | Actual Result |
|---|---|---|---|
| AC-1 | `./gradlew compileJava` | BUILD SUCCESSFUL | Pending S1-S3 |
| AC-2 | `./gradlew compileJava` | BUILD SUCCESSFUL | Pending S2 |
| AC-3 | `./gradlew compileJava` | BUILD SUCCESSFUL | Pending S3 |
| AC-4 | `./gradlew compileTestJava` | BUILD SUCCESSFUL | Pending S4 |
| AC-5 | `./gradlew test && ./gradlew spotlessCheck` | BUILD SUCCESSFUL | Pending S4 |

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
  <approved_by>[AUTO: DELEGATED]</approved_by>
  <approved_date>2026-09-12</approved_date>
</gate>

</execution_plan>
