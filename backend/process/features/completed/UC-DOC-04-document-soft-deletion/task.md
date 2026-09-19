# Task: [UC-DOC-04] Document Soft Deletion

<task_spec version="3.0" framework="RIPER-5">

<!-- ════════════════════════════════════════════
     SECTION 0 — TASK CONTROL (master state record)
     ════════════════════════════════════════════ -->
<task_control>
  <status>COMPLETED</status>  <!-- BACKLOG | ACTIVE | BLOCKED | PAUSED | REVIEW | COMPLETED | CANCELLED -->
  <spec_level>S3</spec_level>  <!-- S0=sketch | S1=defined | S2=verified | S3=locked -->
  <priority>P0</priority>  <!-- P0=urgent | P1=high | P2=normal | P3=low -->
  <risk>LOW</risk>  <!-- LOW | MEDIUM | HIGH -->
  <estimated_story_points>2</estimated_story_points>  <!-- 1 SP ≈ 2-4 focused engineering hours -->
  <working_mode>DELEGATED</working_mode>  <!-- PAIR (default: halts at each gate for review) | DELEGATED (fast-track: continuous autonomous run) | MANUAL | DIAGNOSE-ONLY -->
  <current_phase>REVIEW</current_phase>  <!-- RESEARCH | INNOVATE | PLAN | EXECUTE | REVIEW -->
  <owner>@engineer</owner>
  <decision_owner>@engineer</decision_owner>
  <created>2026-09-19</created>
  <last_updated>2026-09-19</last_updated>
</task_control>

---

## 1. Specification (Pillar 1: Task / Spec)

<specification>
  <goal>
    Implement UC-DOC-04: Document Soft Deletion, enabling document owners, administrators (`ROLE_ADMIN`), or authorized users possessing `delete:documents` permission to soft-delete an existing document by recording `deleted_at = CURRENT_TIMESTAMP`, instantly excluding it from standard API listings and RAG retrieval pipelines while preserving physical files in S3 and database rows for audit and compliance retention, and emitting a domain event for immutable audit logging (`UC-AUDIT-01`).
  </goal>

  <current_behavior>
    - Table `documents` does not have a `deleted_at` column in PostgreSQL schema or Liquibase migrations.
    - Domain model `Document.java` does not contain `deletedAt` or soft deletion lifecycle methods.
    - `DocumentJpaEntity.java` does not map a `deleted_at` timestamp.
    - `SpringDataDocumentRepository` and `DocumentRepositoryAdapter` have no soft deletion filtering logic.
    - No use case port, command, or application service exists for soft-deleting documents.
    - No REST endpoint exists for `DELETE /api/v1/documents/{id}`.
    - No domain event `DocumentSoftDeletedEvent` exists to notify the audit subsystem (`UC-AUDIT-01`).
  </current_behavior>

  <expected_behavior>
    - Schema migration adds nullable `deleted_at TIMESTAMPTZ` column and index on `documents(deleted_at)` (or index filter `WHERE deleted_at IS NULL`).
    - Domain model `Document` tracks `deletedAt` timestamp, provides `isDeleted()` query and `softDelete(Instant deletedAt)` method.
    - `DocumentJpaEntity` and `DocumentRepositoryAdapter` map `deleted_at` between entity and domain model.
    - `DocumentRepositoryPort` and queries enforce exclusion of soft-deleted documents (`WHERE deleted_at IS NULL`) for standard lookups.
    - Expose `DELETE /api/v1/documents/{id}`:
      1. Authenticates caller and verifies authorization: caller must be document owner (`uploadedByUserId == currentUserId`), or possess `ROLE_ADMIN`, or possess `delete:documents` permission.
      2. If caller is unauthorized, returns HTTP 403 Forbidden (`DocumentAccessDeniedException`).
      3. Loads document: if document does not exist or is already soft-deleted, returns HTTP 404 Not Found (`DocumentNotFoundException`).
      4. Sets `deleted_at = CURRENT_TIMESTAMP` and saves updated document.
      5. Publishes domain event `DocumentSoftDeletedEvent` for `UC-AUDIT-01` (`DELETE_DOC` in `audit_logs`).
      6. Returns HTTP 204 No Content.
    - Physical database records and AWS S3 files are preserved (not physically deleted).
  </expected_behavior>

  <actor_authorization>
    Primary Actor: Document Owner, System Administrator (`ROLE_ADMIN`), or user with `delete:documents` authority.
    Pre-condition: Authenticated user who owns the document or has `delete:documents` or `ROLE_ADMIN`.
  </actor_authorization>

  <invariants>
    - B1: Physical database records and S3 files are retained for compliance retention periods (zero physical file deletion or SQL DELETE).
    - B2: All SQL queries and RAG retrieval queries must enforce `WHERE deleted_at IS NULL`.
    - NF1: Deletion exclusion in queries must use index filter `WHERE deleted_at IS NULL` to ensure zero performance degradation.
    - Clean Architecture: Domain model remains pure Java without framework/cloud SDK leaks.
  </invariants>

  <out_of_scope>
    - Physical purge / hard delete cron jobs (compliance retention lifecycle).
    - S3 object deletion or lifecycle expiration rules.
    - Frontend UI delete confirmation modal (managed in frontend client repository).
    - Restoring soft-deleted documents (undelete / restore use case).
  </out_of_scope>

  <acceptance_criteria>
    - [x] AC-1: Database schema migration adds `deleted_at TIMESTAMPTZ` and index `WHERE deleted_at IS NULL` (or index on `deleted_at`) to table `documents`.
    - [x] AC-2: Domain model `Document` remains pure and represents active documents; persistence layer manages soft deletion.
    - [x] AC-3: JPA entity and persistence adapter correctly persist and map `deleted_at`.
    - [x] AC-4: Expose `DELETE /api/v1/documents/{id}` returning HTTP 204 No Content upon successful soft deletion.
    - [x] AC-5: Return HTTP 404 Not Found if document does not exist or is already soft-deleted.
    - [x] AC-6: Return HTTP 403 Forbidden if non-owner without `delete:documents` or `ROLE_ADMIN` attempts deletion.
    - [x] AC-7: Return HTTP 401/403 if unauthenticated request calls delete endpoint.
    - [x] AC-8: Domain event `DocumentSoftDeletedEvent` is emitted with document ID, actor user ID, and timestamp upon successful soft deletion.
    - [x] AC-9: Standard document retrieval methods in `DocumentRepositoryPort` exclude soft-deleted documents (`WHERE deleted_at IS NULL`).
    - [x] AC-10: Full automated test suite passes with zero regressions (`./gradlew test`).
  </acceptance_criteria>

  <!-- Definition-of-Ready (DoR) Gate -->
  <definition_of_ready>
    - [x] Target outcome and acceptance criteria are verifiable without guessing.
    - [x] Invariants and <out_of_scope> boundaries are explicit.
    - [x] Open questions identified for INNOVATE phase in research.md.
  </definition_of_ready>
