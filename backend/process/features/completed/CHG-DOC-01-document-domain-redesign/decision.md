# Decision: DEC-DOC-02 Document Domain Model & Schema Redesign

<technical_decision task_id="CHG-DOC-01" dec_id="DEC-DOC-02" version="1.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. Present options. PAIR: engineer decides. DELEGATED: agent auto-selects optimal recommendation and advances. -->
<decision_status>
  <phase>INNOVATE</phase>
  <mode>READ-ONLY</mode>
  <decision_owner>@engineer</decision_owner>
  <last_updated>2026-09-16</last_updated>
</decision_status>

---

## 1. Context

<context>
  <task>process/features/active/CHG-DOC-01-document-domain-redesign/task.md</task>
  <research_artifact>specs/business/use_cases/02_document_management.md</research_artifact>
  <constraints>
    - Adhere strictly to the core domain requirement: "A document needs at least what information so that the system could comprehend it".
    - Do not model hypothetical future features; model strictly what current use cases (UC-DOC-01: Document Upload & Storage) actually need.
    - Preserve Clean Architecture: Domain POJO must remain free of Spring, JPA, and AWS dependencies.
    - Zero regression on existing functional tests and authorization invariants.
  </constraints>
</context>

---

## 2. Decision Required

<decision_question>
  Which domain modeling and database schema strategy should be adopted to eliminate over-engineering in Document.java (originally containing 19 concepts) while ensuring the system retains complete comprehension of ingested documents?
</decision_question>

---

## 3. Options

