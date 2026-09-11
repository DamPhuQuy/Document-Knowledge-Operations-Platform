# Task: CHG-IAM-02 Reorganize Persistence Secondary Adapter Structure

<task_spec version="3.0" framework="RIPER-5">

<!-- ════════════════════════════════════════════
     SECTION 0 — TASK CONTROL (master state record)
     ════════════════════════════════════════════ -->
<task_control>
  <status>COMPLETED</status>  <!-- BACKLOG | ACTIVE | BLOCKED | PAUSED | REVIEW | COMPLETED | CANCELLED -->
  <spec_level>S2</spec_level>  <!-- S0=sketch | S1=defined | S2=verified | S3=locked -->
  <priority>P2</priority>  <!-- P0=urgent | P1=high | P2=normal | P3=low -->
  <risk>LOW</risk>  <!-- LOW | MEDIUM | HIGH -->
  <estimated_story_points>1</estimated_story_points>
  <working_mode>PAIR</working_mode>  <!-- PAIR (default: halts at each gate for review) | DELEGATED (fast-track: continuous autonomous run) -->
  <current_phase>REVIEW</current_phase>  <!-- RESEARCH | INNOVATE | PLAN | EXECUTE | REVIEW -->
  <owner>@engineer</owner>
  <decision_owner>@engineer</decision_owner>
  <created>2026-09-11</created>
  <last_updated>2026-09-11</last_updated>
</task_control>

---

## 1. Specification (Pillar 1: Task / Spec)

<specification>
  <goal>
    Clean up and reorganize the persistence secondary adapter layer in the IAM subsystem (`src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence`) by modularizing JPA entities, Spring Data repository interfaces, and outbound port repository adapters into clean, dedicated subpackages. Concurrently update `process/context/architecture/architecture-template.md` and the context router `process/context/all-context.md` to establish and document this standard structure for all DDD modules.
  </goal>

  <current_behavior>
    - All persistence files reside flatly in `com.platform.app.iam.infrastructure.adapters.secondary.persistence`:
      - 4 JPA entities (`UserJpaEntity`, `RoleJpaEntity`, `PermissionJpaEntity`, `RefreshTokenJpaEntity`)
      - 2 Spring Data JPA repository interfaces (`SpringDataUserRepository`, `SpringDataRefreshTokenRepository`)
      - 2 Outbound repository adapters (`UserRepositoryAdapter`, `RefreshTokenRepositoryAdapter`)
    - `process/context/architecture/architecture-template.md` displays a flat `persistence/` directory without specifying how internal persistence concerns are structured.
    - `process/context/all-context.md` contains a typo in the architecture entry (`arhitecture` / `/contenxt/...`).
  </current_behavior>

  <expected_behavior>
    - Persistence components in IAM are segregated into cleanly separated subpackages (e.g. `entity/`, `repository/`, `adapter/`), eliminating clutter and clarifying architectural roles.
    - All imports, package declarations, and test references (`PersistenceAdaptersTest`, `AuthControllerTest`) are cleanly updated without regression.
    - `architecture-template.md` is updated with clear directory tree definitions, responsibility guidelines, and dependency flows for `secondary/persistence`.
    - `all-context.md` context index is corrected and synchronized.
    - `./gradlew test` and `./gradlew check` pass with zero failures.
  </expected_behavior>

  <actor_authorization>
    Internal developer refactoring; no change to external REST contracts, HTTP responses, database schema, or domain invariants.
  </actor_authorization>

  <invariants>
    - Clean Architecture / Hexagonal / DDD rules remain intact: domain models never reference JPA entities or Spring Data repositories.
    - Repository adapters continue implementing outbound domain/application ports (`UserRepositoryPort`, `RefreshTokenRepositoryPort`).
    - Spring Data repository queries and JPA mappings remain functionally identical.
    - Zero breaking changes to existing test suites and application behavior.
  </invariants>

  <out_of_scope>
    - Altering domain entity models or domain logic.
    - Modifying database tables, columns, or Liquibase changelog scripts.
    - Altering application service use cases or primary REST controllers.
  </out_of_scope>

  <acceptance_criteria>
    - [x] AC-1: JPA entities relocated to dedicated package (`persistence.entity`) with updated package statements and imports.
    - [x] AC-2: Spring Data JPA repository interfaces relocated to dedicated package (`persistence.repository`) with updated package statements and imports.
    - [x] AC-3: Outbound repository adapters relocated to dedicated package (`persistence.adapter`) implementing outbound ports and cleanly injecting Spring Data repositories.
    - [x] AC-4: Architecture template in `process/context/architecture/architecture-template.md` and router in `process/context/all-context.md` updated with the new persistence architectural standards.
    - [x] AC-5: All tests (`PersistenceAdaptersTest`, `AuthControllerTest`, and full suite) updated and passing green (`./gradlew test`).
  </acceptance_criteria>
</specification>

---

## 2. Context Boundaries (Pillar 2: Context)

