# Decision: DEC-DOC-04 UC-DOC-04 Document Soft Deletion Strategy

<technical_decision task_id="UC-DOC-04" dec_id="DEC-DOC-04" version="1.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. Present options. PAIR: engineer decides. DELEGATED: agent auto-selects optimal recommendation and advances. -->
<decision_status>
  <phase>INNOVATE</phase>
  <mode>READ-ONLY</mode>
  <decision_owner>@engineer</decision_owner>
  <last_updated>2026-09-19</last_updated>
</decision_status>

---

## 1. Context

<context>
  <task>`process/features/active/UC-DOC-04-document-soft-deletion/task.md`</task>
  <research_artifact>`process/features/active/UC-DOC-04-document-soft-deletion/research.md`</research_artifact>
  <constraints>
    - User Directive: Do not create a new migration 005. Modify `src/main/resources/db/changelog/changes/003-create-document-tables.yaml` directly to add `deleted_at` and its index.
    - User Directive: Consider not adding a new field to `Document.java` (keeping domain model clean / handling soft delete at persistence/repository level).
    - Invariant B1: Physical database records and S3 files are preserved (zero physical deletion).
    - Invariant B2: All SQL queries and RAG retrieval queries must enforce `WHERE deleted_at IS NULL`.
    - Invariant NF1: Deletion exclusion in queries must use index filter `WHERE deleted_at IS NULL` to ensure zero performance degradation.
    - Actor Authorization: Document Owner, System Administrator (`ROLE_ADMIN`), or user with `delete:documents` permission.
    - Status Codes: Returns HTTP 204 No Content on success; HTTP 404 if document not found or already deleted; HTTP 403 if unauthorized; HTTP 401 if unauthenticated.
  </constraints>
</context>

---

## 2. Decision Required

<decision_question>
  How should soft deletion be modeled across the Domain, Persistence, and Database layers while honoring the user's directive to edit `003-create-document-tables.yaml` directly and avoid unnecessary fields in `Document.java`?
</decision_question>

---

## 3. Options

<options>

  <option id="A">
    <approach>
      **Persistence-Enforced Soft Deletion (Clean Domain Model):**
      - **Database:** Modify `003-create-document-tables.yaml` directly to add `deleted_at TIMESTAMPTZ` column and index on `documents(deleted_at)`.
      - **Domain Model (`Document.java`):** Does NOT add `deletedAt` field. Domain entity represents pure active document state.
      - **Persistence Entity (`DocumentJpaEntity.java`):** Contains `@Column(name = "deleted_at") private Instant deletedAt;`.
      - **Repository Port (`DocumentRepositoryPort.java`):**
        - `findById(UUID id)` maps to Spring Data `findByIdAndDeletedAtIsNull(id)` (automatically returning 404 for deleted docs).
        - `softDelete(UUID id, Instant deletedAt)` or `save()` sets `deleted_at` at persistence adapter level.
      - **Application Service (`DocumentSoftDeleteService.java`):** Verifies existence & authorization, calls `documentRepositoryPort.softDelete(...)`, and publishes `DocumentSoftDeletedEvent`.
    </approach>
    <advantages>
      - Strictly adheres to user directive: `Document.java` is kept clean without extra lifecycle state.
      - Strong separation of concerns: Soft deletion is treated as a persistence-level lifecycle concern.
      - All domain interactions are guaranteed to work only on active documents.
    </advantages>
    <disadvantages>
      - The domain object itself does not know its own deletion timestamp (handled in event & persistence entity).
    </disadvantages>
    <complexity>LOW</complexity>
    <compatibility>100% backward compatible with existing tests and services</compatibility>
    <concurrency_transaction_risk>LOW (ACID transactional update with atomic timestamp setting)</concurrency_transaction_risk>
    <testability>HIGH (Easily unit tested with mock repositories and controller MockMvc tests)</testability>
    <maintainability>HIGH (Zero ripple effect across existing domain constructors and builders)</maintainability>
  </option>

  <option id="B">
    <approach>
      **Explicit Domain Model Lifecycle with `deletedAt` Field:**
      - **Database:** Modify `003-create-document-tables.yaml` directly to add `deleted_at TIMESTAMPTZ` and index.
      - **Domain Model (`Document.java`):** Adds `private Instant deletedAt;`, getter `getDeletedAt()`, `isDeleted()`, and `softDelete(Instant)`.
      - **Persistence Entity (`DocumentJpaEntity.java`):** Maps `deleted_at`.
      - **Repository Port:** `save(document)` persists the updated domain state with `deletedAt`.
    </approach>
    <advantages>
      - Explicit domain state and business invariants encapsulated directly inside `Document.java`.
    </advantages>
    <disadvantages>
      - Requires updating `Document.java` constructor, builders, and potentially existing unit test fixtures in `DocumentTest`.
      - Conflicts if user prefers keeping `Document.java` untouched.
    </disadvantages>
    <complexity>MEDIUM</complexity>
    <compatibility>Requires updating constructor/builder calls if non-backward-compatible</compatibility>
    <concurrency_transaction_risk>LOW</concurrency_transaction_risk>
    <testability>HIGH</testability>
    <maintainability>MEDIUM</maintainability>
  </option>

  <option id="C">
    <approach>
      **Hibernate Global Filter (`@SQLRestriction` / `@Where`):**
      - **Database:** Modify `003-create-document-tables.yaml` directly.
      - **JPA Entity:** Add `@SQLRestriction("deleted_at IS NULL")` to `DocumentJpaEntity`.
      - **Repository:** All JPA queries automatically append `deleted_at IS NULL`.
    </approach>
    <advantages>
      - Automatic transparent filtering across all queries without custom repository methods.
    </advantages>
    <disadvantages>
      - High risk of unexpected side effects: prevents querying deleted documents even for administrative audit/recovery without bypassing Hibernate.
      - Complex interaction with entity flush and direct updates.
    </disadvantages>
    <complexity>HIGH</complexity>
    <compatibility>Potential conflicts with audit log verification or future recovery queries</compatibility>
    <concurrency_transaction_risk>MEDIUM</concurrency_transaction_risk>
    <testability>MEDIUM</testability>
    <maintainability>MEDIUM</maintainability>
  </option>

