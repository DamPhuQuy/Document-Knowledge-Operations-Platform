# Task Lite: [CHG-SHR-01] Implement UUIDv7 Generator & Migrate Entities

<task_lite version="3.0" framework="RIPER-5-Lite">

<!-- ════════════════════════════════════════════
     SECTION 0 — TASK CONTROL
     ════════════════════════════════════════════ -->
<task_control>
  <track>LITE</track>
  <status>COMPLETED</status>
  <priority>P2</priority>
  <working_mode>PAIR</working_mode>
  <current_phase>REVIEW</current_phase>
  <owner>@engineer</owner>
</task_control>

---

## 1. Intent & Specification

<specification>
  <goal>
    Add 'com.github.f4b6a3:uuid-creator' dependency, implement a shared IdGenerator utility in `shared/util/` producing RFC 9562 UUIDv7 IDs, and replace existing `UUID.randomUUID()` calls in domain entities and application services.
  </goal>

  <invariants>
    - Zero regressions across existing test suite (`./gradlew test`).
    - Domain models remain pure and clean without heavy framework coupling.
    - Generated UUIDs are valid version 7 (variant 2, version 7, time-ordered epoch).
  </invariants>

  <acceptance_criteria>
    - [x] AC-1: `build.gradle` includes `com.github.f4b6a3:uuid-creator:6.0.0`.
    - [x] AC-2: `IdGenerator` utility class implemented in `com.platform.app.shared.util` providing `nextId()` generating RFC 9562 UUIDv7.
    - [x] AC-3: Comprehensive unit tests in `IdGeneratorTest` verifying UUID version 7, variant 2, and time-ordered monotonicity.
    - [x] AC-4: Existing `UUID.randomUUID()` calls in Document and IAM domain models and application services migrated to `IdGenerator.nextId()`.
    - [x] AC-5: Full test suite passes cleanly with `./gradlew test` and Spotless check passes (`./gradlew check`).
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
    <file>build.gradle</file>
    <file>src/main/java/com/platform/app/shared/util/IdGenerator.java</file>
    <file>src/test/java/com/platform/app/shared/util/IdGeneratorTest.java</file>
    <file>src/main/java/com/platform/app/document/domain/model/Document.java</file>
    <file>src/main/java/com/platform/app/document/domain/model/DocumentVersion.java</file>
    <file>src/main/java/com/platform/app/document/domain/model/DocumentUserAccess.java</file>
    <file>src/main/java/com/platform/app/document/domain/model/DocumentDepartmentAccess.java</file>
    <file>src/main/java/com/platform/app/document/domain/model/DocumentRoleAccess.java</file>
    <file>src/main/java/com/platform/app/document/application/services/DocumentUploadService.java</file>
    <file>src/main/java/com/platform/app/document/application/services/DocumentVersionService.java</file>
    <file>src/main/java/com/platform/app/iam/domain/model/RefreshToken.java</file>
    <file>src/main/java/com/platform/app/iam/application/services/DepartmentService.java</file>
  </allowed_files>

  <forbidden_files>
    <file>src/main/resources/db/changelog/**</file>
    <file>docs/**</file>
  </forbidden_files>
</scope_contract>

---

## 3. Execution Plan (Compact Slices)

<execution_plan>
  ### Slice 1: Add uuid-creator dependency & Implement IdGenerator with Unit Tests
  - **Action:** Add `com.github.f4b6a3:uuid-creator:6.0.0` to `build.gradle`. Create `IdGenerator.java` in `com.platform.app.shared.util` and `IdGeneratorTest.java` in `src/test/java/com/platform/app/shared/util/`.
  - **Verifier:** `./gradlew test --tests "com.platform.app.shared.util.IdGeneratorTest"`
  - **Status:** [x] DONE

  ### Slice 2: Migrate Domain Entities and Application Services to IdGenerator
  - **Action:** Replace `UUID.randomUUID()` with `IdGenerator.nextId()` across `Document`, `DocumentVersion`, `DocumentUserAccess`, `DocumentDepartmentAccess`, `DocumentRoleAccess`, `DocumentUploadService`, `DocumentVersionService`, `RefreshToken`, and `DepartmentService`.
  - **Verifier:** `./gradlew test` and `./gradlew check`
  - **Status:** [x] DONE
</execution_plan>

---

## 4. Consolidated Verification & Gates

<verification_gates>
  <!-- Gate G1/G2: Scope & Architecture check (pre-execution) -->
  - [x] **Gate G1/G2 (Plan Approved):** Allowed files confirmed, test verifiers defined.

  <!-- Gate G3: Verification evidence (post-execution) -->
  - [x] **Gate G3 (Ready for Handoff):**
    - [x] Verifier commands executed cleanly (Zero errors via `./gradlew check`).
    - [x] Git diff inspected — NO files touched outside `<allowed_files>`.
    - [x] No temporary debug logs or unintended changes.
</verification_gates>

</task_lite>
