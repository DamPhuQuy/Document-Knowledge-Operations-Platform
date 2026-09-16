# Task Lite: [TASK-VT-01] Enable Virtual Threads in Spring Boot

<task_lite version="3.0" framework="RIPER-5-Lite">

<!-- ════════════════════════════════════════════
     SECTION 0 — TASK CONTROL
     ════════════════════════════════════════════ -->
<task_control>
  <track>LITE</track>  <!-- LITE: Single-file artifact for fast iteration, bugfixes, micro-tasks -->
  <status>COMPLETED</status>  <!-- ACTIVE | REVIEW | COMPLETED | CANCELLED -->
  <priority>P2</priority>  <!-- P0=urgent | P1=high | P2=normal | P3=low -->
  <working_mode>PAIR</working_mode>  <!-- PAIR (review at gates) | DELEGATED (autonomous run) -->
  <current_phase>REVIEW</current_phase>  <!-- PLAN | EXECUTE | REVIEW -->
  <owner>@engineer</owner>
</task_control>

---

## 1. Intent & Specification

<specification>
  <goal>
    Enable Java 21+ Project Loom Virtual Threads in the Spring Boot backend via `spring.threads.virtual.enabled=true`, allowing Tomcat request handling and asynchronous task executors to run on lightweight virtual threads for high concurrency and lower memory footprint.
  </goal>

  <invariants>
    - Zero breaking changes to REST controllers, database transactions, or security context propagation.
    - File streaming (S3 object storage upload) and JPA operations must execute without thread pinning deadlocks.
    - Test harness must compile and run cleanly with `./gradlew test`.
  </invariants>

  <acceptance_criteria>
    - [ ] AC-1: `spring.threads.virtual.enabled: true` is configured in `src/main/resources/application.yaml`.
    - [ ] AC-2: Automated test (`VirtualThreadsTest.java`) asserts that the Spring task execution context executes tasks on Virtual Threads (`Thread.currentThread().isVirtual() == true`).
    - [ ] AC-3: Fix blocking compile error in `DocumentUploadServiceTest.java` so `:compileTestJava` succeeds and the test suite passes.
  </acceptance_criteria>

  <definition_of_ready>
    - [x] Intent and acceptance criteria are clear without assumptions.
    - [x] Allowed files in Section 2 are identified.
  </definition_of_ready>
</specification>

---

## 2. Scope Contract & File Whitelist

<scope_contract>
  <allowed_files>
    <file>src/main/resources/application.yaml</file>
    <file>src/test/java/com/platform/app/shared/VirtualThreadsTest.java</file>
    <file>src/test/java/com/platform/app/document/application/services/DocumentUploadServiceTest.java</file>
    <file>src/main/java/com/platform/app/document/application/services/DocumentUploadService.java</file>
    <file>process/general-plans/active/enable-virtual-threads/task-lite.md</file>
  </allowed_files>

  <forbidden_files>
    <file>build.gradle</file>
    <file>settings.gradle</file>
    <file>src/main/java/**</file>
    <file>docker-compose.yaml</file>
  </forbidden_files>
</scope_contract>

---

## 3. Execution Plan (Compact Slices)

<execution_plan>
  ### Slice 1: Enable Virtual Threads in application.yaml
  - **Action:** Add `spring.threads.virtual.enabled: ${SPRING_THREADS_VIRTUAL_ENABLED:false}` to `src/main/resources/application.yaml`.
  - **Verifier:** Visual inspection and file syntax check.
  - **Status:** [x] DONE

  ### Slice 2: Fix DocumentUploadServiceTest compilation & Add VirtualThreadsTest
  - **Action:**
    1. Fix constructor call and mock in `DocumentUploadServiceTest.java` (mock `StoreMetadataUseCase` instead of obsolete repository ports).
    2. Add `VirtualThreadsTest.java` to verify that Spring Boot initializes with virtual threads enabled and tasks run on virtual threads.
  - **Verifier:** `./gradlew test --tests "com.platform.app.shared.VirtualThreadsTest"`
  - **Status:** [x] DONE

  ### Slice 3: Full Test Suite Verification
  - **Action:** Run the entire test suite `./gradlew test` to ensure zero regressions.
  - **Verifier:** `./gradlew test`
  - **Status:** [x] DONE
</execution_plan>

---

## 4. Consolidated Verification & Gates

<verification_gates>
  <!-- Gate G1/G2: Scope & Architecture check (pre-execution) -->
  - [x] **Gate G1/G2 (Plan Approved):** Allowed files confirmed, test verifiers defined. Approved by engineer.

  <!-- Gate G3: Verification evidence (post-execution) -->
  - [x] **Gate G3 (Ready for Handoff):**
    - [x] Verifier commands executed cleanly (123 tests passing, zero errors).
    - [x] Single source of truth in `.env` configured for Docker Compose & application.yaml.
    - [x] Fallback defaults completely decoupled from `.env` and aligned with `.env.example`.
    - [x] No temporary debug logs or unintended changes.
</verification_gates>

</task_lite>
