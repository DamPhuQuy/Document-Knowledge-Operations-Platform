# Task Lite: [CHG-DOC-02] Streamline DocumentVersion & Entity

<task_lite version="3.0" framework="RIPER-5-Lite">

<!-- ════════════════════════════════════════════
     SECTION 0 — TASK CONTROL
     ════════════════════════════════════════════ -->
<task_control>
  <track>LITE</track>
  <status>COMPLETED</status>
  <priority>P2</priority>
  <working_mode>DELEGATED</working_mode>
  <current_phase>HANDOFF</current_phase>
  <owner>@engineer</owner>
</task_control>

---

## 1. Intent & Specification

<specification>
  <goal>
    Streamline DocumentVersion domain model, JPA entity, repository adapter, Liquibase migration, and database specifications by eliminating infrastructure leaks (storageBucket) and dead flags (isS3Synced), retaining only the 8 essential version snapshot concepts.
  </goal>

  <invariants>
    - Pure POJO Domain Model without framework dependencies.
    - Zero regressions across existing test suite.
  </invariants>

  <acceptance_criteria>
    - [x] AC-1: DocumentVersion domain model and DocumentVersionJpaEntity contain only the 8 essential concepts: id, documentId, versionNumber, storageKey, fileSizeBytes, checksumSha256, changeSummary, uploadedByUserId, createdAt.
    - [x] AC-2: Liquibase 003 and dbml schema specifications reflect the streamlined document_versions table (dropping storage_bucket, is_s3_synced).
    - [x] AC-3: DocumentVersionRepositoryAdapter, DocumentVersionTest, and DocumentRepositoryAdapterTest updated and passing.
    - [x] AC-4: All automated checks and tests pass cleanly (`./gradlew check`).
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
    <file>src/main/java/com/platform/app/document/domain/model/DocumentVersion.java</file>
    <file>src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/entity/DocumentVersionJpaEntity.java</file>
    <file>src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentVersionRepositoryAdapter.java</file>
    <file>src/main/resources/db/changelog/changes/003-create-document-tables.yaml</file>
    <file>docs/specs/database/schema.dbml</file>
    <file>docs/specs/database/modules/02_document_management.dbml</file>
    <file>src/test/java/com/platform/app/document/domain/model/DocumentVersionTest.java</file>
    <file>src/test/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentRepositoryAdapterTest.java</file>
  </allowed_files>

  <forbidden_files>
    <file>src/main/java/com/platform/app/iam/**</file>
    <file>src/main/java/com/platform/app/audit/**</file>
  </forbidden_files>
</scope_contract>

---

## 3. Execution Plan (Compact Slices)

<execution_plan>
  ### Slice 1: Streamline DocumentVersion Domain Model & Tests
  - **Action:** Remove storageBucket and isS3Synced from DocumentVersion.java; update DocumentVersionTest.java.
  - **Verifier:** `./gradlew test --tests "*DocumentVersionTest*"`
  - **Status:** [x] DONE

  ### Slice 2: Streamline Entity, Adapter & Database Schemas
  - **Action:** Remove storage_bucket and is_s3_synced from DocumentVersionJpaEntity.java, DocumentVersionRepositoryAdapter.java, 003-create-document-tables.yaml, schema.dbml, and 02_document_management.dbml. Update DocumentRepositoryAdapterTest.java.
  - **Verifier:** `./gradlew test --tests "*DocumentRepositoryAdapterTest*"`
  - **Status:** [x] DONE

  ### Slice 3: Full Regression & Code Quality Gate
  - **Action:** Run `./gradlew check` to verify spotless and all 123+ tests.
  - **Verifier:** `./gradlew check`
  - **Status:** [x] DONE
</execution_plan>

---

## 4. Consolidated Verification & Gates

<verification_gates>
  <!-- Gate G1/G2: Scope & Architecture check (pre-execution) -->
  - [x] **Gate G1/G2 (Plan Approved):** Allowed files confirmed, test verifiers defined. [AUTO: DELEGATED]

  <!-- Gate G3: Verification evidence (post-execution) -->
  - [x] **Gate G3 (Ready for Handoff):**
    - [x] Verifier commands executed cleanly (Zero errors).
    - [x] Git diff inspected — NO files touched outside `<allowed_files>`.
    - [x] No temporary debug logs or unintended changes.
</verification_gates>

</task_lite>