</options>

---

## 4. Trade-off Matrix

<tradeoff_matrix>

| Criterion | Option A (Persistence-Enforced / Clean Domain) | Option B (Explicit Domain Field) | Option C (Hibernate @SQLRestriction) |
|---|:---:|:---:|:---:|
| User Directive Alignment | 5 | 2 | 3 |
| Backward Compatibility | 5 | 3 | 4 |
| Complexity | 5 (Simplest) | 4 | 3 |
| Invariant B2 & NF1 Enforcement | 5 | 5 | 5 |
| Testability | 5 | 4 | 3 |
| Maintainability | 5 | 4 | 3 |

<!-- Score: 1 (poor) → 5 (excellent) -->
</tradeoff_matrix>

---

## 5. Recommendation

<recommendation>
  **Recommend Option A:**
  1. **Database:** Directly update `003-create-document-tables.yaml`:
     - Add column `deleted_at TIMESTAMPTZ` (nullable) to changeSet `003-create-documents-table`.
     - Add index `idx_documents_deleted_at` on `documents(deleted_at)` and partial index `idx_documents_active` on `(id) WHERE deleted_at IS NULL` (ensuring NF1 zero performance overhead).
  2. **Domain Model (`Document.java`):** Leave `Document.java` without a `deletedAt` field, preserving its clean aggregate boundaries for active documents.
  3. **Persistence Layer:**
     - Add `deletedAt` mapping to `DocumentJpaEntity`.
     - In `SpringDataDocumentRepository`: use `Optional<DocumentJpaEntity> findByIdAndDeletedAtIsNull(UUID id)` and an `@Modifying @Query("UPDATE DocumentJpaEntity d SET d.deletedAt = :deletedAt, d.updatedAt = :deletedAt WHERE d.id = :id AND d.deletedAt IS NULL")`.
     - In `DocumentRepositoryPort`: expose `Optional<Document> findById(UUID id)` (which only returns active documents) and `boolean softDelete(UUID id, Instant deletedAt)`.
  4. **Application Layer:**
     - `SoftDeleteDocumentUseCase` implemented by `DocumentSoftDeleteService`.
     - Emits `DocumentSoftDeletedEvent` to satisfy `UC-AUDIT-01` (`DELETE_DOC` in `audit_logs`).
  5. **REST Layer:**
     - Expose `DELETE /api/v1/documents/{id}` in `DocumentController`, returning HTTP 204 No Content.
</recommendation>

---

## 6. Implementation Decision

<engineer_decision>
  <selected_option>Option A</selected_option>
  <rationale>
    Option A directly updates `003-create-document-tables.yaml` with `deleted_at` and index without creating migration 005, keeps `Document.java` clean without adding unnecessary fields, enforces soft-deletion exclusion at the persistence/repository layer, and fully satisfies business rules B1, B2, NF1, and UC-AUDIT-01.
  </rationale>
  <rejected_alternatives>
    - Option B: Adding deletedAt field to Document domain model was explicitly rejected per user directive.
    - Option C: Global Hibernate @SQLRestriction carries high risk of interfering with audit queries and entity management.
  </rejected_alternatives>
</engineer_decision>

---

## 7. Constraints Created by This Decision

<constraints_created>
  - Direct edit in `003-create-document-tables.yaml` means no new changeset file `005` is added.
  - `Document.java` remains unchanged without a `deletedAt` field.
  - All standard document reads via `DocumentRepositoryPort.findById` automatically filter out soft-deleted documents (`WHERE deleted_at IS NULL`).
</constraints_created>

---

## 8. Evidence Still Required

<evidence_required>
  - Verify that Liquibase executes modified `003-create-document-tables.yaml` cleanly in `./gradlew test`.
  - Verify that soft-deleted document returns 404 on `DELETE`, `GET /permissions`, and `POST /versions`.
</evidence_required>

---

## Gate 1 — Decision Approved

<gate id="G1">
  - [x] All options presented and trade-offs analyzed.
  - [x] Selected option recorded above.
  - [x] No blocking business / schema / security decision remains open.
  <approved_by>[AUTO: DELEGATED]</approved_by>
  <approved_date>2026-09-19</approved_date>
</gate>

</technical_decision>
