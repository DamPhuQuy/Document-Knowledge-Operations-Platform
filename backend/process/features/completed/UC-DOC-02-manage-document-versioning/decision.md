# Decision: DEC-DOC-03 Document Version Progression & Transaction Coordination

<technical_decision task_id="UC-DOC-02" dec_id="DEC-DOC-03" version="1.0" framework="RIPER-5">

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
  <task>process/features/active/UC-DOC-02-manage-document-versioning/task.md</task>
  <research_artifact>process/features/active/UC-DOC-02-manage-document-versioning/research.md</research_artifact>
  <constraints>
    - Adhere strictly to UC-DOC-02 specification requirements:
      - Post-condition 1: New version record created with `version_number = current_version + 1`.
      - Post-condition 2: `documents.current_version` points to the new version.
      - Post-condition 3: Historical version records in `document_versions` remain intact and immutable (Rule B1).
      - Step 6: Backend updates `documents.current_version`, `storage_key`, `checksum_sha256`, and sets `status = UPLOADED` / `PENDING`.
      - Step 7: Backend emits `DocumentVersionCreatedEvent` for re-indexing.
      - Step 9: Returns HTTP 200 OK with new version details.
    - Clean Architecture: Domain entities (`Document`, `DocumentVersion`) remain pure Java POJOs without AWS SDK or Spring Data JPA dependencies.
    - NF1: Version transition must be ACID-compliant with zero downtime for readers.
    - Zero orphaned objects: If database transaction fails, uploaded S3 object must be removed via compensation delete.
  </constraints>
</context>

---

## 2. Decision Required

<decision_question>
  How should the Document aggregate root and document_versions coordinate version progression, active storage pointer synchronization, and transaction rollback/compensation when uploading document revision v2+?
</decision_question>

---

## 3. Options

<options>

  <option id="A">
    <approach>
      Explicit `currentVersion` Pointer on `Document` with Dual-Write Atomic Transaction (Recommended):
      - Schema & Entity:
        - Add `currentVersion` (int, default 1) to `Document.java`, `DocumentJpaEntity`, and `documents` table.
      - Version Calculation:
        - Next version is determined directly from the aggregate root: `nextVersion = document.getCurrentVersion() + 1`.
      - Storage & Streaming:
        - Stream upload file to S3 under `documents/{docId}/v{nextVersion}/{sanitizedFileName}` with single-pass `DigestInputStream` (SHA-256).
      - Atomic Persistence (@Transactional):
        - Insert new `DocumentVersion` snapshot (`id`, `documentId`, `versionNumber = nextVersion`, `storageKey`, `fileSizeBytes`, `checksumSha256`, `changeSummary`, `uploadedByUserId`, `createdAt = now`).
        - Update parent `Document` aggregate root via domain method `document.applyNewVersion(nextVersion, storageKey, checksumSha256, fileSizeBytes, contentType, originalFileName)`:
          - Sets `currentVersion = nextVersion`, updates active storage pointers, sets `status = UPLOADED`, updates `updatedAt = now`.
        - If database transaction fails, execute S3 compensation delete on `documents/{docId}/v{nextVersion}/{sanitizedFileName}`.
      - Events & Response:
        - Emit `DocumentVersionCreatedEvent`.
        - Return `DocumentVersionResponseDto` with HTTP 200 OK.
    </approach>
    <advantages>
      - Exactly fulfills UC-DOC-02 Post-Condition 2 ("documents.current_version points to the new version") and Step 6.
      - O(1) read performance: Readers fetching active document metadata never need to run `MAX(version_number)` or join `document_versions`.
      - Strong DDD encapsulation: Aggregate root guarantees that `documents` active pointers and `currentVersion` are always in sync.
      - Clean rollback semantics with immediate S3 compensation cleanup.
    </advantages>
    <disadvantages>
      - Requires adding `current_version` (INT) to `documents` table and updating `DocumentJpaEntity` and `Document.java`.
    </disadvantages>
    <complexity>LOW</complexity>
    <compatibility>Fully backwards-compatible with existing UC-DOC-01 initial upload (where version defaults to 1)</compatibility>
    <concurrency_transaction_risk>LOW (standard database row-level locking on Document record during @Transactional update)</concurrency_transaction_risk>
    <testability>HIGH (straightforward to verify both DocumentVersion creation and Document update)</testability>
    <maintainability>EXCELLENT (clear domain responsibilities and predictable queries)</maintainability>
  </option>

  <option id="B">
    <approach>
      Dynamic Version Number Resolution via `document_versions` Aggregation Query:
      - Keep `Document` schema without `currentVersion`.
      - When uploading revision v2+:
        - Query `document_versions` for `SELECT COALESCE(MAX(version_number), 1) + 1 FROM document_versions WHERE document_id = ?`.
        - Upload to S3 under `documents/{docId}/v{nextVersion}/{sanitizedFileName}`.
        - In `@Transactional`:
          - Insert `DocumentVersion` snapshot.
          - Update `Document` active storage pointers (`storageKey`, `checksumSha256`, `fileSizeBytes`, etc.) without updating any version field on `documents`.
    </approach>
    <advantages>
      - Avoids adding `current_version` column to `documents` table.
    </advantages>
    <disadvantages>
      - Violates UC-DOC-02 Post-condition 2: "`documents.current_version` points to the new version" and Step 6.
      - Higher query latency on reads: determining the current version requires an aggregate query on `document_versions`.
      - Vulnerable to race conditions if two revision uploads execute concurrently without explicit table locking.
    </disadvantages>
    <complexity>MEDIUM</complexity>
    <compatibility>Moderate (omits current_version from Document)</compatibility>
    <concurrency_transaction_risk>MEDIUM (gap between MAX query and INSERT can cause unique constraint violation)</concurrency_transaction_risk>
    <testability>MEDIUM</testability>
    <maintainability>FAIR (fragmented version state between tables)</maintainability>
  </option>

  <option id="C">
    <approach>
      Asynchronous Staged Version Transition:
      - Version upload creates a `DocumentVersion` with status `STAGED`.
      - Version is not made active on `Document` immediately.
      - An asynchronous worker processes the file (OCR, parsing, malware scan).
      - Once processing succeeds, a background task promotes `DocumentVersion` to `ACTIVE` and updates `documents.current_version`.
    </approach>
    <advantages>
      - Prevents readers from accessing newly uploaded revisions until fully indexed/validated.
    </advantages>
    <disadvantages>
      - Grossly violates YAGNI and over-engineers UC-DOC-02.
      - Conflicts with UC-DOC-02 Basic Path Step 6 & 9: synchronous HTTP 200 OK with new version details, and asynchronous re-indexing triggered *after* commit via `DocumentVersionCreatedEvent`.
    </disadvantages>
    <complexity>HIGH</complexity>
    <compatibility>LOW (alters synchronous API contract)</compatibility>
    <concurrency_transaction_risk>HIGH (multi-stage asynchronous state machine)</concurrency_transaction_risk>
    <testability>LOW (requires async worker test harnesses)</testability>
    <maintainability>POOR (unnecessary complexity for current scope)</maintainability>
  </option>

