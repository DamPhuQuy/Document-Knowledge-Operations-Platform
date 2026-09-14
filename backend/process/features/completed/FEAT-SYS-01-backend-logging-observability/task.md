# Task: [FEAT-SYS-01] Backend Structured Logging & Observability Infrastructure

<task_spec version="3.0" framework="RIPER-5">

<!-- ════════════════════════════════════════════
     SECTION 0 — TASK CONTROL (master state record)
     ════════════════════════════════════════════ -->
<task_control>
  <status>COMPLETED</status>  <!-- BACKLOG | ACTIVE | BLOCKED | PAUSED | REVIEW | COMPLETED | CANCELLED -->
  <spec_level>S2</spec_level>  <!-- S0=sketch | S1=defined | S2=verified | S3=locked -->
  <priority>P1</priority>  <!-- P0=urgent | P1=high | P2=normal | P3=low -->
  <risk>MEDIUM</risk>  <!-- LOW | MEDIUM | HIGH -->
  <estimated_story_points>3</estimated_story_points>  <!-- 1 SP ≈ 2-4 focused engineering hours -->
  <working_mode>DELEGATED</working_mode>  <!-- PAIR (default: halts at each gate for review) | DELEGATED (fast-track: continuous autonomous run) | MANUAL | DIAGNOSE-ONLY -->
  <current_phase>REVIEW</current_phase>  <!-- RESEARCH | INNOVATE | PLAN | EXECUTE | REVIEW -->
  <owner>@engineer</owner>
  <decision_owner>@engineer</decision_owner>
  <created>2026-09-14</created>
  <last_updated>2026-09-14</last_updated>
</task_control>

---

## 1. Specification (Pillar 1: Task / Spec)

<specification>
  <goal>
    Establish a comprehensive, production-grade structured logging and request observability infrastructure across the backend subsystem, complying with Clean Hexagonal Architecture, OWASP logging hygiene (zero credential/PII leakage), and contextual tracing (MDC / correlation ID).
  </goal>

  <current_behavior>
    - The backend codebase across `src/main/java/com/platform/app/` contains almost no runtime logging.
    - Only a single log statement exists in `RestExceptionHandler.java` line 105 for unexpected generic exceptions (`Exception.class`).
    - Incoming HTTP requests, authentication flows, department operations, role assignments, security filters (`JwtAuthenticationFilter`), and repository adapters execute silently with no observability.
    - No correlation ID (Trace ID / Request ID) is tracked or injected into the logging context (MDC), making request tracing across concurrent threads or distributed log aggregators impossible.
    - No structured JSON logging format is configured for staging/production environments.
  </current_behavior>

  <expected_behavior>
    - Every HTTP request passing through the primary REST adapters is assigned a unique `traceId` / `correlationId` via an MDC filter, propagated throughout the execution thread, returned in HTTP response headers (`X-Trace-Id` or `X-Request-Id`), and cleared cleanly upon request completion.
    - Layer-calibrated logging:
      - Primary REST Adapters: Log request entry, response status, execution duration, path, and client IP at appropriate levels (`DEBUG`/`INFO`).
      - Security & Filter Layer: Log token authentication events, token expiration/validation failures at `WARN` without leaking secret claims or bearer tokens.
      - Application Services: Log business use-case milestones, state transitions, domain event publication, and business rejection reasons at `INFO`/`WARN`.
      - Exception Handlers: Log client errors (4xx) at `WARN` (without stack traces) and server errors (5xx) at `ERROR` with sanitized error context and stack traces.
      - Secondary Adapters: Log infrastructure operations, external call latency, or persistence exceptions at `DEBUG`/`ERROR`.
    - Strict adherence to rule `no_credential_leakage`: Passwords, refresh tokens, JWT strings, and sensitive credentials are never written to logs.
  </expected_behavior>

  <actor_authorization>
    System-wide infrastructure concern; applies uniformly to all incoming client traffic (authenticated or anonymous) and internal asynchronous event handlers.
  </actor_authorization>

  <invariants>
    - Zero Credential Leakage: Raw passwords, password hashes, JWT bearer tokens, and refresh tokens must NEVER appear in log files or console output.
    - Hexagonal Dependency Rule: Pure domain models in `iam/domain/` remain decoupled from infrastructure logging frameworks. Logging resides in adapters and application services.
    - Thread-Safety & MDC Cleanup: MDC context MUST be cleared in a `finally` block to prevent thread-pool memory leaks or cross-request context contamination.
    - Backward Compatibility: Existing REST contracts, HTTP status codes, and test suites must continue passing 100% without regression.
  </invariants>

  <out_of_scope>
    - Distributed tracing collectors (e.g., OpenTelemetry collector, Zipkin/Jaeger server setup).
    - Database audit log table persistence (handled separately under UC-AUDIT-01).
    - Modification of database schema or Liquibase migrations.
  </out_of_scope>

  <acceptance_criteria>
    - [ ] AC-1: A primary filter or interceptor attaches a unique `traceId` to SLF4J MDC for each incoming request, exposes it in response headers, and guarantees deterministic MDC clearing in `finally`.
    - [ ] AC-2: Core authentication and authorization workflows (login success/failure, token validation failure, account lockout) log structured contextual messages without leaking passwords or tokens.
    - [ ] AC-3: Business use cases (`DepartmentService`, `AssignRolesService`, `LoginService`) log key operational milestones at `INFO` and validation/conflict rejections at `WARN`.
    - [ ] AC-4: `RestExceptionHandler` logs client errors (4xx) at `WARN` and unexpected system exceptions (5xx) at `ERROR` with `traceId` context.
    - [ ] AC-5: Configuration in `application.yaml` / `application-prod.yaml` supports calibrated log levels and structured layout.
    - [ ] AC-6: All automated unit and integration test suites pass (`./gradlew test`, `./gradlew check`).
  </acceptance_criteria>

  <!-- Definition-of-Ready (DoR) Gate — Frontload the thinking before dispatch -->
  <definition_of_ready>
    - [x] Target outcome and acceptance criteria are verifiable without guessing.
    - [x] Invariants and <out_of_scope> boundaries are explicit.
    - [x] Open questions identified for resolution in INNOVATE phase (decision.md).
  </definition_of_ready>