<context_boundaries>
  <target_files>
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/**` — [Reorganize into subpackages]
    - `src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/**` — [Update package/imports for tests]
    - `src/test/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AuthControllerTest.java` — [Update imports]
    - `process/context/architecture/architecture-template.md` — [Document persistence architecture template]
    - `process/context/all-context.md` — [Fix router entry]
  </target_files>

  <context_groups>
    - `arhitecture` -> `process/context/architecture/architecture-template.md`
    - `tests` -> `process/context/tests/all-tests.md`
  </context_groups>

  <source_of_truth>
    <requirement>User prompt to review messy persistence organization, make it cleaner, and update architecture template context</requirement>
    <architecture>[`process/context/architecture/architecture-template.md`](process/context/architecture/architecture-template.md)</architecture>
    <existing_behavior>[`src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/PersistenceAdaptersTest.java`](src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/PersistenceAdaptersTest.java)</existing_behavior>
    <tests>[`src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/PersistenceAdaptersTest.java`](src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/PersistenceAdaptersTest.java)</tests>
  </source_of_truth>
</context_boundaries>

---

## 3. Verification Strategy

<verification_strategy>

| AC / Risk | Evidence required | Verifier |
|---|---|---|
| AC-1 | JPA entities compiled under `persistence.entity` package | `./gradlew compileJava` |
| AC-2 | Spring Data repos compiled under `persistence.repository` package | `./gradlew compileJava` |
| AC-3 | Repository adapters compiled under `persistence.adapter` package | `./gradlew compileJava` |
| AC-4 | `architecture-template.md` and `all-context.md` verified and clean | File inspection / lint |
| AC-5 | All persistence unit and integration tests passing | `./gradlew test` |

</verification_strategy>

---

## 4. Decisions

<decisions>
  <approved_decisions>
  </approved_decisions>

  <open_decisions>
    | ID | Question | Why it matters | Owner | Blocking? |
    |---|---|---|---|---|
    | DEC-IAM-02 | Which subpackaging structure should be standardized for secondary persistence? | Governs package layout for IAM and future modules in architecture template | @engineer | YES (Gate 1) |
  </open_decisions>
</decisions>

---

## 5. RIPER-5 Execution Plan (Pillar 4: Loop)

<execution_plan>
  <phase name="Research" order="1">
    - [x] Ingest task spec, domain invariants, and out-of-scope boundaries.
    - [x] Read corresponding tests and port interfaces.
    - [x] Establish execution flow, boundaries, and source-of-truth conflicts.
    - [x] Produce/update `research.md` artifact.
    <gate id="G0" label="Research Complete">
      - [x] Current behavior understood and documented.
      - [x] Execution flow traced.
      - [x] No unresolved research blocker.
    </gate>
  </phase>

  <phase name="Innovate" order="2">
    - [x] Generate 2–3 alternative approaches with trade-off matrix.
    - [x] Produce `decision.md` artifact.
    <gate id="G1" label="Gate 1 — Decision Approved">
      - [x] Options reviewed and trade-offs analyzed.
      - [x] Selected option recorded in `decision.md`.
      - [x] No blocking business/schema/security decision remains open.
      <approved_by>@engineer</approved_by>
      <approved_date>2026-09-11</approved_date>
    </gate>
  </phase>

  <phase name="Plan" order="3">
    - [x] Decompose into vertical slices with verifiers and rollback points.
    - [x] Populate `plan.md` with scope contract and verification matrix.
    <gate id="G2" label="Gate 2 — Plan Approved">
      - [x] Every slice has a verifier.
      - [x] Allowed/forbidden file scope is defined.
      - [x] Rollback point defined per slice.
      - [x] Stop conditions defined.
      - [x] Plan approved.
      <approved_by>@engineer</approved_by>
      <approved_date>2026-09-11</approved_date>
    </gate>
  </phase>

  <phase name="Execute" order="4">
    - [x] Implement each slice atomically.
    - [x] Run verifier after each slice.
    - [x] Inspect diff after each slice.
    - [x] Update `state.md` after each slice.
  </phase>

  <phase name="Review" order="5">
    - [x] Review full diff, behavior, architecture, data, security, regression.
    - [x] Produce `review.md` with findings and verification matrix.
    <gate id="G3" label="Gate 3 — Review Passed">
      - [x] All AC verified with evidence.
      - [x] Residual risk accepted.
      - [x] Review decision: PASS.
      - [x] Ready for handoff.
      <approved_by>@engineer</approved_by>
      <approved_date>2026-09-11</approved_date>
    </gate>
  </phase>
</execution_plan>

---

## 6. Guardrails & Escalation (Pillar 3 & 4: Harness)

<guardrails>
  <stop_conditions>
    - Missing business or policy decision.
    - Public API / DB schema change not declared in this spec.
    - New external dependency not declared in this spec.
    - Scope expansion beyond `<out_of_scope>`.
    - Retry budget exhausted on a recurring failure.
  </stop_conditions>

  <retry_budget max_attempts="3">
    Maximum 3 consecutive attempts per distinct failure symptom before halting.
  </retry_budget>
</guardrails>

</task_spec>
