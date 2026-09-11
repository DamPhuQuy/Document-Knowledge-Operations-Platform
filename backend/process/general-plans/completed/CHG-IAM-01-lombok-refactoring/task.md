# Task: CHG-IAM-01 Refactor IAM Module to Use Lombok

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
    Refactor the IAM subsystem (`src/main/java/com/platform/app/iam`) to utilize Project Lombok annotations for boilerplate reduction (constructors, accessors, equals/hashCode, logging) while strictly adhering to Clean Architecture / DDD invariants, JPA entity best practices (avoiding lazy-load/recursion issues), and preserving 100% test compatibility.
  </goal>

  <current_behavior>
    - JPA entities (`UserJpaEntity`, `RoleJpaEntity`, `PermissionJpaEntity`, `RefreshTokenJpaEntity`) manually implement verbose getters, setters, no-arg constructors, all-arg constructors, and equals/hashCode methods (~500 lines of repetitive boilerplate).
    - Spring beans and services (`LoginService`, `AuthController`, `UserRepositoryAdapter`, `RefreshTokenRepositoryAdapter`, `SpringEventPublisherAdapter`) use manual constructor injection boilerplate.
    - `RestExceptionHandler` manually instantiates `LoggerFactory.getLogger(...)`.
    - Domain models (`User`, `Role`, `RefreshToken`, `Permission`) manually write getters and equals/hashCode implementations.
  </current_behavior>

  <expected_behavior>
    - JPA entities use Lombok `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, and `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` on ID fields. No hazardous `@Data` or `@ToString` on lazy relationship collections.
    - Spring components and services use `@RequiredArgsConstructor` for clean constructor dependency injection.
    - REST exception handler uses `@Slf4j` for logging.
    - Domain models leverage Lombok where safe without compromising encapsulation, collection unmodifiability, or domain invariants.
    - Code passes `./gradlew test` and `./gradlew spotlessCheck` with zero regressions.
  </expected_behavior>

  <actor_authorization>
    Internal developer refactoring; no change to external API contracts, database schema, or HTTP authorization behavior.
  </actor_authorization>

  <invariants>
    - Clean Architecture / DDD boundaries must remain intact (domain does not leak JPA annotations or infrastructure concerns).
    - JPA entity equals/hashCode must strictly use `id` (or primary key) and avoid traversing lazy associations (`roles`, `permissions`).
    - Java records (`UserId`, `RoleId`, `DepartmentId`, `LoginRequest`, `AuthResponse`, `ErrorResponse`, `UserProfileDto`, `AuthTokensDto`, events) already provide compact immutable representations and do not require Lombok.
    - Public API responses and database queries must remain 100% identical in behavior.
  </invariants>

  <out_of_scope>
    - Changing API endpoints, JSON payloads, or HTTP status codes.
    - Modifying Liquibase database migrations or SQL tables.
    - Altering Spring Security configurations or JWT token structures.
    - Forcing Lombok onto modern Java records where Lombok is redundant.
  </out_of_scope>

  <acceptance_criteria>
    - [x] AC-1: JPA entities in `iam/infrastructure/adapters/secondary/persistence/` (`UserJpaEntity`, `RoleJpaEntity`, `PermissionJpaEntity`, `RefreshTokenJpaEntity`) refactored with safe Lombok annotations (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`).
    - [x] AC-2: Spring components/services/controllers/adapters in `iam` refactored using Lombok `@RequiredArgsConstructor` for constructor dependency injection.
    - [x] AC-3: `RestExceptionHandler` refactored using Lombok `@Slf4j`.
    - [x] AC-4: Domain model entities (`User`, `Role`, `RefreshToken`, `Permission`) refactored using selective Lombok annotations (`@Getter`, `@EqualsAndHashCode`) while preserving unmodifiable collections and validation invariants.
    - [x] AC-5: Spotless check (`./gradlew spotlessCheck`) and test suite (`./gradlew test`) pass with 0 regressions.
  </acceptance_criteria>
</specification>

---

## 2. Context Boundaries (Pillar 2: Context)