<options>

  <option id="A">
    <approach>
      Core Comprehension Lean Model (Recommended):
      - Trim Document down to 11 essential concepts representing the 5 core facets of document comprehension:
        1. Identity & Name: `id`, `title`, `originalFileName`
        2. Media & Format: `contentType` (standard MIME type e.g. "application/pdf", eliminates redundant `fileType`)
        3. Footprint & Integrity: `fileSizeBytes`, `checksumSha256`
        4. Storage Location: `storageKey` (eliminates infrastructure leak `storageBucket`)
        5. Lifecycle & Ownership: `status` (DocumentStatus: UPLOADED, PROCESSING, READY, FAILED), `uploadedByUserId`, `departmentId`, `accessLevel`, `createdAt`, `updatedAt`
      - Eliminate: `fileType` (duplicated by `contentType`), `storageBucket` (environment config in S3StorageProperties), `isS3Synced` (dead code), `metadata` (untyped JSON junk drawer), `description` (unused by parsers/RAG), `currentVersion` (premature optimization for UC-DOC-02), `deletedAt` (unused by UC-DOC-01).
      - Database & JPA: Update `documents` table in Liquibase and `DocumentJpaEntity` to match these exact 11 columns, dropping the 8 dead columns and indexes.
      - Persistence Flow: Keep upload lean without redundant duplicate inserts into `document_versions` for initial uploads.
    </approach>
    <advantages>
      - Drastically simplifies the domain model from 19 fields to 11 coherent attributes.
      - Eliminates database bloat and removes dead code/columns.
      - Fixes infrastructure leaking into domain (`storageBucket` removed).
      - Perfectly matches active use case requirements (UC-DOC-01) while remaining forward-extensible.
    </advantages>
    <disadvantages>
      - Requires updating Liquibase changelog and adjusting DTO/adapter mapping and existing test assertions.
    </disadvantages>
    <complexity>LOW</complexity>
    <compatibility>Requires updating internal DTO and database schema; API response remains clean</compatibility>
    <concurrency_transaction_risk>LOW (pure schema simplification and cleaner domain POJO)</concurrency_transaction_risk>
    <testability>HIGH (cleaner mock setups and straightforward assertions)</testability>
    <maintainability>EXCELLENT (clean DDD aggregate root with clear invariants)</maintainability>
  </option>

  <option id="B">
    <approach>
      Ultra-Minimal Binary Storage Pointer:
      - Treat Document strictly as a low-level binary storage record: `id`, `originalFileName`, `contentType`, `fileSizeBytes`, `checksumSha256`, `storageKey`, `uploadedByUserId`, `createdAt`.
      - Strip out `accessLevel` and `departmentId`, delegating all security classification entirely to an external ACL subsystem.
    </approach>
    <advantages>
      - Extremely small model (8 fields).
    </advantages>
    <disadvantages>
      - Violates Business Rule B2 in UC-DOC-01 (default access level `INTERNAL` scoped to the uploader's `department_id`).
      - Pre-filtering during RAG retrieval becomes complex and slow because document access levels cannot be checked on the primary document entity.
    </disadvantages>
    <complexity>MEDIUM</complexity>
    <compatibility>LOW (breaks UC-DOC-01 and access control specs)</compatibility>
    <concurrency_transaction_risk>MEDIUM (cross-table coordination needed for simple document queries)</concurrency_transaction_risk>
    <testability>MEDIUM</testability>
    <maintainability>POOR (anemic domain requiring multiple joins for basic authorization)</maintainability>
  </option>

  <option id="C">
    <approach>
      Conservative Cleanup:
      - Only remove `fileType`, `isS3Synced`, and `metadata`.
      - Keep `description`, `currentVersion`, `deletedAt`, and `storageBucket` in the domain entity and DB schema.
    </approach>
    <advantages>
      - Minimal change to current classes (reduces fields from 19 to 16).
    </advantages>
    <disadvantages>
      - Leaves 16 concepts, retaining speculative modeling and premature optimization.
      - Retains infrastructure leaks (`storageBucket`) and unused fields (`description`, `deletedAt`).
    </disadvantages>
    <complexity>LOW</complexity>
    <compatibility>HIGH</compatibility>
    <concurrency_transaction_risk>LOW</concurrency_transaction_risk>
    <testability>MEDIUM</testability>
    <maintainability>FAIR (still over-engineered)</maintainability>
  </option>

</options>

---

## 4. Trade-off Matrix

<tradeoff_matrix>

| Criterion | Option A (Core Comprehension Lean) | Option B (Ultra-Minimal Binary Pointer) | Option C (Conservative Cleanup) |
|---|:---:|:---:|:---:|
| Alignment with Core Domain Question | 5 | 3 | 2 |
| Avoidance of Premature Modeling | 5 | 5 | 2 |
| Business Rule Compliance (UC-DOC-01 & B2) | 5 | 2 | 5 |
| Infrastructure Separation (No Bucket Leaks) | 5 | 5 | 1 |
| Testability & Maintainability | 5 | 3 | 3 |

<!-- Score: 1 (poor) → 5 (excellent) -->
</tradeoff_matrix>

---

## 5. Recommendation

<recommendation>
  Adopt **Option A (Core Comprehension Lean Model)**.
  - It directly answers the core question: what does the system need to comprehend the document? (Identity, Media format for parsers, Size & SHA-256 for integrity, Storage key for reading bytes, Status for processing readiness, and Owner/Dept/AccessLevel for governance).
  - It eliminates 8 redundant, dead, or leaked fields (`fileType`, `storageBucket`, `isS3Synced`, `metadata`, `description`, `currentVersion`, `deletedAt`).
  - It respects UC-DOC-01 invariants without over-engineering for future, un-implemented use cases.
</recommendation>

---

## 6. Implementation Decision

<engineer_decision>
  <selected_option>A</selected_option>
  <rationale>Approved by engineer. Option A (Core Comprehension Lean Model) eliminates redundant, dead, and leaked fields, focusing strictly on the 11 essential concepts required for the system to comprehend and operate documents under UC-DOC-01.</rationale>
  <rejected_alternatives>
    - Option B: Stripping access level and department breaks Rule B2 and authorization pre-filtering.
    - Option C: Retains speculative modeling and infrastructure leaks.
  </rejected_alternatives>
</engineer_decision>

---

## 7. Constraints Created by This Decision

<constraints_created>
  - `Document.java` must contain only the 11 approved fields and domain state-transition methods (`markProcessing`, `markReady`, `markFailed`).
  - `contentType` must be normalized (e.g. lowercase trimmed MIME type `application/pdf`).
  - `storageKey` is relative to the bucket; the S3 bucket name is resolved solely in infrastructure adapter (`S3StorageProperties`).
  - `DocumentJpaEntity` and Liquibase `003-create-document-tables.yaml` must reflect the 11 columns and clean indexes.
</constraints_created>

---

## 8. Evidence Still Required

<evidence_required>
  - All unit and integration tests under `src/test/java/com/platform/app/document/` pass with `./gradlew test`.
  - `./gradlew spotlessCheck` passes.
</evidence_required>

---

## Gate 1 — Decision Approved

<gate id="G1">
  - [x] All options presented and trade-offs analyzed.
  - [x] Selected option recorded above.
  - [x] No blocking business / schema / security decision remains open.
  <approved_by>@engineer</approved_by>
  <approved_date>2026-09-16</approved_date>
</gate>

</technical_decision>
