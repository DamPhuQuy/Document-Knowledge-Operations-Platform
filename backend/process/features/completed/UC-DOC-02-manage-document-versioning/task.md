# Task: [UC-DOC-02] Manage Document Versioning

<task_spec version="3.0" framework="RIPER-5">

<!-- ════════════════════════════════════════════
     SECTION 0 — TASK CONTROL (master state record)
     ════════════════════════════════════════════ -->
<task_control>
  <status>COMPLETED</status>  <!-- BACKLOG | ACTIVE | BLOCKED | PAUSED | REVIEW | COMPLETED | CANCELLED -->
  <spec_level>S3</spec_level>  <!-- S0=sketch | S1=defined | S2=verified | S3=locked -->
  <priority>P1</priority>  <!-- P0=urgent | P1=high | P2=normal | P3=low -->
  <risk>MEDIUM</risk>  <!-- LOW | MEDIUM | HIGH -->
  <estimated_story_points>3</estimated_story_points>  <!-- 1 SP ≈ 2-4 focused engineering hours -->
  <working_mode>DELEGATED</working_mode>  <!-- PAIR (default: halts at each gate for review) | DELEGATED (fast-track: continuous autonomous run) | MANUAL | DIAGNOSE-ONLY -->
  <current_phase>REVIEW</current_phase>  <!-- RESEARCH | INNOVATE | PLAN | EXECUTE | REVIEW -->
  <owner>@engineer</owner>
  <decision_owner>@engineer</decision_owner>
  <created>2026-09-16</created>
  <last_updated>2026-09-16</last_updated>
</task_control>

---

## 1. Specification (Pillar 1: Task / Spec)