</specification>

---

## 2. Context Boundaries (Pillar 2: Context)

<context_boundaries>
  <target_files>
    - `src/main/java/com/platform/app/shared/infrastructure/logging/*` — MDC correlation filter and logging utilities
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/*` — REST controllers, exception handlers, security filters
    - `src/main/java/com/platform/app/iam/application/services/*` — IAM application services (`LoginService`, `DepartmentService`, `AssignRolesService`)
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/*` — Token provider and lockout adapters
    - `src/main/resources/application.yaml` — Logging configuration profiles
    - `src/main/resources/application-prod.yaml` — Production logging configuration
    - `src/test/java/com/platform/app/*` — Logging and MDC verification tests
  </target_files>

  <context_groups>
    - `protocols` (`process/development-protocols/implementation-standards.md`)
    - `architecture` (`process/context/architecture/architecture-template.md`)
    - `tests` (`process/context/tests/all-tests.md`)
  </context_groups>

  <source_of_truth>
    <requirement>User request: backend codebase currently lacks logging; initialize RESEARCH phase for big changes</requirement>
    <architecture>[`process/context/architecture/architecture-template.md`](process/context/architecture/architecture-template.md)</architecture>
    <standards>[`process/development-protocols/implementation-standards.md`](process/development-protocols/implementation-standards.md) (Section 5: Observability & Logging Hygiene)</standards>
    <tests>All existing test suites under `src/test/java/com/platform/app/`</tests>
  </source_of_truth>
</context_boundaries>

---

## 3. Verification Strategy

<verification_strategy>

| AC / Risk | Evidence required | Verifier |
|---|---|---|
| AC-1 | HTTP responses contain `X-Trace-Id` header and MDC traceId is active during execution | `./gradlew test --tests "*TraceIdFilterTest*"` or integration test asserting response headers |
| AC-2 | Login success, failure, lockout, and invalid token log entries verified via Logback TestAppender / OutputCapture | `./gradlew test --tests "*AuthControllerTest*"` & `*SecurityTest*` |
| AC-3 | Department and role service operations emit appropriate INFO/WARN logs | `./gradlew test --tests "*DepartmentServiceTest*"` & `*AssignRolesServiceTest*` |
| AC-4 | 4xx and 5xx exception handling logs appropriately without credential leakage | `./gradlew test --tests "*RestExceptionHandlerTest*"` |
| AC-5 | Profile configuration validated | `./gradlew check` |
| AC-6 | Full regression test suite passing | `./gradlew test` and `./gradlew check` |

</verification_strategy>

---

## 4. Decisions

<decisions>
  <approved_decisions>
    <!-- Filled after Gate 1 approval -->
  </approved_decisions>

  <open_decisions>
    <!-- To be evaluated in INNOVATE phase (decision.md) -->
    <!-- | ID | Question | Why it matters | Owner | Blocking? | -->
    | DEC-LOG-01 | Correlation ID mechanism: Servlet Filter vs OncePerRequestFilter vs HandlerInterceptor | Determines where traceId is initialized and whether security filters are covered | @engineer | YES (at G1) |
    | DEC-LOG-02 | Location of shared logging infrastructure: `com.platform.app.shared.infrastructure` vs `com.platform.app.system` | Adheres to module architecture specified in `architecture-template.md` | @engineer | YES (at G1) |
    | DEC-LOG-03 | Request/Response payload logging: Log body vs Headers & Metadata only | Trade-off between observability detail vs memory overhead and PII leak risk | @engineer | YES (at G1) |
  </open_decisions>
</decisions>

---

## 5. RIPER-5 Execution Plan (Pillar 4: Loop)

<execution_plan>
  <phase name="Research" order="1">
    - [x] Ingest task spec, domain invariants, and out-of-scope boundaries.
    - [x] Read existing codebase components, configuration, and testing harness.
    - [x] Establish execution flow, layer boundaries, and credential safety constraints.
    - [x] Produce `research.md` artifact with classified evidence.
    <gate id="G0" label="Research Complete">
      - [x] Current behavior understood and documented.
      - [x] Execution flow traced.
      - [x] No unresolved research blocker.
    </gate>
  </phase>

  <phase name="Innovate" order="2">
    - [ ] Generate 2–3 alternative approaches with trade-off matrix in `decision.md`.
    - [ ] Evaluate Correlation ID placement, payload logging boundaries, and module location.
    <gate id="G1" label="Gate 1 — Decision Approved">
      - [ ] Options reviewed and trade-offs analyzed.
      - [ ] Selected option recorded in `decision.md`.
      - [ ] Approved by engineer.
      <approved_by></approved_by>
      <approved_date></approved_date>
    </gate>
  </phase>

  <phase name="Plan" order="3">
    - [ ] Decompose into vertical slices with verifiers and rollback points.
    - [ ] Populate `plan.md` with scope contract and verification matrix.
    <gate id="G2" label="Gate 2 — Plan Approved">
      - [ ] Slices, verifiers, and allowed/forbidden scopes specified.
      - [ ] Approved by engineer.
      <approved_by></approved_by>
      <approved_date></approved_date>
    </gate>
  </phase>

  <phase name="Execute" order="4">
    - [ ] Implement each slice atomically.
    - [ ] Run verifier after each slice.
    - [ ] Update `state.md` after each slice.
  </phase>

  <phase name="Review" order="5">
    - [ ] Audit full diff against clean architecture, security, and regression gates.
    - [ ] Produce `review.md`.
    <gate id="G3" label="Gate 3 — Review Passed">
      - [ ] All AC verified with evidence.
      - [ ] Approved by engineer.
      <approved_by></approved_by>
      <approved_date></approved_date>
    </gate>
  </phase>
</execution_plan>

---

## 6. Guardrails & Escalation (Pillar 3 & 4: Harness)

<guardrails>
  <stop_conditions>
    - Attempt to log plaintext passwords, token strings, or sensitive PII.
    - Breaking existing API responses, status codes, or database schema.
    - Unconstrained dependencies outside standard Spring Boot / SLF4J / Logback capabilities.
    - Retry budget exhausted (3 attempts).
  </stop_conditions>

  <retry_budget max_attempts="3">
    Maximum 3 consecutive attempts per distinct failure symptom before halting.
  </retry_budget>
</guardrails>

</task_spec>
