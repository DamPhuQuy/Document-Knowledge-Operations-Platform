# Plan: PLAN-LOG-01 Backend Structured Logging & Observability Infrastructure

<execution_plan task_id="FEAT-SYS-01" plan_id="PLAN-LOG-01" version="2.0" framework="RIPER-5">

<!-- PLAN PHASE. Plan artifacts only. No source-code changes. -->
<plan_status>
  <phase>PLAN</phase>
  <last_updated>2026-09-14</last_updated>
</plan_status>

---

## 1. Input Artifacts

<input_artifacts>
  <task_spec>[task.md](task.md)</task_spec>
  <research>[research.md](research.md)</research>
  <decision>[decision.md](decision.md) — DEC-LOG-01, Option B (Highest-Precedence Servlet Filter + SLF4J MDC + Layer-Calibrated Logging)</decision>
</input_artifacts>

---

## 2. Execution Constraints

<execution_constraints>
  <allowed_files>
    - `src/main/java/com/platform/app/shared/infrastructure/logging/*` — MDC tracing filter and constants
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/*` — REST controllers and exception handler
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/*` — Security filter and security adapters
    - `src/main/java/com/platform/app/iam/application/services/*` — IAM Application Services
    - `src/main/resources/application.yaml` — Logging pattern and levels
    - `src/main/resources/application-prod.yaml` — Production logging configuration
    - `src/main/resources/application-staging.yaml` — Staging logging configuration
    - `src/test/java/com/platform/app/shared/infrastructure/logging/*` — Tracing filter tests
    - `src/test/java/com/platform/app/iam/*` — Test suites updated if log assertions or dependencies change
  </allowed_files>
  <forbidden_files>
    - `src/main/java/com/platform/app/iam/domain/model/*` — Pure domain models must remain free of logging dependencies
    - `src/main/resources/db/changelog/*` — Zero DB schema changes
    - `build.gradle` — No new external dependencies required
  </forbidden_files>
  <allowed_commands>
    - `./gradlew compileJava`
    - `./gradlew test`
    - `./gradlew check`
    - `./gradlew test --tests <pattern>`
    - `git status`
    - `git diff`
  </allowed_commands>
  <restricted_operations>
    - No DB migrations or schema adjustments.
    - No direct logging from domain models.
    - Zero plain-text credentials or bearer token strings written to log messages.
  </restricted_operations>
  <required_approvals>
    - [AUTO: DELEGATED] fast-track mode authorized by user.
  </required_approvals>
</execution_constraints>

---

## 3. Slice Summary

<slice_summary>

| ID | Behavior | Boundary | Files | AC | Verifier | Risk | Mode | Rollback |
|---|---|---|---|---|---|---|---|---|
| S1 | Shared Tracing & MDC Infrastructure | Shared Infrastructure | `TraceIdFilter.java`, `LoggingConstants.java`, `application*.yaml`, `TraceIdFilterTest.java` | AC-1, AC-5 | `./gradlew test --tests "*TraceIdFilterTest*"` | LOW | ATOMIC | `git checkout --` |
| S2 | Security & Filter Layer Telemetry | IAM Secondary Security | `JwtAuthenticationFilter.java`, `JwtTokenProviderAdapter.java`, `InMemoryAccountLockoutAdapter.java` | AC-2 | `./gradlew test --tests "*AuthControllerTest*"` | LOW | ATOMIC | `git checkout --` |
| S3 | Primary REST Adapters & Exception Logging | IAM Primary REST | `AuthController.java`, `DepartmentController.java`, `UserRoleController.java`, `UserDepartmentController.java`, `RestExceptionHandler.java` | AC-3, AC-4 | `./gradlew test --tests "*ControllerTest*"` | LOW | ATOMIC | `git checkout --` |
| S4 | Application Services Business Telemetry & Verification | IAM Application Services | `LoginService.java`, `DepartmentService.java`, `AssignRolesService.java` | AC-3, AC-6 | `./gradlew test && ./gradlew check` | LOW | ATOMIC | `git checkout --` |

</slice_summary>

---

## 4. Slice Details

<slices>

  <slice id="S1">
    <objective>Establish shared request correlation filter (`TraceIdFilter`) and configure MDC pattern in `application.yaml`</objective>
    <change>
      - Create `LoggingConstants` with MDC keys (`TRACE_ID`, `CLIENT_IP`).
      - Create `TraceIdFilter` extending `OncePerRequestFilter` with `@Order(Ordered.HIGHEST_PRECEDENCE)`:
        - Extracts incoming `X-Request-Id` or `X-Trace-Id` or generates random UUID (compact 32-char hex or UUID string).
        - Populates `MDC.put("traceId", ...)`.
        - Adds `X-Trace-Id` to response header.
        - Guarantees `MDC.clear()` in `finally`.
      - Update `application.yaml`, `application-prod.yaml`, `application-staging.yaml` console log pattern to display `[%X{traceId:-}]`.
      - Write unit tests in `TraceIdFilterTest.java` verifying header propagation and MDC clearing.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/shared/infrastructure/logging/LoggingConstants.java`
      - `src/main/java/com/platform/app/shared/infrastructure/logging/TraceIdFilter.java`
      - `src/main/resources/application.yaml`
      - `src/main/resources/application-prod.yaml`
      - `src/main/resources/application-staging.yaml`
      - `src/test/java/com/platform/app/shared/infrastructure/logging/TraceIdFilterTest.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-1: Request correlation filter injects traceId to MDC, writes `X-Trace-Id` response header, and clears MDC in finally.
      - [ ] AC-5: `application.yaml` pattern outputs traceId in console logs.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "*TraceIdFilterTest*"
      ```
    </verifier>
    <expected_evidence>Tests in TraceIdFilterTest pass (exit code 0)</expected_evidence>
    <rollback_point>Remove shared logging directory and revert yaml changes</rollback_point>
    <stop_conditions>
      - Filter ordering conflicts or servlet filter compilation failures.
    </stop_conditions>
  </slice>

  <slice id="S2">
    <objective>Add calibrated security telemetry in `JwtAuthenticationFilter`, `JwtTokenProviderAdapter`, and `InMemoryAccountLockoutAdapter`</objective>
    <change>
      - In `JwtAuthenticationFilter`: Log token validation success at `DEBUG`, token validation failure / expiration at `WARN` with sanitized reason (no token string).
      - In `JwtTokenProviderAdapter`: Log token creation at `DEBUG` (with subject/userId, without raw token), validation errors at `DEBUG`/`WARN`.
      - In `InMemoryAccountLockoutAdapter`: Log failure counter increments and lockout activation at `WARN`.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/filter/JwtAuthenticationFilter.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/adapter/JwtTokenProviderAdapter.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/adapter/InMemoryAccountLockoutAdapter.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-2: Core authentication and authorization workflows log structured messages without leaking credentials.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "*AuthControllerTest*"
      ```
    </verifier>
    <expected_evidence>Existing controller/auth tests pass cleanly with new logs active</expected_evidence>
    <rollback_point>Revert security adapter modifications</rollback_point>
    <stop_conditions>
      - Leaking credentials or breaking security filter behavior.
    </stop_conditions>
  </slice>

  <slice id="S3">
    <objective>Instrument REST Controllers and calibrate `RestExceptionHandler` for 4xx and 5xx errors</objective>
    <change>
      - In `AuthController`: Log login attempt (email, client IP) at `INFO`, completion at `DEBUG`.
      - In `DepartmentController`, `UserRoleController`, `UserDepartmentController`: Log incoming mutations at `INFO` and queries at `DEBUG`.
      - In `RestExceptionHandler`:
        - Log validation errors at `WARN` (field error summary).
        - Log `InvalidCredentialsException`, `AccountDisabledException`, `AccountLockedException` at `WARN`.
        - Log `DepartmentNotFoundException`, `DepartmentCodeConflictException` at `WARN`.
        - Preserve `ERROR` for unexpected 5xx exceptions with stack trace.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AuthController.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/DepartmentController.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/UserRoleController.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/UserDepartmentController.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-3: REST controllers log key operational milestones.
      - [ ] AC-4: `RestExceptionHandler` logs client 4xx errors at `WARN` and 5xx errors at `ERROR`.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "*ControllerTest*"
      ```
    </verifier>
    <expected_evidence>All controller tests pass with 0 failures</expected_evidence>
    <rollback_point>Revert primary REST adapter modifications</rollback_point>
    <stop_conditions>
      - Modifying existing JSON error response structure.
    </stop_conditions>
  </slice>

  <slice id="S4">
    <objective>Instrument IAM Application Services and verify full regression suite</objective>
    <change>
      - In `LoginService`: Log authentication success at `INFO`, event publication at `DEBUG`.
      - In `DepartmentService`: Log department creation/update at `INFO`, code conflict / not found at `WARN`.
      - In `AssignRolesService`: Log role assignments at `INFO`, self-revocation rejection at `WARN`.
      - Run full validation: `./gradlew test` and `./gradlew check`.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/iam/application/services/LoginService.java`
      - `src/main/java/com/platform/app/iam/application/services/DepartmentService.java`
      - `src/main/java/com/platform/app/iam/application/services/AssignRolesService.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-3: Application services log operational milestones and domain events.
      - [ ] AC-6: Full automated test suite passes (`./gradlew test`, `./gradlew check`).
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test && ./gradlew check
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with 100% test pass rate</expected_evidence>
    <rollback_point>Revert application service changes</rollback_point>
    <stop_conditions>
      - Breaking service unit tests or spotless formatting check.
    </stop_conditions>
  </slice>

</slices>

---

## 5. Scope Contract

<scope_contract>
  <allowed>
    - Creating `com.platform.app.shared.infrastructure.logging.*` (MDC filter and constants).
    - Adding `@Slf4j` and calibrated log statements in REST controllers, exception handler, security filter/adapters, and application services.
    - Updating console log pattern in `application.yaml`, `application-prod.yaml`, `application-staging.yaml`.
    - Adding unit tests for `TraceIdFilter`.
  </allowed>
  <forbidden>
    - Any edits to domain model entities (`com.platform.app.iam.domain.model.*`).
    - Any changes to database migrations (`src/main/resources/db/changelog/*`).
    - Adding external dependencies to `build.gradle`.
    - Logging plaintext passwords, token strings, or secrets.
  </forbidden>
</scope_contract>

---

## 6. Verification Matrix

<verification_matrix>

| AC / Risk | Test / Command | Expected Evidence | Strictness | Actual Result |
|---|---|---|---|---|
| AC-1 | `./gradlew test --tests "*TraceIdFilterTest*"` | TraceIdFilterTest passes; header & MDC verified | hard-mandatory | PENDING |
| AC-2 | `./gradlew test --tests "*AuthControllerTest*"` | AuthControllerTest passes | hard-mandatory | PENDING |
| AC-3 | `./gradlew test --tests "*DepartmentServiceTest*" --tests "*AssignRolesServiceTest*"` | Service unit tests pass | hard-mandatory | PENDING |
| AC-4 | `./gradlew test --tests "*RestExceptionHandlerTest*"` | Exception handler tests pass | hard-mandatory | PENDING |
| AC-5 | `./gradlew check` | Spotless check and compilation pass | hard-mandatory | PENDING |
| AC-6 | `./gradlew test` | 100% test suite pass | hard-mandatory | PENDING |

</verification_matrix>

---

## Gate 2 — Plan Approved

<gate id="G2">
  - [x] Every slice has a defined verifier.
  - [x] Scope contract (allowed / forbidden) approved.
  - [x] Enforcement strictness assigned per AC/verification item.
  - [x] Stop conditions defined per slice.
  - [x] Rollback point defined per slice.
  - [x] Allowed commands listed.
  - [x] Plan approved.
  <approved_by>[AUTO: DELEGATED]</approved_by>
  <approved_date>2026-09-14</approved_date>
</gate>

</execution_plan>
