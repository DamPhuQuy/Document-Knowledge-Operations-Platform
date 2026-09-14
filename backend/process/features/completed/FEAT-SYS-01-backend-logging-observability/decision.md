# Decision: DEC-LOG-01 Backend Structured Logging & Tracing Architecture

<technical_decision task_id="FEAT-SYS-01" dec_id="DEC-LOG-01" version="1.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. Present options. PAIR: engineer decides. DELEGATED: agent auto-selects optimal recommendation and advances. -->
<decision_status>
  <phase>INNOVATE</phase>
  <mode>READ-ONLY</mode>
  <decision_owner>@engineer</decision_owner>
  <last_updated>2026-09-14</last_updated>
</decision_status>

---

## 1. Context

<context>
  <task>[FEAT-SYS-01](task.md)</task>
  <research_artifact>[research.md](research.md)</research_artifact>
  <constraints>
    - Java 25, Spring Boot 4.0.7, Spring Security 7.x.
    - Strict adherence to Clean Hexagonal Architecture: domain models must not depend on infrastructure logging frameworks.
    - Zero Credential Leakage: Passwords, tokens, secrets, and sensitive PII must never be logged.
    - Thread safety: MDC context must be guaranteed to clean up in a finally block to prevent thread-pool leakage in Tomcat worker threads.
    - Zero disruption: Must not break existing HTTP contracts, status codes, or existing tests.
  </constraints>
</context>

---

## 2. Decision Required

<decision_question>
  What architecture and mechanisms should be established for request correlation tracing (MDC), shared logging infrastructure, and layer-by-layer log calibration across the backend subsystem?
</decision_question>

---

## 3. Options

<options>

  <option id="A">
    <approach>Spring MVC HandlerInterceptor & Controller Advice Only</approach>
    <advantages>
      - Easy to implement within standard Spring WebMVC.
      - Can access handler method metadata directly.
    </advantages>
    <disadvantages>
      - Does NOT intercept requests rejected early by Spring Security filter chains (e.g. `JwtAuthenticationFilter`, 401/403 pre-dispatch errors).
      - Cannot trace authentication failures or early filter exceptions with `traceId`.
      - Excludes non-MVC requests or static resources.
    </disadvantages>
    <complexity>LOW</complexity>
    <compatibility>HIGH</compatibility>
    <concurrency_transaction_risk>LOW</concurrency_transaction_risk>
    <testability>MEDIUM</testability>
    <maintainability>MEDIUM</maintainability>
  </option>

  <option id="B">
    <approach>Highest-Precedence Servlet Filter + SLF4J MDC + Layer-Calibrated Logging (Recommended)</approach>
    <advantages>
      - `TraceIdFilter` executes at `Ordered.HIGHEST_PRECEDENCE`, wrapping the entire request lifecycle including Spring Security filters, dispatch, service execution, and error handling.
      - Guarantees `X-Trace-Id` propagation in response headers and deterministic `MDC.clear()` in a `try...finally` block.
      - Shared infrastructure cleanly housed in `com.platform.app.shared.infrastructure.logging`.
      - Calibrated log levels across layers:
        - Security / Filter: `DEBUG` for valid auth, `WARN` for token validation failures (sanitized).
        - REST Controllers: `DEBUG` for request start/finish with latency.
        - Application Services: `INFO` for state changes & domain events, `WARN` for business conflicts.
        - Exception Handler: `WARN` for 4xx client errors, `ERROR` for 5xx system errors.
      - Zero new third-party dependencies required (uses built-in SLF4J + Logback + Spring Web).
    </advantages>
    <disadvantages>
      - Does not capture full serialized request bodies (intentional to prevent credential leakage and upload memory overhead).
    </disadvantages>
    <complexity>LOW</complexity>
    <compatibility>HIGH</compatibility>
    <concurrency_transaction_risk>LOW (deterministic thread-local cleanup in filter finally block)</concurrency_transaction_risk>
    <testability>HIGH</testability>
    <maintainability>HIGH</maintainability>
  </option>

  <option id="C">
    <approach>Full OpenTelemetry SDK / Micrometer Tracing with Logback JSON Appender</approach>
    <advantages>
      - Produces native OpenTelemetry spans and JSON log lines ready for distributed collectors (Jaeger, Tempo, Datadog).
    </advantages>
    <disadvantages>
      - Introduces substantial heavyweight dependencies (`micrometer-tracing`, `opentelemetry-javaagent` or exporter libraries) into `build.gradle`.
      - Adds configuration complexity and overhead for local development and H2 testing.
      - Overkill for current single-subsystem monolithic stage.
    </disadvantages>
    <complexity>HIGH</complexity>
    <compatibility>MEDIUM</compatibility>
    <concurrency_transaction_risk>MEDIUM</concurrency_transaction_risk>
    <testability>MEDIUM</testability>
    <maintainability>MEDIUM</maintainability>
  </option>

