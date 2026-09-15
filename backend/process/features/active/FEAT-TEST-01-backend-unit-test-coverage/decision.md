# Decision: DEC-TEST-01 Backend Comprehensive Unit Test Suite

<technical_decision task_id="FEAT-TEST-01" dec_id="DEC-TEST-01" version="1.0" framework="RIPER-5">

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
  <task>[FEAT-TEST-01](task.md)</task>
  <research_artifact>[research.md](research.md)</research_artifact>
  <constraints>
    - Must not introduce Spring ApplicationContext overhead to pure domain models or adapter unit tests.
    - Must execute completely offline without Docker, Postgres, or S3 dependencies.
    - Must achieve high branch coverage for `JwtAuthenticationFilter` and `RestExceptionHandler`.
  </constraints>
</context>

---

## 2. Decision Required

<decision_question>
  What architectural testing strategy should be adopted to close the unit test coverage gaps across `JwtAuthenticationFilter`, `RestExceptionHandler`, `DocumentVersionRepositoryAdapter`, and domain models?
</decision_question>

---

## 3. Options

<options>

  <option id="A">
    <approach>Pure Hermetic Unit Testing with Mockito, MockHttpServletRequest & AssertJ (Recommended)</approach>
    <advantages>
      - Blazing fast execution (< 200ms total runtime for all new tests).
      - Zero Spring ApplicationContext bootstrap overhead.
      - 100% deterministic and isolated from external environments.
      - Directly exercises every conditional branch, edge case, and null check without filter chain boilerplate.
      - Strictly adheres to the Tactical DDD Testing Pyramid.
    </advantages>
    <disadvantages>
      - Does not test Spring annotation scanning (e.g. `@RestControllerAdvice` discovery), though this is already validated by the existing MockMvc controller slice tests.
    </disadvantages>
    <complexity>LOW</complexity>
    <compatibility>100% backward compatible</compatibility>
    <concurrency_transaction_risk>None (in-memory unit testing)</concurrency_transaction_risk>
    <testability>Maximum testability with granular branch assertions</testability>
    <maintainability>High; no fragile Spring context cache invalidations</maintainability>
  </option>

  <option id="B">
    <approach>Spring Web Slice Testing (@WebMvcTest) for Filter & ExceptionHandler + Unit Tests for Domain</approach>
    <advantages>
      - Validates Spring WebMVC dispatcher resolution and auto-wiring.
    </advantages>
    <disadvantages>
      - Slower build times (spins up Spring WebMvc slices taking 2-5 seconds).
      - Complex SecurityFilter mock setup required to test `JwtAuthenticationFilter` in isolation.
      - Fragile when global security configs or custom properties are involved.
    </disadvantages>
    <complexity>MEDIUM</complexity>
    <compatibility>100% backward compatible</compatibility>
    <concurrency_transaction_risk>None</concurrency_transaction_risk>
    <testability>Medium; requires configuring MockMvc filters</testability>
    <maintainability>Medium</maintainability>
  </option>

</options>

---

## 4. Trade-off Matrix

<trade_off_matrix>
| Criterion | Weight | Option A (Pure Unit) | Option B (Web Slice) |
| :--- | :---: | :---: | :---: |
| Execution Speed & Feedback Loop | 30% | **5/5** (<200ms) | 3/5 (~3-5s) |
| Branch & Edge-Case Coverage | 30% | **5/5** (Direct invocation) | 4/5 (Via HTTP mock) |
| Clean Architecture Adherence | 20% | **5/5** (Pure ports/adapters) | 3/5 (Coupled to Spring) |
| Maintenance Overhead | 20% | **5/5** (Zero context caching) | 3/5 (Context reloads) |
| **Weighted Total** | 100% | **5.0 / 5.0** | **3.4 / 5.0** |
</trade_off_matrix>

---

## 5. Recommendation

<recommendation>
  **Adopt Option A (Pure Hermetic Unit Testing)**.
  Existing controller tests (`AuthControllerTest`, `DepartmentControllerTest`, `DocumentControllerTest`, etc.) already provide extensive Spring `@WebMvcTest` coverage for HTTP dispatch. The remaining gaps (`JwtAuthenticationFilter`, `RestExceptionHandler`, `DocumentVersionRepositoryAdapter`, `Role`, `Permission`) are best covered by pure, deterministic unit tests with Mockito and MockHttpServletRequest. This keeps build times fast and test failures immediately pinpointable.
</recommendation>

---

## 6. Engineer Decision & Gate 1 Sign-Off

<engineer_decision>
  <!-- In PAIR mode: Left for the engineer to sign off. -->
  <selected_option></selected_option>
  <rationale></rationale>
  <sign_off gate="G1">
    <approved_by></approved_by>
    <date></date>
  </sign_off>
</engineer_decision>

</technical_decision>
