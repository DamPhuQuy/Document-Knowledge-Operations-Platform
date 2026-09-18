# Handoff: UC-DOC-02 Manage Document Versioning

<handoff task_id="UC-DOC-02" version="2.0" framework="RIPER-5">

<!-- Final projection. Short. Do not duplicate research/plan/review artifacts. -->
<!-- Answer: What changed? Why? What proves it? What remains risky? -->

<handoff_status>
  <review_decision>PASS</review_decision>  <!-- Must be PASS before handoff is valid -->
  <review_artifact>process/features/active/UC-DOC-02-manage-document-versioning/review.md</review_artifact>
  <completed_date>2026-09-16</completed_date>
</handoff_status>

---

## 1. What Changed

<what_changed>
  Implemented extension use case UC-DOC-02: Manage Document Versioning, allowing document owners and administrators to upload replacement revisions (`POST /api/v1/documents/{id}/versions`). Revisions are stream-uploaded to S3 under `documents/{doc_id}/v{version}/{filename}`, immutable snapshots are created in `document_versions`, active pointers and `current_version` are updated atomically on `documents`, domain event `DocumentVersionCreatedEvent` is published for AI re-indexing, and unauthorized users are rejected with HTTP 403 Forbidden.
</what_changed>

<main_changes>
  - `src/main/java/com/platform/app/document/domain/model/Document.java` — Added `currentVersion` (int, default 1) and domain method `applyNewVersion(...)`.
  - `src/main/java/com/platform/app/document/domain/exception/DocumentNotFoundException.java` — Mapped to HTTP 404 in RestExceptionHandler.
  - `src/main/java/com/platform/app/document/domain/exception/DocumentAccessDeniedException.java` — Mapped to HTTP 403 in RestExceptionHandler.
  - `src/main/java/com/platform/app/document/application/ports/inbound/UploadDocumentVersionUseCase.java` — Inbound use case contract.
  - `src/main/java/com/platform/app/document/application/ports/inbound/StoreVersionMetadataUseCase.java` — Transactional SPI for atomic dual-write.
  - `src/main/java/com/platform/app/document/application/dto/UploadDocumentVersionCommand.java` — Command object.
  - `src/main/java/com/platform/app/document/application/dto/DocumentVersionResponseDto.java` — Response DTO returned on HTTP 200 OK.
  - `src/main/java/com/platform/app/document/application/event/DocumentVersionCreatedEvent.java` — Domain event for downstream re-indexing and auditing.
  - `src/main/java/com/platform/app/document/application/services/DocumentVersionService.java` — Orchestrator for validation, ownership check, streaming S3 upload, compensation rollback, and event publication.
  - `src/main/java/com/platform/app/document/application/services/DocumentMetadataService.java` — Transactional persistence of version snapshot and document pointer update.
  - `src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentController.java` — Added endpoint `POST /api/v1/documents/{id}/versions`.
  - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java` — Added 404 and 403 exception handlers.
  - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/entity/DocumentJpaEntity.java` — Added `current_version` column.
  - `src/main/resources/db/changelog/changes/003-create-document-tables.yaml` — Added `current_version INT NOT NULL DEFAULT 1` to `documents` table.
  - `../docs/specs/database/schema.dbml` & `../docs/specs/database/modules/02_document_management.dbml` — Updated schema specs.
</main_changes>

---

## 2. Why

<why>
  UC-DOC-02 enables Knowledge Workers and Managers to iterate on existing documents without breaking document identity, providing full historical version tracking in `document_versions` while keeping the active pointer on `documents` up to date. S3 compensation delete ensures that storage artifacts are not leaked if the database transaction fails.
</why>

---

## 3. What Proves It

<evidence>

| AC | Verifier | Result |
|---|---|---|
| AC-1 | `./gradlew test --tests "*DocumentControllerTest.shouldUploadNewVersionSuccessfully*"` | PASS |
| AC-2 | `./gradlew test --tests "*DocumentVersionServiceTest.shouldUploadNewVersionSuccessfullyWhenOwner*"` | PASS |
| AC-3 | `./gradlew test --tests "*DocumentRepositoryAdapterTest*"` | PASS |
| AC-4 | `./gradlew test --tests "*DocumentControllerTest.shouldRejectVersionUploadWhenNotOwnerAndNotAdmin*"` | PASS |
| AC-5 | `./gradlew test --tests "*DocumentControllerTest.shouldReturn404WhenDocumentNotFound*"` | PASS |
| AC-6 | `./gradlew test --tests "*DocumentControllerTest.shouldRejectInvalidExtensionOnVersionUpload*"` | PASS |
| AC-7 | `./gradlew test --tests "*DocumentVersionServiceTest*"` | PASS |
| AC-8 | `./gradlew check` | PASS (136+ tests) |

```bash
./gradlew check
```

</evidence>

---

## 4. What Remains Risky

<residual_risk>
  - Dynamic ACL grants (UC-DOC-03) are not yet implemented; ownership is currently enforced by comparing `document.uploadedByUserId` against the authenticated JWT principal or verifying `ROLE_ADMIN`. When UC-DOC-03 is implemented, the ACL check can be expanded to inspect `document_user_access` for `EDIT` permissions.
</residual_risk>

---

## 5. Decisions & Assumptions

<decisions_and_assumptions>
  - DEC-DOC-03: Option A adopted. Explicit `currentVersion` attribute on `Document` aggregate root and `documents` table guarantees O(1) reads for readers without expensive aggregation queries. Dual-write in `@Transactional` method guarantees ACID consistency.
</decisions_and_assumptions>

---

## 6. Next Action

<next_action>
  NONE (Feature complete and verified).
</next_action>

</handoff>
