# Task: CHG-IAM-03 Refactor JWT Configuration to ConfigurationProperties

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
    Migrate the JWT security configuration from disparate `@Value` annotations in `JwtTokenProviderAdapter` to a dedicated, type-safe, immutable `@ConfigurationProperties` record (`JwtProperties`) with Jakarta validation.
  </goal>

  <current_behavior>
    `JwtTokenProviderAdapter` injects `@Value("${app.jwt.secret}")` and `@Value("${app.jwt.expiration-ms}")` via its constructor. `refresh-expiration-ms` is present in `application.yaml` but not accessible in a unified property holder.
  </current_behavior>

  <expected_behavior>
    - `JwtProperties` record declared under `com.platform.app.iam.infrastructure.adapters.secondary.security.config` with `@ConfigurationProperties(prefix = "app.jwt")` and `@Validated`.
    - `AppApplication` annotated with `@ConfigurationPropertiesScan`.
    - `JwtTokenProviderAdapter` injects `JwtProperties` directly, removing `@Value` annotations.
    - `JwtTokenProviderAdapterTest` and full test suite updated and passing 100%.
  </expected_behavior>

  <actor_authorization>
    Internal developer refactoring; no change to external API contracts, token format, or security requirements.
  </actor_authorization>

  <invariants>
    - JWT signing algorithm and claims generation must remain 100% identical.
    - 256-bit secret key validation remains strictly enforced.
    - Zero breaking changes to authentication endpoints.
  </invariants>

  <out_of_scope>
    - Altering JWT token claims or token signing mechanisms.
    - Changing CORS or HTTP security endpoints.
  </out_of_scope>

  <acceptance_criteria>
    - [x] AC-1: `JwtProperties` record created with `@ConfigurationProperties(prefix = "app.jwt")` and bean validation.
    - [x] AC-2: `@ConfigurationPropertiesScan` added to `AppApplication`.
    - [x] AC-3: `JwtTokenProviderAdapter` refactored to inject `JwtProperties` without `@Value`.
    - [x] AC-4: `JwtTokenProviderAdapterTest` updated and passing green.
    - [x] AC-5: Full test suite (`./gradlew test`) passes with zero regression.
  </acceptance_criteria>
</specification>

---

## 2. Context Boundaries (Pillar 2: Context)

<context_boundaries>
  <target_files>
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/config/JwtProperties.java`
    - `src/main/java/com/platform/app/AppApplication.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/adapter/JwtTokenProviderAdapter.java`
    - `src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/security/JwtTokenProviderAdapterTest.java`
  </target_files>

  <source_of_truth>
    <requirement>User request to use ConfigurationProperties for JwtTokenProviderAdapter</requirement>
    <existing_behavior>[`src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/adapter/JwtTokenProviderAdapter.java`](src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/adapter/JwtTokenProviderAdapter.java)</existing_behavior>
    <tests>[`src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/security/JwtTokenProviderAdapterTest.java`](src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/security/JwtTokenProviderAdapterTest.java)</tests>
  </source_of_truth>
</context_boundaries>

---

## 3. Verification Strategy

<verification_strategy>

| AC / Risk | Evidence required | Verifier |
|---|---|---|
| AC-1 | `JwtProperties` compiles with `@ConfigurationProperties` | `./gradlew compileJava` |
| AC-2 | `AppApplication` compiles with `@ConfigurationPropertiesScan` | `./gradlew compileJava` |
| AC-3 | `JwtTokenProviderAdapter` compiles with `JwtProperties` | `./gradlew compileJava` |
| AC-4, AC-5 | All tests pass | `./gradlew test` |

</verification_strategy>

---

## 4. Decisions

<decisions>
  <approved_decisions>
    - DEC-IAM-03: Use Java Record for `JwtProperties` with `@Validated` and `@ConfigurationProperties(prefix = "app.jwt")` for immutability and fail-fast validation. Approved by user ("proceed").
  </approved_decisions>
</decisions>

---

## 5. RIPER-5 Execution Plan (Pillar 4: Loop)

<execution_plan>
  <phase name="Research" order="1">
    - [x] Analyze `JwtTokenProviderAdapter` and `application.yaml` property bindings.
    <gate id="G0" label="Research Complete">
      - [x] Baseline documented.
    </gate>
  </phase>

  <phase name="Innovate" order="2">
    - [x] Propose Java Record `@ConfigurationProperties` architecture.
    <gate id="G1" label="Gate 1 — Decision Approved">
      - [x] Approved by engineer via prompt "proceed".
      <approved_by>@engineer</approved_by>
      <approved_date>2026-09-11</approved_date>
    </gate>
  </phase>

  <phase name="Plan" order="3">
    - [x] Slices defined: S1 (JwtProperties & Scan), S2 (Adapter refactoring), S3 (Tests & Verification).
    <gate id="G2" label="Gate 2 — Plan Approved">
      - [x] Plan approved by engineer.
      <approved_by>@engineer</approved_by>
      <approved_date>2026-09-11</approved_date>
    </gate>
  </phase>

  <phase name="Execute" order="4">
    - [x] Implement each slice atomically.
    - [x] Run verifiers after each slice.
    - [x] Update `state.md`.
  </phase>

  <phase name="Review" order="5">
    - [x] Review diff and test verification.
    <gate id="G3" label="Gate 3 — Review Passed">
      - [x] All AC verified.
      <approved_by>@engineer</approved_by>
      <approved_date>2026-09-11</approved_date>
    </gate>
  </phase>
</execution_plan>

</task_spec>