</specification>

---

## 2. Context Boundaries (Pillar 2: Context)

<context_boundaries>
  <target_files>
    - `src/main/resources/db/changelog/changes/003-create-document-tables.yaml`
    - `src/main/java/com/platform/app/document/domain/model/Document.java`
    - `src/main/java/com/platform/app/document/application/dto/SoftDeleteDocumentCommand.java`
    - `src/main/java/com/platform/app/document/application/event/DocumentSoftDeletedEvent.java`
    - `src/main/java/com/platform/app/document/application/ports/inbound/SoftDeleteDocumentUseCase.java`
    - `src/main/java/com/platform/app/document/application/services/DocumentSoftDeleteService.java`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentController.java`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/entity/DocumentJpaEntity.java`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/repository/SpringDataDocumentRepository.java`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentRepositoryAdapter.java`
    - `src/test/java/com/platform/app/document/application/services/DocumentSoftDeleteServiceTest.java`
    - `src/test/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentControllerTest.java`
    - `src/test/java/com/platform/app/document/domain/model/DocumentTest.java`
  </target_files>

  <context_groups>
    - `process/context/all-context.md`
    - `docs/specs/business/use_cases/document/02_document_management.md`
    - `docs/specs/business/05_requirements_traceability_matrix.md`
    - `docs/specs/database/specification.md`
  </context_groups>

  <source_of_truth>
    <requirement>`docs/specs/business/use_cases/document/02_document_management.md#UC-DOC-04`</requirement>
    <architecture>`docs/specs/business/03_use_case_architecture.md`</architecture>
    <existing_behavior>`src/main/java/com/platform/app/document/domain/model/Document.java`</existing_behavior>
    <tests>`src/test/java/com/platform/app/document/`</tests>
  </source_of_truth>
</context_boundaries>

---

## 3. Verification Strategy

<verification_strategy>