</options>

---

## 4. Trade-off Matrix

<tradeoff_matrix>

| Criterion | Option A (Interceptor) | Option B (Filter + MDC) | Option C (OTel / Micrometer) |
|---|:---:|:---:|:---:|
| Security Layer Coverage | 1 | 5 | 5 |
| Architectural Purity | 4 | 5 | 3 |
| Implementation Simplicity | 4 | 5 | 2 |
| Dependency & Runtime Overhead | 5 | 5 | 2 |
| Credential Leakage Prevention | 3 | 5 | 3 |
| Testability & Verification | 4 | 5 | 3 |
| Total Score | 21 / 30 | **30 / 30** | 18 / 30 |

<!-- Score: 1 (poor) → 5 (excellent) -->
</tradeoff_matrix>

---

## 5. Recommendation

<recommendation>
  Adopt **Option B (Highest-Precedence Servlet Filter + SLF4J MDC + Layer-Calibrated Logging)**:
  1. Build a lightweight, bulletproof `TraceIdFilter` in `shared/infrastructure/logging` that reads incoming `X-Request-Id` / `X-Trace-Id` or generates a fresh UUID, places it in `MDC`, sets the response header, and guarantees `MDC.clear()` in `finally`.
  2. Configure uniform console & file pattern in `application.yaml` / `application-prod.yaml` displaying `%X{traceId}`.
  3. Calibrate log statements across `iam` layers (Controllers, Exception Handler, Services, Security Filters, Adapters) with strict parameterization and zero credential exposure.
</recommendation>

---

## 6. Implementation Decision

<!-- PAIR mode: Completed by engineer before Gate 1 passes.
     DELEGATED / Fast-Track mode: Agent automatically populates Recommendation
     into <selected_option>, documents rationale, signs Gate 1 with [AUTO: DELEGATED], and proceeds. -->
<engineer_decision>
  <selected_option>Option B</selected_option>
  <rationale>[AUTO: DELEGATED] Option B provides full end-to-end request coverage (including Spring Security filters), ensures deterministic MDC cleanup, introduces zero dependency overhead, strictly satisfies Clean Architecture, and guarantees zero credential leakage.</rationale>
  <rejected_alternatives>
    - Option A: Rejected because HandlerInterceptor does not cover Spring Security filter chain or pre-dispatch errors.
    - Option C: Rejected as over-engineering for the current backend stage, adding heavy external dependencies without immediate need.
  </rejected_alternatives>
</engineer_decision>

---

## 7. Constraints Created by This Decision

<constraints_created>
  - `TraceIdFilter` must be ordered before `SecurityFilterChain` (`Ordered.HIGHEST_PRECEDENCE`).
  - No domain model under `com.platform.app.iam.domain.model.*` may import SLF4J or log directly.
  - Sensitive fields (`password`, `passwordHash`, `token`, `refreshToken`, `authorization`) must never be passed to logger arguments.
  - Every filter execution must execute `MDC.clear()` in `finally`.
</constraints_created>

---

## 8. Evidence Still Required

<evidence_required>
  - Automated test verifying `X-Trace-Id` header is returned on both 2xx and 4xx/5xx responses.
  - Automated test verifying MDC cleanup after request completes.
</evidence_required>

---

## Gate 1 — Decision Approved

<gate id="G1">
  - [x] All options presented and trade-offs analyzed.
  - [x] Selected option recorded above.
  - [x] No blocking business / schema / security decision remains open.
  <approved_by>[AUTO: DELEGATED]</approved_by>
  <approved_date>2026-09-14</approved_date>
</gate>

</technical_decision>