</options>

---

## 4. Trade-off Matrix

<tradeoff_matrix>

| Criterion | Option A (Explicit Pointer - Recommended) | Option B (Dynamic Query) | Option C (Async Staged) |
|---|:---:|:---:|:---:|
| Spec Adherence (UC-DOC-02) | 5 | 3 | 2 |
| Architectural Cleanliness (DDD) | 5 | 3 | 2 |
| Query Performance (O(1) Read) | 5 | 2 | 4 |
| Concurrency Safety | 5 | 3 | 3 |
| Implementation Complexity | 4 | 3 | 1 |
| Testability | 5 | 4 | 2 |
| **Overall Score** | **29 / 30** | **18 / 30** | **14 / 30** |

<!-- Score: 1 (poor) → 5 (excellent) -->
</tradeoff_matrix>

---

## 5. Recommendation

<recommendation>
  Adopt **Option A: Explicit `currentVersion` Pointer on `Document` with Dual-Write Atomic Transaction**:
  1. **Matches Exact Business Specification:** Fully satisfies UC-DOC-02 post-condition 2 and basic path step 6 (`documents.current_version` updated to `next_version`).
  2. **High Performance & Simplicity:** Fetching active document metadata remains an $O(1)$ single-row lookup without extra aggregations or joins.
  3. **Transactional Safety:** Coordinating the S3 upload followed by an atomic `@Transactional` database commit (with automatic S3 compensation delete on error) guarantees zero orphaned artifacts and ACID compliance (NF1).
  4. **Clean Domain Model:** Encapsulates version transition logic inside the `Document` aggregate root (`document.applyNewVersion(...)`), keeping the domain model expressive and strictly decoupled from infrastructure concerns.
</recommendation>

---

## 6. Implementation Decision

<!-- PAIR mode: Completed by engineer before Gate 1 passes.
     DELEGATED / Fast-Track mode: Agent automatically populates Recommendation
     into <selected_option>, documents rationale, signs Gate 1 with [AUTO: DELEGATED], and proceeds. -->
<engineer_decision>
  <selected_option>Option A</selected_option>
  <rationale>Option A directly satisfies UC-DOC-02 post-conditions and basic path step 6 with O(1) read performance, atomic dual-write transaction consistency, and DDD domain encapsulation.</rationale>
  <rejected_alternatives>
    - Option B: Dynamic querying of MAX(version_number) adds read latency, is vulnerable to race conditions, and violates UC-DOC-02 requirement to update documents.current_version.
    - Option C: Asynchronous staged transitions over-engineer the synchronous UC-DOC-02 workflow and violate YAGNI.
  </rejected_alternatives>
</engineer_decision>

---

## 7. Constraints Created by This Decision

<constraints_created>
  - `Document.java` must include `currentVersion` (int, default 1) and a domain method `applyNewVersion(...)`.
  - `DocumentJpaEntity.java` and Liquibase `003-create-document-tables.yaml` (plus DBML schemas) must include `current_version INT NOT NULL DEFAULT 1`.
  - Storage key for revisions must follow `documents/{docId}/v{versionNumber}/{originalFileName}`.
  - S3 upload must occur before DB commit; on DB failure, S3 compensation delete must be executed.
  - Emission of `DocumentVersionCreatedEvent` occurs after successful DB commit.
  - User authorization check verifies: current authenticated user is document uploader (`document.getUploadedByUserId().equals(currentUserId)`) OR user has `ROLE_ADMIN`. If unauthorized, throw `DocumentAccessDeniedException` -> HTTP 403.
</constraints_created>

---

## 8. Evidence Still Required

<evidence_required>
  - Verify that adding `current_version` column to `003-create-document-tables.yaml` and `DocumentJpaEntity` causes zero regression across existing tests.
  - Verify that mock multipart upload for versioning properly captures the file name and content type.
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
