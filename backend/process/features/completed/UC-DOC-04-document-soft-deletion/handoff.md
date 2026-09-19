# Handoff: UC-DOC-04 Document Soft Deletion

<handoff task_id="UC-DOC-04" version="2.0" framework="RIPER-5">

<handoff_status>
  <review_decision>PASS</review_decision>
  <review_artifact>`process/features/completed/UC-DOC-04-document-soft-deletion/review.md`</review_artifact>
  <completed_date>2026-09-19</completed_date>
</handoff_status>

---

## 1. What Changed

<what_changed>
  Implemented UC-DOC-04: Document Soft Deletion. Authorized document owners, administrators, or users with `delete:documents` permission can soft-delete a document via `DELETE /api/v1/documents/{id}`. The backend marks `deleted_at = CURRENT_TIMESTAMP` in PostgreSQL, excludes it from subsequent queries and RAG retrieval pipelines, preserves physical S3 files and database rows for compliance retention, and emits `DocumentSoftDeletedEvent` for immutable audit logging (`UC-AUDIT-01`).
</what_changed>

<main_changes>
  - `src/main/resources/db/changelog/changes/003-create-document-tables.yaml` — Added `deleted_at TIMESTAMPTZ` and index `idx_documents_deleted_at`.
  - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/entity/DocumentJpaEntity.java` — Mapped `deletedAt` column.
  - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/repository/SpringDataDocumentRepository.java` — Added `findByIdAndDeletedAtIsNull` and `softDeleteById` update query.
  - `src/main/java/com/platform/app/document/application/ports/outbound/DocumentRepositoryPort.java` — Enforced soft-delete exclusion on `findById` and added `boolean softDelete(UUID id, Instant deletedAt)`.
  - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentRepositoryAdapter.java` — Implemented soft deletion persistence.
  - `src/main/java/com/platform/app/document/application/dto/SoftDeleteDocumentCommand.java` — Inbound command DTO.
  - `src/main/java/com/platform/app/document/application/event/DocumentSoftDeletedEvent.java` — Domain event for `UC-AUDIT-01` (`DELETE_DOC`).
  - `src/main/java/com/platform/app/document/application/ports/inbound/SoftDeleteDocumentUseCase.java` — Use case interface.
  - `src/main/java/com/platform/app/document/application/services/DocumentSoftDeleteService.java` — Orchestration service enforcing authorization, atomic update, and event dispatch.
  - `src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentController.java` — Added `DELETE /api/v1/documents/{id}` returning HTTP 204 No Content.
  - Comprehensive automated tests added in `DocumentSoftDeleteServiceTest`, `DocumentControllerTest`, and `DocumentRepositoryAdapterTest`.
</main_changes>

---

## 2. Why

<why>
  Soft deletion enables immediate removal of sensitive or obsolete documents from standard listings and vector search retrieval pipelines without losing audit integrity or violating legal compliance retention periods.
</why>

---

## 3. What Proves It

<evidence>

| AC | Verifier | Result |
|---|---|---|
| AC-1 | `./gradlew test` | PASS |
| AC-2 | `git diff Document.java` (empty) | PASS |
| AC-3 | `DocumentRepositoryAdapterTest` | PASS |
| AC-4 | `DocumentControllerTest.shouldReturnNoContentWhenOwnerDeletesDocument` | PASS |
| AC-5 | `DocumentControllerTest.shouldReturnNotFoundWhenDeletingAlreadySoftDeletedDocument` | PASS |
| AC-6 | `DocumentControllerTest.shouldReturnForbiddenWhenUnauthorizedUserDeletesDocument` | PASS |
| AC-7 | `DocumentControllerTest.shouldRejectUnauthenticatedDeleteRequest` | PASS |
| AC-8 | `DocumentSoftDeleteServiceTest.shouldSoftDeleteDocument_whenCallerIsOwner` | PASS |
| AC-9 | `DocumentRepositoryAdapterTest.shouldReturnEmptyWhenDocumentIsSoftDeletedOrNotFound` | PASS |
| AC-10 | `./gradlew check` | PASS |

```bash
./gradlew check
./gradlew test
```

</evidence>

---

## 4. What Remains Risky

<residual_risk>
  - None. Invariants B1, B2, NF1 are enforced and verified through automated tests.
</residual_risk>

---

## 5. Decisions & Assumptions

<decisions_and_assumptions>
  - Modified `003-create-document-tables.yaml` directly per user instruction instead of adding a new migration 005.
  - Kept `Document.java` clean without `deletedAt` field per user instruction, treating soft deletion as a persistence/repository lifecycle concern.
</decisions_and_assumptions>

---

## 6. Next Action

<next_action>
  NONE. Feature is complete.
</next_action>

</handoff>