<specification>
  <goal>
    Implement UC-DOC-02: Manage Document Versioning, allowing document authors and managers with `write:documents` permission and document ownership (or EDIT ACL) to upload updated revisions of an existing document, creating immutable historical snapshots in `document_versions` while updating the active pointer and metadata in `documents`, dispatching `DocumentVersionCreatedEvent` for re-indexing, and invoking UC-AUDIT-01.
  </goal>

  <current_behavior>
    - UC-DOC-01 is implemented (`POST /api/v1/documents`), which uploads an initial document and persists it in `documents`.
    - `DocumentVersion` domain model and `DocumentVersionJpaEntity` exist and were streamlined to 8 core fields: `id`, `documentId`, `versionNumber`, `storageKey`, `fileSizeBytes`, `checksumSha256`, `changeSummary`, `uploadedByUserId`, `createdAt`.
    - No endpoint exists for uploading replacement revisions to an existing document (`POST /api/v1/documents/{id}/versions`).
    - No domain service or use case exists for document versioning orchestration (`UploadDocumentVersionUseCase`).
    - No event `DocumentVersionCreatedEvent` exists.
  </current_behavior>

  <expected_behavior>
    - Expose endpoint `POST /api/v1/documents/{id}/versions` accepting `multipart/form-data` with `file` part and optional/required `changeSummary`.
    - Verifies user has `write:documents` authority and is the owner of the document (`uploadedByUserId == currentUserId`) or has administrator privileges.
    - Validates parent document exists in `documents` (returns HTTP 404 if not found).
    - If user lacks edit permissions on the document, returns HTTP 403 Forbidden.
    - Validates file type extension (PDF, DOCX, TXT, XLSX) and size ($\le 50\text{ MB}$).
    - Uploads file stream to S3 Object Storage (`EXT-01`) under key `documents/{doc_id}/v{next_version}/{file_name}` computing streaming SHA-256 integrity checksum.
    - In an ACID transaction:
      1. Creates new record in `document_versions` with `version_number = current_version + 1`.
      2. Updates `documents` active metadata: storage key, file size, checksum SHA-256, status (e.g. `UPLOADED` / `PENDING`), and `updatedAt`.
    - Historical version records remain intact and immutable.
    - Dispatches domain event `DocumentVersionCreatedEvent` containing version snapshot metadata for downstream AI re-indexing.
    - Invokes `UC-AUDIT-01` logging.
    - Returns HTTP 200 OK with new version details.
  </expected_behavior>

  <actor_authorization>
    Primary Actor: Document Owner / Manager (`ROLE_STAFF` or `ROLE_ADMIN`).
    Authorization Pre-condition: User possesses `write:documents` authority and owns the document (`uploadedByUserId == currentUserId`) or has administrative privileges.
  </actor_authorization>

  <invariants>
    - B1: Historical versions in `document_versions` are immutable and cannot be overwritten.
    - B2: Chunks and embeddings are bound to specific `document_version_id` to prevent version mismatch.
    - Clean Architecture: Domain entities remain pure Java POJOs without framework/cloud SDK leaks.
    - NF1: Version transition must be ACID-compliant with zero downtime for readers.
    - S3 Compensation: If database transaction fails, uploaded S3 object is purged via compensation delete.
  </invariants>

  <out_of_scope>
    - UC-DOC-03: Dynamic Access Control Matrix / ACL grants configuration (department/user access table population).
    - AI & RAG Subsystem: Text parsing, chunking, OCR, and vector embeddings generation (triggered asynchronously via `DocumentVersionCreatedEvent`).
    - Presigned URL generation for downloading specific versions.
  </out_of_scope>

  <acceptance_criteria>
    - [x] AC-1: Endpoint `POST /api/v1/documents/{id}/versions` successfully accepts multipart file and change summary, incrementing version number to `current_version + 1`.
    - [x] AC-2: S3 storage key strictly follows `documents/{doc_id}/v{next_version}/{file_name}` with streaming SHA-256 verification.
    - [x] AC-3: New immutable snapshot is persisted in `document_versions`, and parent `documents` record is updated with active metadata in an ACID transaction.
    - [x] AC-4: Non-owner without admin rights uploading a new version receives HTTP 403 Forbidden.
    - [x] AC-5: Uploading version for non-existent document ID returns HTTP 404 Not Found.
    - [x] AC-6: Unsupported media type or oversized file returns HTTP 415 or HTTP 413 respectively.
    - [x] AC-7: Domain event `DocumentVersionCreatedEvent` is published upon successful commit.
    - [x] AC-8: Full unit and integration test suite passes with zero regressions (`./gradlew check`).
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
    - `src/main/java/com/platform/app/document/domain/model/Document.java`
    - `src/main/java/com/platform/app/document/domain/model/DocumentVersion.java`
    - `src/main/java/com/platform/app/document/domain/exception/DocumentNotFoundException.java`
    - `src/main/java/com/platform/app/document/domain/exception/DocumentAccessDeniedException.java`
    - `src/main/java/com/platform/app/document/application/ports/inbound/UploadDocumentVersionUseCase.java`
    - `src/main/java/com/platform/app/document/application/dto/UploadDocumentVersionCommand.java`
    - `src/main/java/com/platform/app/document/application/dto/DocumentVersionResponseDto.java`
    - `src/main/java/com/platform/app/document/application/event/DocumentVersionCreatedEvent.java`
    - `src/main/java/com/platform/app/document/application/services/DocumentVersionService.java`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentController.java`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentVersionRepositoryAdapter.java`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/repository/SpringDataDocumentVersionRepository.java`
    - `src/test/java/com/platform/app/document/**`
  </target_files>

  <context_groups>
    - `document-management`
    - `object-storage`
    - `audit-logging`
  </context_groups>

  <source_of_truth>
    <requirement>UC-DOC-02 Specification (Manage Document Versioning)</requirement>
    <architecture>Clean Hexagonal Architecture (`process/context/architecture/architecture-template.md`)</architecture>
    <existing_behavior>`src/test/java/com/platform/app/document/application/services/DocumentUploadServiceTest.java`</existing_behavior>
    <tests>`./gradlew test`</tests>
  </source_of_truth>
</context_boundaries>

---

## 3. Verification Strategy

<verification_strategy>

| AC / Risk | Evidence required | Verifier |
|---|---|---|
| AC-1 | Version uploaded, versionNumber = previous + 1, HTTP 200 OK | `./gradlew test --tests "*DocumentControllerTest*"` |
| AC-2 | S3 storage key matches format `documents/{doc_id}/v{v}/{name}` | `./gradlew test --tests "*DocumentVersionServiceTest*"` |
| AC-3 | `document_versions` row saved and `documents` updated atomically | `./gradlew test --tests "*DocumentRepositoryAdapterTest*"` |
| AC-4 | Non-owner non-admin receives HTTP 403 | `./gradlew test --tests "*DocumentControllerTest*"` |
| AC-5 | Non-existent docId receives HTTP 404 | `./gradlew test --tests "*DocumentControllerTest*"` |
| AC-6 | Invalid mime or >50MB rejected with 415 / 413 | `./gradlew test --tests "*DocumentControllerTest*"` |
| AC-7 | `DocumentVersionCreatedEvent` captured and verified | `./gradlew test --tests "*DocumentVersionServiceTest*"` |
| AC-8 | Spotless and full test suite pass cleanly | `./gradlew check` |

</verification_strategy>

</task_spec>
