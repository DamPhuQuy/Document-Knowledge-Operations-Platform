# Task: CHG-IAM-05 Adopt Lombok @Builder Pattern for Object Creation

<task_spec version="3.0" framework="RIPER-5">

<!-- ════════════════════════════════════════════
     SECTION 0 — TASK CONTROL (master state record)
     ════════════════════════════════════════════ -->
<task_control>
  <status>COMPLETED</status>  <!-- BACKLOG | ACTIVE | BLOCKED | PAUSED | REVIEW | COMPLETED | CANCELLED -->
  <spec_level>S2</spec_level>  <!-- S0=sketch | S1=defined | S2=verified | S3=locked -->
  <priority>P2</priority>  <!-- P0=urgent | P1=high | P2=normal | P3=low -->
  <risk>LOW</risk>  <!-- LOW | MEDIUM | HIGH -->
  <estimated_story_points>1</estimated_story_points>  <!-- 1 SP ≈ 2-4 focused engineering hours -->
  <working_mode>DELEGATED</working_mode>  <!-- PAIR (default: halts at each gate for review) | DELEGATED (fast-track: continuous autonomous run) -->
  <current_phase>REVIEW</current_phase>  <!-- RESEARCH | INNOVATE | PLAN | EXECUTE | REVIEW -->
  <owner>@engineer</owner>
  <decision_owner>@engineer</decision_owner>
  <created>2026-09-12</created>
  <last_updated>2026-09-12</last_updated>
</task_control>

---

## 1. Specification (Pillar 1: Task / Spec)

<specification>
  <goal>
    Refactor object instantiation across the IAM subsystem (`src/main/java/com/platform/app/iam`), shared domain models (`src/main/java/com/platform/app/shared`), and associated test suites to use Lombok `@Builder`, eliminating fragile multi-argument `new ClassName(...)` invocations in favor of expressive, maintainable, and type-safe fluent builders.
  </goal>

  <current_behavior>
    - Multiple classes, JPA entities, DTOs, and domain models are constructed via verbose positional `new` constructor calls (e.g., `new RefreshToken(...)`, `new UserJpaEntity(...)`, `new UserProfileDto(...)`, `new ErrorResponse(...)`).
    - Recent enhancements to domain models (`UserFlags`, `AuditMetadata`, `User.java` builder) resulted in mismatching constructor signatures in test fixtures and persistence adapters, breaking `./gradlew test` compilation.
    - Test fixtures in `UserTest`, `LoginServiceTest`, `AuthControllerTest`, `JwtTokenProviderAdapterTest`, and `PersistenceAdaptersTest` use verbose multi-argument `new` calls that are sensitive to signature changes.
  </current_behavior>

  <expected_behavior>
    - Domain models (`User`, `Role`, `Permission`, `RefreshToken`) have clean `@Builder` annotations (preserving unmodifiable collections, defensive copies, and domain encapsulation).
    - JPA entities (`UserJpaEntity`, `RoleJpaEntity`, `PermissionJpaEntity`, `RefreshTokenJpaEntity`) have `@Builder` (with `@Builder.Default` for collections to avoid null references).
    - Multi-property DTOs, events, and value objects (`UserProfileDto`, `UserLoginSuccessEvent`, `UserLoginFailedEvent`, `LoginCommand`, `LoginRequest`, `ErrorResponse`, `JwtProperties`) leverage `@Builder`.
    - Convenience methods on `User` (`isEnabled()`, `isInternal()`, `getCreatedAt()`, `getUpdatedAt()`) are exposed to maintain backward compatibility with callers and tests.
    - Persistence adapters (`RefreshTokenRepositoryAdapter`, `UserRepositoryAdapter`) and test suites consistently utilize fluent `.builder()...build()` patterns.
    - `./gradlew test` and `./gradlew spotlessCheck` pass cleanly.
  </expected_behavior>

  <actor_authorization>
    Internal developer refactoring; no change to external API contracts, database schema, or authorization policies.
  </actor_authorization>

  <invariants>
    - Clean Architecture / DDD boundaries: Domain models must remain clean and not depend on JPA or persistence mechanisms.
    - Encapsulation: Aggregates and entities must not expose mutable internal state via builders or setters; collections must default to non-null safe values (`@Builder.Default`).
    - Test compatibility: Existing functional assertions must remain identical.
    - Public API responses, JSON payloads, and database queries must remain 100% identical in behavior.
  </invariants>

  <out_of_scope>
    - Single-value identifier records (`UserId`, `RoleId`, `DepartmentId`) — single-field records do not benefit from builders.
    - Modifying database schemas, Liquibase scripts, or security authorization logic.
    - Creating new application features or REST endpoints.
  </out_of_scope>

  <acceptance_criteria>
    - [x] AC-1: All candidate domain models, JPA entities, and multi-field DTOs/events in the IAM and shared modules have `@Builder` configured safely.
    - [x] AC-2: Creation of domain models and JPA entities in persistence adapters utilizes fluent `.builder()...build()` syntax instead of positional multi-arg `new`.
    - [x] AC-3: Creation of DTOs, commands, and events in application services and REST controllers utilizes builder syntax where applicable.
    - [x] AC-4: Test suites (`UserTest`, `LoginServiceTest`, `AuthControllerTest`, `JwtTokenProviderAdapterTest`, `PersistenceAdaptersTest`) are migrated to builder patterns, restoring complete build and test health.
    - [x] AC-5: `./gradlew test` and `./gradlew spotlessCheck` execute with 0 failures and 0 regressions.
  </acceptance_criteria>
