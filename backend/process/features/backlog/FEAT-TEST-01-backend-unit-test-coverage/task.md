# Task: [FEAT-TEST-01] Backend Comprehensive Unit Test Suite

<task_spec version="3.0" framework="RIPER-5">

<!-- ════════════════════════════════════════════
     SECTION 0 — TASK CONTROL (master state record)
     ════════════════════════════════════════════ -->
<task_control>
  <status>ACTIVE</status>  <!-- BACKLOG | ACTIVE | BLOCKED | PAUSED | REVIEW | COMPLETED | CANCELLED -->
  <spec_level>S1</spec_level>  <!-- S0=sketch | S1=defined | S2=verified | S3=locked -->
  <priority>P1</priority>  <!-- P0=urgent | P1=high | P2=normal | P3=low -->
  <risk>LOW</risk>  <!-- LOW | MEDIUM | HIGH -->
  <estimated_story_points>3</estimated_story_points>  <!-- 1 SP ≈ 2-4 focused engineering hours -->
  <working_mode>PAIR</working_mode>  <!-- PAIR (default: halts at each gate for review) | DELEGATED (fast-track: continuous autonomous run) | MANUAL | DIAGNOSE-ONLY -->
  <current_phase>RESEARCH</current_phase>  <!-- RESEARCH | INNOVATE | PLAN | EXECUTE | REVIEW -->
  <owner>@engineer</owner>
  <decision_owner>@engineer</decision_owner>
  <created>2026-09-14</created>
  <last_updated>2026-09-14</last_updated>
</task_control>

---

## 1. Specification (Pillar 1: Task / Spec)

<specification>
  <goal>
    Establish a comprehensive, resilient, and deterministic unit test suite for the backend subsystem, closing all critical test coverage gaps across `document`, `iam`, and `shared` bounded contexts (specifically targeting Security Filters, RestExceptionHandler, Persistence Adapters, and uncovered Domain Models), while maintaining 100% build pass rate without relying on external network or running infrastructure.
  </goal>

  <current_behavior>
    - Current backend test suite contains 120 passing tests across 22 test files.
    - Several critical components lack unit tests entirely:
      1. `JwtAuthenticationFilter`: Critical security barrier validating incoming Bearer JWT tokens has zero unit tests.
      2. `RestExceptionHandler`: Centralized HTTP exception handler handling 12+ domain exceptions (IAM and Document) has zero direct unit tests.
      3. `DocumentVersionRepositoryAdapter`: Document version persistence adapter has no dedicated unit test.
      4. `Role` & `Permission` domain models: Lack unit tests verifying invariants and equality.
      5. Enums (`AccessLevel`, `ProcessingStatus`) and Configuration properties (`S3StorageProperties`, `JwtProperties`) lack deterministic unit tests.
  </current_behavior>

  <expected_behavior>
    - Every critical security filter (`JwtAuthenticationFilter`), global exception mapping (`RestExceptionHandler`), repository adapter (`DocumentVersionRepositoryAdapter`), and domain model (`Role`, `Permission`) has isolated unit tests using JUnit 5, Mockito, and AssertJ.
    - All tests run hermetically in memory without requiring Docker, live S3, or live PostgreSQL.
    - Test suite passes cleanly with `./gradlew test` with zero failures and regression-free execution.
  </expected_behavior>

  <actor_authorization>
    System Developer / QA Engineer executing `./gradlew test`.
  </actor_authorization>

  <invariants>
    - Zero production source code regression: No changes to business logic in `src/main/java` unless fixing a proven defect discovered during testing.
    - Clean Architecture compliance: Unit tests for domain models must not load Spring ApplicationContext.
    - Hermetic test isolation: All external I/O (S3, DB, HTTP) must be mocked using Mockito or MockMvc in standalone mode.
  </invariants>

  <out_of_scope>
    - End-to-end multi-service test orchestration with AI service or Frontend.
    - Performance load testing (k6/Gatling) or long-running endurance benchmarks.
  </out_of_scope>

  <acceptance_criteria>
    - [ ] AC-1: `JwtAuthenticationFilterTest` covers all branch paths: missing token, non-bearer header, invalid token, valid token context establishment, and filter chain continuation.
    - [ ] AC-2: `RestExceptionHandlerTest` asserts exact HTTP status codes and RFC-7807/ErrorResponse payloads for all domain exceptions (400, 401, 403, 404, 409, 413, 415, 423, 502).
    - [ ] AC-3: `DocumentVersionRepositoryAdapterTest` verifies persistence mapping and retrieval between domain model and JPA entity.
    - [ ] AC-4: `RoleTest` and `PermissionTest` verify domain invariants, builder integrity, and value equality.
    - [ ] AC-5: Total test count increases from 120, with `./gradlew test` exiting 0 with zero failures.
  </acceptance_criteria>

  <!-- Definition-of-Ready (DoR) Gate -->
  <definition_of_ready>
    - [x] Target outcome and acceptance criteria are verifiable without guessing.
    - [x] Invariants and <out_of_scope> boundaries are explicit.
    - [x] Test scope whitelist is completely mapped against `src/main/java`.
  </definition_of_ready>
</specification>

---

## 2. Scope Contract (Pillar 3: Harness)

<scope_contract>
  <allowed_files>
    <file>process/features/active/FEAT-TEST-01-backend-unit-test-coverage/task.md</file>
    <file>process/features/active/FEAT-TEST-01-backend-unit-test-coverage/research.md</file>
    <file>process/features/active/FEAT-TEST-01-backend-unit-test-coverage/decision.md</file>
    <file>process/features/active/FEAT-TEST-01-backend-unit-test-coverage/plan.md</file>
    <file>process/features/active/FEAT-TEST-01-backend-unit-test-coverage/state.md</file>
    <file>process/features/active/FEAT-TEST-01-backend-unit-test-coverage/review.md</file>
    <file>process/features/active/FEAT-TEST-01-backend-unit-test-coverage/handoff.md</file>
    <file>src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/security/filter/JwtAuthenticationFilterTest.java</file>
    <file>src/test/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandlerTest.java</file>
    <file>src/test/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentVersionRepositoryAdapterTest.java</file>
    <file>src/test/java/com/platform/app/iam/domain/model/RoleTest.java</file>
    <file>src/test/java/com/platform/app/iam/domain/model/PermissionTest.java</file>
    <file>src/test/java/com/platform/app/document/domain/model/AccessLevelTest.java</file>
  </allowed_files>

  <forbidden_files>
    <file>build.gradle</file>
    <file>src/main/resources/**</file>
    <file>../docker-compose.yaml</file>
  </forbidden_files>
</scope_contract>

---

## 3. RIPER-5 Phase Status

<riper5_status>
  <phase name="RESEARCH" status="IN_PROGRESS" gate="G0" />
  <phase name="INNOVATE" status="PENDING" gate="G1" />
  <phase name="PLAN" status="PENDING" gate="G2" />
  <phase name="EXECUTE" status="PENDING" />
  <phase name="REVIEW" status="PENDING" gate="G3" />
</riper5_status>

</task_spec>