<context_boundaries>
  <target_files>
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/UserJpaEntity.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/RoleJpaEntity.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/PermissionJpaEntity.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/RefreshTokenJpaEntity.java`
    - `src/main/java/com/platform/app/iam/application/services/LoginService.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AuthController.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/UserRepositoryAdapter.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/RefreshTokenRepositoryAdapter.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/messaging/SpringEventPublisherAdapter.java`
    - `src/main/java/com/platform/app/iam/domain/model/User.java`
    - `src/main/java/com/platform/app/iam/domain/model/Role.java`
    - `src/main/java/com/platform/app/iam/domain/model/RefreshToken.java`
    - `src/main/java/com/platform/app/iam/domain/model/Permission.java`
  </target_files>

  <context_groups>
    - `process/context/all-context.md`
    - `process/development-protocols/implementation-standards.md`
  </context_groups>

  <source_of_truth>
    <requirement>User request: `/small-change '/home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam' sử dụng lombok`</requirement>
    <architecture>Clean Architecture / DDD (AGENTS.md, implementation-standards.md)</architecture>
    <existing_behavior>Existing test suite in `src/test/java/com/platform/app/iam/`</existing_behavior>
    <tests>`./gradlew test`</tests>
  </source_of_truth>
</context_boundaries>

---

## 3. Verification Strategy

<verification_strategy>

| AC / Risk | Evidence required | Verifier |
|---|---|---|
| AC-1 | Clean JPA entities, zero manual boilerplate getters/setters, tests pass | `./gradlew test --tests "*PersistenceAdaptersTest*"` |
| AC-2 | Clean constructor DI via `@RequiredArgsConstructor`, tests pass | `./gradlew test --tests "*LoginServiceTest*" && ./gradlew test --tests "*AuthControllerTest*"` |
| AC-3 | `@Slf4j` functional in `RestExceptionHandler`, tests pass | `./gradlew test --tests "*AuthControllerTest*"` |
| AC-4 | Domain entities clean, invariants and unmodifiable collections intact | `./gradlew test --tests "*UserTest*" && ./gradlew test --tests "*RefreshTokenTest*"` |
| AC-5 | All tests pass and formatting conforms | `./gradlew check` |

</verification_strategy>

---

## 4. Decisions

<decisions>
  <approved_decisions>
    <!-- Decisions that are final and must not be re-opened by the agent -->
  </approved_decisions>

  <open_decisions>
    <!-- Decisions that still need a human to choose; agent must HALT on these -->
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
    <!-- READ-ONLY. Present 2-3 viable options. PAIR: engineer selects. DELEGATED: agent auto-selects optimal option. -->
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
    <!-- Plan artifacts only. No source-code changes. -->
    - [x] Decompose into vertical slices with verifiers and rollback points.
    - [x] Populate `plan.md` with scope contract and verification matrix.
    <gate id="G2" label="Gate 2 — Plan Approved">
      - [x] Every slice has a verifier.
      - [x] Allowed/forbidden file scope is defined.
      - [x] Rollback point defined per slice.
      - [x] Stop conditions defined.
      - [x] Plan approved.
      <approved_by>@engineer</approved_by>  <!-- Engineer name (PAIR) or [AUTO: DELEGATED] (DELEGATED/Fast-Track) -->
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
    <!-- READ-ONLY. May run verification commands. No code fixes during review. -->
    - [x] Review full diff, behavior, architecture, data, security, regression.
    - [x] Produce `review.md` with findings and verification matrix.
    <gate id="G3" label="Gate 3 — Review Passed">
      - [x] All AC verified with evidence.
      - [x] Residual risk accepted.
      - [x] Review decision: PASS.
      - [x] Ready for handoff.
      <approved_by>@engineer</approved_by>  <!-- Engineer name (PAIR) or [AUTO: DELEGATED] (DELEGATED/Fast-Track) -->
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
    - Security policy change required.
    - Scope expansion beyond `<out_of_scope>`.
    - Retry budget exhausted on a recurring failure.
  </stop_conditions>

  <retry_budget max_attempts="3">
    Maximum 3 consecutive attempts per distinct failure symptom before halting.
    Never suppress errors with flags (e.g. `# type: ignore`, `eslint-disable`).
    If exhausted, log into `<open_decisions>` and halt.
  </retry_budget>
</guardrails>

</task_spec>