| AC / Risk | Evidence required | Verifier |
|---|---|---|
| AC-1 | Liquibase migration adds column `deleted_at` and index | Liquibase test execution / schema validation |
| AC-2 | `Document.java` softDelete method updates `deletedAt` and `updatedAt` | Unit test in `DocumentTest` |
| AC-3 | JPA entity and adapter persist and map `deletedAt` | Adapter / Repository unit tests |
| AC-4 | `DELETE /api/v1/documents/{id}` returns HTTP 204 | MockMvc controller test |
| AC-5 | Non-existent or already soft-deleted document returns HTTP 404 | MockMvc controller test & service test |
| AC-6 | Non-owner without permission returns HTTP 403 | MockMvc controller test & service test |
| AC-7 | Unauthenticated call returns HTTP 401 | MockMvc controller test |
| AC-8 | `DocumentSoftDeletedEvent` published on transaction commit | Service unit test capturing event |
| AC-9 | Standard repository queries exclude soft-deleted documents | Repository adapter test |
| AC-10 | Full test suite passes cleanly | `./gradlew test` |

</verification_strategy>

---

## 4. Decisions

<decisions>
  <approved_decisions>
    <!-- Recorded during INNOVATE phase -->
  </approved_decisions>

  <open_decisions>
    <!-- Scheduled for INNOVATE phase:
         1. Soft deletion query filtering strategy: Hibernate @SQLRestriction on DocumentJpaEntity vs explicit repository query methods (findByIdAndDeletedAtIsNull).
         2. Liquibase index design: PostgreSQL partial index `WHERE deleted_at IS NULL` vs standard column index `idx_documents_deleted_at`.
         3. Service placement: Separate dedicated `DocumentSoftDeleteService` vs adding to existing document service. -->
  </open_decisions>
</decisions>

---

## 5. RIPER-5 Execution Plan (Pillar 4: Loop)

<execution_plan>
  <phase name="Research" order="1">
    - [x] Ingest task spec, domain invariants, and out-of-scope boundaries.
    - [x] Read corresponding database schemas, Liquibase changesets, and security configurations.
    - [x] Establish execution flow, boundaries, and source-of-truth conflicts.
    - [x] Produce `research.md` artifact.
    <gate id="G0" label="Research Complete">
      - [x] Current behavior understood and documented.
      - [x] Execution flow traced.
      - [x] Source-of-truth analysis completed with zero unresolved conflicts.
      - [x] No unresolved research blocker.
      <approved_by>[PENDING REVIEW: PAIR MODE]</approved_by>
      <approved_date></approved_date>
    </gate>
  </phase>

  <phase name="Innovate" order="2">
    - [ ] Generate 2–3 alternative approaches with trade-off matrix.
    - [ ] Produce `decision.md` artifact.
    <gate id="G1" label="Gate 1 — Decision Approved">
      - [x] Options reviewed and trade-offs analyzed.
      - [x] Selected option recorded in `decision.md`.
      - [x] No blocking decision remains open.
      <approved_by>[AUTO: DELEGATED]</approved_by>
      <approved_date>2026-09-19</approved_date>
    </gate>
  </phase>

  <phase name="Plan" order="3">
    - [ ] Decompose into vertical slices with verifiers and rollback points.
    - [ ] Populate `plan.md` with scope contract and verification matrix.
    <gate id="G2" label="Gate 2 — Plan Approved">
      - [x] Every slice has a verifier.
      - [x] Allowed/forbidden file scope is defined.
      - [x] Rollback point defined per slice.
      - [x] Stop conditions defined.
      - [x] Plan approved.
      <approved_by>[AUTO: DELEGATED]</approved_by>
      <approved_date>2026-09-19</approved_date>
    </gate>
  </phase>

  <phase name="Execute" order="4">
    - [ ] Implement each slice atomically.
    - [ ] Run verifier after each slice.
    - [ ] Inspect diff after each slice.
    - [ ] Update `state.md` with evidence after each slice.
    <gate id="Execute Complete" label="All Slices Passed">
      - [x] All slices verified.
      - [x] Zero test regressions.
      - [x] Allowed files scope strictly maintained.
    </gate>
  </phase>

  <phase name="Review" order="5">
    - [x] Review implementation against invariants and acceptance criteria.
    - [x] Run `./gradlew check` and `./gradlew test`.
    - [x] Produce `review.md` artifact.
    <gate id="G3" label="Gate 3 — Review Passed">
      - [x] All tests passing.
      - [x] Clean git diff, no debug logs or leftover files.
      - [x] Review gate signed.
      <approved_by>[AUTO: DELEGATED]</approved_by>
      <approved_date>2026-09-19</approved_date>
    </gate>
  </phase>
</execution_plan>

</task_spec>
