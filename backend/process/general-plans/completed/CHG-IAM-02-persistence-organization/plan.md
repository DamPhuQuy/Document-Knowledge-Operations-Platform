# Plan: PLAN-IAM-02 Reorganize Persistence Secondary Adapter Structure

<execution_plan task_id="CHG-IAM-02" plan_id="PLAN-IAM-02" version="2.0" framework="RIPER-5">

<!-- PLAN PHASE. Plan artifacts only. No source-code changes. -->
<plan_status>
  <phase>PLAN</phase>
  <last_updated>2026-09-11</last_updated>
</plan_status>

---

## 1. Input Artifacts

<input_artifacts>
  <task_spec>[`process/general-plans/active/CHG-IAM-02-persistence-organization/task.md`](task.md)</task_spec>
  <research>[`process/general-plans/active/CHG-IAM-02-persistence-organization/research.md`](research.md)</research>
  <decision>[`process/general-plans/active/CHG-IAM-02-persistence-organization/decision.md`](decision.md) — DEC-IAM-02, Option A (Categorical Subpackages)</decision>
</input_artifacts>

---

## 2. Execution Constraints

<execution_constraints>
  <allowed_files>
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/**`
    - `src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/**`
    - `src/test/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AuthControllerTest.java`
    - `process/context/architecture/architecture-template.md`
    - `process/context/all-context.md`
    - `process/general-plans/active/CHG-IAM-02-persistence-organization/**`
  </allowed_files>
  <forbidden_files>
    - `src/main/java/com/platform/app/iam/domain/**`
    - `src/main/java/com/platform/app/iam/application/**`
    - `src/main/resources/db/**`
  </forbidden_files>
  <allowed_commands>
    - `./gradlew compileJava`
    - `./gradlew test`
    - `./gradlew check`
    - `git diff`
  </allowed_commands>
  <restricted_operations>
    - No DB schema/migration changes.
    - No changes to domain models or application port interfaces.
  </restricted_operations>
  <required_approvals>
    - Gate 2 sign-off before entering EXECUTE.
  </required_approvals>
</execution_constraints>

---

## 3. Slice Summary

<slice_summary>

| ID | Behavior | Boundary | Files | AC | Verifier | Risk | Mode | Rollback |
|---|---|---|---|---|---|---|---|---|
| S1 | Relocate JPA entities | `persistence/entity/` | 4 entities | AC-1 | `./gradlew compileJava` | LOW | MODIFY | Git restore |
| S2 | Relocate Spring Data repos | `persistence/repository/` | 2 repos | AC-2 | `./gradlew compileJava` | LOW | MODIFY | Git restore |
| S3 | Relocate outbound adapters | `persistence/adapter/` | 2 adapters | AC-3 | `./gradlew compileJava` | LOW | MODIFY | Git restore |
| S4 | Update test suites | test classes | 2 test files | AC-5 | `./gradlew test` | LOW | MODIFY | Git restore |
| S5 | Update architecture template & router | docs | 2 markdown files | AC-4 | Doc inspection / grep | LOW | MODIFY | Git restore |

</slice_summary>

---

## 4. Slice Details

<slices>

  <slice id="S1">
    <objective>Move JPA entities to `persistence.entity` package and remove old root entity files</objective>
    <change>Create `persistence/entity/{UserJpaEntity,RoleJpaEntity,PermissionJpaEntity,RefreshTokenJpaEntity}.java` with updated package and internal entity references. Remove old files.</change>
    <allowed_files>
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/entity/**`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/*.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-1: JPA entities compile under `persistence.entity` package.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew compileJava
      ```
    </verifier>
    <expected_evidence>Build succeeds or cleanly identifies remaining repo/adapter import transitions</expected_evidence>
    <rollback_point>Restore `src/main/java/.../secondary/persistence/` via git checkout</rollback_point>
    <stop_conditions>
      - JPA mapping annotation compilation errors not resolvable via imports.
    </stop_conditions>
  </slice>

  <slice id="S2">
    <objective>Move Spring Data JPA repositories to `persistence.repository` package and remove old files</objective>
    <change>Create `persistence/repository/{SpringDataUserRepository,SpringDataRefreshTokenRepository}.java` with updated package and imports from `persistence.entity.*`. Remove old files.</change>
    <allowed_files>
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/repository/**`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/*.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-2: Spring Data repos compile under `persistence.repository` package.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew compileJava
      ```
    </verifier>
    <expected_evidence>Repositories compile cleanly referencing new entity locations</expected_evidence>
    <rollback_point>Restore via git checkout</rollback_point>
    <stop_conditions>
      - Query derivation or Spring Data interface resolution failures.
    </stop_conditions>
  </slice>

  <slice id="S3">
    <objective>Move Outbound Port Adapters to `persistence.adapter` package and remove old files</objective>
    <change>Create `persistence/adapter/{UserRepositoryAdapter,RefreshTokenRepositoryAdapter}.java` with updated package and imports from `persistence.entity.*` and `persistence.repository.*`. Remove old files.</change>
    <allowed_files>
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/adapter/**`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/*.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-3: Repository adapters compile under `persistence.adapter` package implementing ports and injecting repos.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew compileJava
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with 0 errors across all main source files</expected_evidence>
    <rollback_point>Restore via git checkout</rollback_point>
    <stop_conditions>
      - Any broken outbound port contract or unmapped domain fields.
    </stop_conditions>
  </slice>

  <slice id="S4">
    <objective>Update unit and integration test suites to use the reorganized packages</objective>
    <change>Update imports in `AuthControllerTest.java` and `PersistenceAdaptersTest.java`. Relocate `PersistenceAdaptersTest.java` to `persistence.adapter` or update its imports.</change>
    <allowed_files>
      - `src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/**`
      - `src/test/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AuthControllerTest.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-5: All tests pass green.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test
      ```
    </verifier>
    <expected_evidence>All unit and integration tests PASS with 0 failures</expected_evidence>
    <rollback_point>Restore test directory via git checkout</rollback_point>
    <stop_conditions>
      - Failing test assertions or Spring context configuration errors.
    </stop_conditions>
  </slice>

  <slice id="S5">
    <objective>Standardize persistence secondary adapter layout in architecture template context</objective>
    <change>Update `process/context/architecture/architecture-template.md` to reflect `secondary/persistence/{entity,repository,adapter}` and document rules. Fix router entry in `process/context/all-context.md`.</change>
    <allowed_files>
      - `process/context/architecture/architecture-template.md`
      - `process/context/all-context.md`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-4: Architecture documentation contains explicit persistence subpackage rules and correct path links.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew check
      ```
    </verifier>
    <expected_evidence>Documentation synchronized and formatting intact</expected_evidence>
    <rollback_point>Revert markdown changes</rollback_point>
    <stop_conditions>
      - Broken links or conflicting rules.
    </stop_conditions>
  </slice>

</slices>

---

## 5. Scope Contract

<scope_contract>
  <allowed>
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/**`
    - `src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/**`
    - `src/test/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AuthControllerTest.java`
    - `process/context/architecture/architecture-template.md`
    - `process/context/all-context.md`
  </allowed>
  <forbidden>
    - Domain models (`domain/**`)
    - Application use cases or port contracts (`application/**`)
    - Database migrations (`db/changelog/**`)
    - Build script dependencies (`build.gradle`)
  </forbidden>
</scope_contract>

---

## 6. Verification Matrix

<verification_matrix>

| AC / Risk | Test / Command | Expected Evidence | Actual Result |
|---|---|---|---|
| AC-1 | `./gradlew compileJava` | Entities compiled under `persistence.entity` | PENDING |
| AC-2 | `./gradlew compileJava` | Repositories compiled under `persistence.repository` | PENDING |
| AC-3 | `./gradlew compileJava` | Adapters compiled under `persistence.adapter` | PENDING |
| AC-4 | Markdown inspection | Architecture template updated with persistence standards | PENDING |
| AC-5 | `./gradlew test` | All test suites pass 100% | PENDING |

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