</specification>

---

## 2. Context Boundaries (Pillar 2: Context)

<context_boundaries>
  <target_files>
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
    - `src/test/java/com/platform/app/iam/domain/model/UserTest.java`
    - `src/test/java/com/platform/app/iam/application/services/LoginServiceTest.java`
    - `src/test/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AuthControllerTest.java`
    - `src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/security/JwtTokenProviderAdapterTest.java`
    - `src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/adapter/PersistenceAdaptersTest.java`
  </target_files>

  <context_groups>
    - [planning | tests | architecture]
  </context_groups>

  <source_of_truth>
    <requirement>User request: refactor codebase from basic `new` creation to `@Builder` (Lombok)</requirement>
    <architecture>Hexagonal Architecture / DDD invariants</architecture>
    <existing_behavior>IAM domain models, entities, and tests in `src/`</existing_behavior>
    <tests>`./gradlew test`</tests>
  </source_of_truth>
</context_boundaries>

---

## 3. Verification Strategy

<verification_strategy>

| AC / Risk | Evidence required | Verifier |
|---|---|---|
| AC-1 | Models/entities/DTOs annotated with `@Builder` without compromising invariants | Source inspection |
| AC-2 | Adapters construct domain & entity objects via `.builder()` | Source inspection |
| AC-3 | Services & controllers construct events/DTOs via `.builder()` | Source inspection |
| AC-4 | Test suites construct fixtures via `.builder()` | Test execution |
| AC-5 | All tests pass, code formatted cleanly | `./gradlew test` && `./gradlew spotlessCheck` |

</verification_strategy>

---

## 4. Decisions

<decisions>
  <approved_decisions>
    <!-- Filled during INNOVATE phase -->
  </approved_decisions>

  <open_decisions>
    <!-- Filled during INNOVATE phase -->
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
      <approved_by>[AUTO: DELEGATED]</approved_by>
      <approved_date>2026-09-12</approved_date>
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
      <approved_by>[AUTO: DELEGATED]</approved_by>
      <approved_date>2026-09-12</approved_date>
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
      <approved_by>[AUTO: DELEGATED]</approved_by>
      <approved_date>2026-09-12</approved_date>
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
    Never suppress errors with flags.
    If exhausted, log into `<open_decisions>` and halt.
  </retry_budget>
</guardrails>

</task_spec>
