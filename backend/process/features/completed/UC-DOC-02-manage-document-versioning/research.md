# Research: UC-DOC-02 Manage Document Versioning

<research_context task_id="UC-DOC-02" version="1.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. No source modifications. No implementation decisions. -->
<research_status>
  <phase>RESEARCH</phase>
  <mode>READ-ONLY</mode>
  <research_owner>@engineer</research_owner>
  <last_updated>2026-09-16</last_updated>
</research_status>

---

## 1. Current Behavior & Existing Implementation

<current_behavior>
  - **Document Bounded Context (`com.platform.app.document`):**
    - `Document.java` represents the document aggregate root with 11 core fields: `id`, `title`, `originalFileName`, `contentType`, `fileSizeBytes`, `checksumSha256`, `storageKey`, `status` (`DocumentStatus`), `uploadedByUserId`, `departmentId`, `accessLevel`, `createdAt`, `updatedAt`.
    - `DocumentVersion.java` represents immutable historical snapshots with 8 core fields: `id`, `documentId`, `versionNumber`, `storageKey`, `fileSizeBytes`, `checksumSha256`, `changeSummary`, `uploadedByUserId`, `createdAt`.
    - `DocumentRepositoryPort` provides `save(Document)` and `findById(UUID)`.
    - `DocumentVersionRepositoryPort` provides `save(DocumentVersion)` and `findByDocumentId(UUID)`.
    - `ObjectStoragePort` provides `upload(key, inputStream, size, contentType)`, `delete(key)`, `exists(key)`, and `getDownloadUrl(key, duration)`.
    - `DocumentController` currently only exposes `POST /api/v1/documents` for initial document upload (`UC-DOC-01`).
    - No endpoint or service exists for uploading new revisions to an existing document (`POST /api/v1/documents/{id}/versions`).
  - **Database Schema Status (`003-create-document-tables.yaml`):**
    - Table `documents` contains active document attributes. Currently, `current_version` was removed during `CHG-DOC-01` because version 2+ uploads were out of scope for `UC-DOC-01`.
    - Table `document_versions` has columns: `id`, `document_id`, `version_number`, `storage_key`, `file_size_bytes`, `checksum_sha256`, `change_summary`, `uploaded_by_user_id`, `created_at`, with a unique index on `(document_id, version_number)`.
  - **Security & Authorization Status:**
    - `DocumentController` checks `@PreAuthorize("hasAuthority('write:documents') or hasAuthority('WRITE:DOCUMENTS') or hasRole('ADMIN')")`.
    - However, resource-level ownership checking (`document.uploadedByUserId == currentUserId`) or ACL verification is not yet implemented for version uploads.
</current_behavior>

---

## 2. Execution Flows

<execution_flow>

### Flow 1: Upload New Version (Basic Path - UC-DOC-02)
```text
Document Owner / Manager (ROLE_STAFF with write:documents or ROLE_ADMIN)
  → POST /api/v1/documents/{id}/versions (multipart/form-data: file, changeSummary)
    → TraceIdFilter (injects traceId, X-Trace-Id)
    → Spring Security FilterChain (authenticates JWT, verifies write:documents authority)
    → DocumentController.uploadVersion(docId, file, changeSummary, authentication)
      ├── Validate file presence, non-empty, extension (.pdf, .docx, .txt, .xlsx), size (<= 50MB)
      ├── Extract current authenticated user UUID
      └── UploadDocumentVersionUseCase.uploadVersion(command)
          ├── 1. Fetch parent Document from DocumentRepositoryPort
          │      └── If not found -> throw DocumentNotFoundException (maps to HTTP 404)
          ├── 2. Verify authorization:
          │      └── If current user != document.uploadedByUserId AND user is not ADMIN:
          │             -> throw DocumentAccessDeniedException (maps to HTTP 403)
          ├── 3. Determine next version number:
          │      └── Option: nextVersion = current_version + 1 (or max(versionNumber) + 1)
          ├── 4. Generate S3 storage key:
          │      └── "documents/{docId}/v{nextVersion}/{sanitizedFileName}"
          ├── 5. Stream-upload to S3 with DigestInputStream (single-pass SHA-256 computation)
          │      └── If S3 upload fails -> throw StorageException (maps to HTTP 502)
          ├── 6. Atomically persist metadata in @Transactional boundary:
          │      ├── Insert new DocumentVersion record into document_versions
          │      └── Update parent Document active pointer (storageKey, checksum, fileSize, status, updatedAt)
          │      (Compensation: If DB transaction fails, delete uploaded S3 object)
          ├── 7. Dispatch domain event:
          │      └── DocumentVersionCreatedEvent (for AI/RAG re-indexing & UC-AUDIT-01 audit log)
          └── 8. Return DocumentVersionResponseDto
    ← HTTP 200 OK with new version details
```

### Flow 2: Access Denied (Alternative Path 3a)
```text
User B (authenticated, has write:documents, but NOT owner and NOT admin)
  → POST /api/v1/documents/{docA_id}/versions
    → DocumentVersionService checks: docA.uploadedByUserId != User B
    → Throws DocumentAccessDeniedException("User does not have permission to edit this document")
    → RestExceptionHandler maps to HTTP 403 Forbidden with ErrorResponse
```

### Flow 3: Parent Document Not Found
```text
User -> POST /api/v1/documents/{random-uuid}/versions
  -> DocumentVersionService attempts findById(random-uuid) -> Empty
  -> Throws DocumentNotFoundException("Document not found with ID: " + id)
  -> RestExceptionHandler maps to HTTP 404 Not Found
```

### Flow 4: Validation Rejections
```text
Client -> POST /api/v1/documents/{id}/versions (file: "script.sh")
  -> File extension validation fails
  -> Throws UnsupportedMediaTypeException -> HTTP 415

Client -> POST /api/v1/documents/{id}/versions (file > 50MB)
  -> File size validation fails
  -> Throws PayloadTooLargeException -> HTTP 413
```

### Flow 5: Storage Failure & Compensation Rollback
```text
DocumentVersionService
  -> Streams upload to S3 -> S3 failure / timeout
  -> Throws StorageException -> HTTP 502 Bad Gateway (DB remains untouched)
  OR
  -> S3 upload succeeds, but DB commit fails (e.g. constraint violation or DB downtime)
  -> Catch block triggers compensation: objectStoragePort.delete(storageKey)
  -> Throws exception -> HTTP 500 / 502 (no orphaned S3 object left behind)
```

</execution_flow>

---

## 3. Relevant Components

<components>

| Component | Layer | Role | Evidence / Confidence |
|---|---|---|---|
| `Document` | Domain Model | Aggregate root; maintains active storage pointer and document status | CONFIRMED in `document/domain/model/Document.java` |
| `DocumentVersion` | Domain Model | Entity representing immutable historical snapshot | CONFIRMED in `document/domain/model/DocumentVersion.java` |
| `UploadDocumentVersionUseCase` | Inbound Port | Command interface for uploading document revision | Architectural requirement (Hexagonal) |
| `UploadDocumentVersionCommand` | Application DTO | Input payload encapsulating file stream, docId, userId, changeSummary | Architectural requirement |
| `DocumentVersionResponseDto` | Application DTO | Response payload returned to client (version details) | Architectural requirement |
| `DocumentVersionCreatedEvent` | Application Event | Domain event emitted for re-indexing & audit logging | Spec step 7 & 8 |
| `DocumentVersionService` | Application Service | Orchestrates validation, authorization, S3 streaming, DB atomic save, and events | Architectural requirement |
| `DocumentRepositoryPort` | Outbound Port | SPI for loading and updating `Document` | CONFIRMED in `document/application/ports/outbound/DocumentRepositoryPort.java` |
| `DocumentVersionRepositoryPort` | Outbound Port | SPI for persisting `DocumentVersion` snapshots | CONFIRMED in `document/application/ports/outbound/DocumentVersionRepositoryPort.java` |
| `ObjectStoragePort` | Outbound Port | SPI for S3 object streaming and compensation delete | CONFIRMED in `document/application/ports/outbound/ObjectStoragePort.java` |
| `DocumentController` | Primary Adapter | REST endpoint `POST /api/v1/documents/{id}/versions` | Existing controller to extend |
| `RestExceptionHandler` | Primary Adapter | Handles exceptions and maps to RFC 7807 JSON responses | CONFIRMED in `iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java` |

</components>

---

## 4. Dependencies & Boundaries

<boundaries>
  <callers>
    - Document Owners and Administrators via `POST /api/v1/documents/{id}/versions`.
  </callers>

  <callees>
    - PostgreSQL database (`documents` update, `document_versions` insert).
    - AWS S3 / MinIO Object Storage (`EXT-01`).
    - Spring `ApplicationEventPublisher` (`DocumentVersionCreatedEvent`).
  </callees>

  <persistence_boundary>
    - `document_versions`: INSERT new version snapshot (`version_number = current_version + 1`). Historical rows are strictly read-only / immutable (Rule B1).
    - `documents`: UPDATE active metadata (`storage_key`, `checksum_sha256`, `file_size_bytes`, `original_file_name`, `content_type`, `status = UPLOADED`, `updated_at`).
    - Transaction boundary: Both DB operations must occur within the same `@Transactional` boundary to guarantee ACID consistency (NF1).
  </persistence_boundary>

  <security_boundary>
    - Route-level security: Authenticated user with `write:documents` permission or `ROLE_ADMIN`.
    - Object-level security: `document.getUploadedByUserId().equals(currentUserId)` or user possesses `ROLE_ADMIN`.
    - Rejection: Non-authorized user must receive HTTP 403 Forbidden.
  </security_boundary>

  <storage_boundary>
    - Key structure: `documents/{doc_id}/v{next_version}/{sanitized_file_name}`.
    - S3 upload must be executed using streaming (`DigestInputStream`) to avoid loading large payloads into JVM memory.
    - S3 compensation rollback if DB save fails.
  </storage_boundary>
</boundaries>

---

## 5. Existing Tests & Test Gaps

<existing_tests>

| Test Suite | Scope | Gap for UC-DOC-02 |
|---|---|---|
| `DocumentUploadServiceTest.java` | UC-DOC-01 initial upload | Tests only `POST /api/v1/documents` (v1 creation) |
| `DocumentControllerTest.java` | UC-DOC-01 REST endpoint | Missing tests for `POST /api/v1/documents/{id}/versions` |
| `DocumentRepositoryAdapterTest.java` | Document persistence | Needs tests for updating document active pointer and retrieving version count |
| `DocumentVersionTest.java` | Domain model invariants | Validates constructor invariants; no revision logic tested yet |

</existing_tests>

---

## 6. Runtime & Configuration

<runtime_config>
  - **JDK:** Java 25 (`JavaLanguageVersion.of(25)`).
  - **Framework:** Spring Boot `4.0.7`, Spring Security 7.x, Hibernate 7.2.
  - **Storage:** AWS S3 SDK v2 (`software.amazon.awssdk:s3:2.29.52`) already configured and integrated via `S3ObjectStorageAdapter`.
  - **Database:** PostgreSQL with Liquibase migrations.
</runtime_config>

---

## 7. Source-of-Truth Analysis

<source_of_truth_analysis>

| Source | Specification / Rule | Authority | Impact |
|---|---|---|---|
| `UC-DOC-02 Specification` | Extension Use Case extending UC-DOC-01; endpoint `POST /api/v1/documents/{id}/versions`; uploads revision v2+ to S3 under `documents/{doc_id}/v{next_version}/{file_name}`; creates snapshot in `document_versions`; updates active pointer in `documents`; emits `DocumentVersionCreatedEvent`; invokes `UC-AUDIT-01`. | User Requirement / SoT | Primary functional requirements and acceptance criteria. |
| `003-create-document-tables.yaml` | `document_versions` has unique constraint on `(document_id, version_number)`. Columns: `id`, `document_id`, `version_number`, `storage_key`, `file_size_bytes`, `checksum_sha256`, `change_summary`, `uploaded_by_user_id`, `created_at`. | DB Schema Contract | Version snapshot persistence format is locked and ready. |
| `Clean Architecture Framework` | Domain POJOs remain independent of Spring/JPA/AWS. Application service coordinates inbound ports, outbound repository ports, and event publishers. | Architectural Standard | Enforces `UploadDocumentVersionUseCase`, `DocumentVersionService`, and port boundaries. |

</source_of_truth_analysis>

---

## 8. Evidence Classification

<evidence>
  <confirmed>
    - Table `document_versions` exists in Liquibase `003-create-document-tables.yaml` with required columns and unique constraint `(document_id, version_number)`.
    - `DocumentVersion.java` and `DocumentVersionJpaEntity.java` were already streamlined to the 8 required concepts in `CHG-DOC-02`.
    - `ObjectStoragePort` and `S3ObjectStorageAdapter` are fully functional and tested for streaming upload and compensation delete.
    - `DocumentRepositoryPort` and `DocumentVersionRepositoryPort` exist with basic persistence adapters.
    - Existing test suite (123+ tests) passes with `./gradlew check`.
  </confirmed>

  <observed>
    - `Document.java` does not currently store a `currentVersion` attribute, as it was trimmed during `CHG-DOC-01`.
    - `documents` table currently does not have a `current_version` column in `003-create-document-tables.yaml`.
    - There is no method in `DocumentVersionRepositoryPort` or `SpringDataDocumentVersionRepository` to find the latest version number for a document (e.g. `findMaxVersionNumberByDocumentId` or `countByDocumentId`).
    - `DocumentNotFoundException` and `DocumentAccessDeniedException` do not yet exist in `com.platform.app.document.domain.exception`.
  </observed>

  <hypothesized>
    - Determining the next version number can either be done by:
      - (Option 1) Re-introducing a `currentVersion` column (default 1) to `Document.java` and `documents` table.
      - (Option 2) Querying `document_versions` for `findTopByDocumentIdOrderByVersionNumberDesc` to compute `nextVersion = latestVersion.getVersionNumber() + 1`.
    - Authorization can be cleanly verified in the application service or security annotation by inspecting `document.getUploadedByUserId().equals(currentUserId)` and `authentication.getAuthorities()`.
  </hypothesized>
</evidence>

---

## 9. Assumptions & Uncertainty

<assumptions>
  - **Assumption 1 (First Version vs Subsequent Versions):** When a document is first uploaded via `POST /api/v1/documents` (UC-DOC-01), does it create a record in `document_versions` with `version_number = 1`, or was that bypassed? In `CHG-DOC-01`, we kept initial upload lean (only inserting into `documents`). If `UC-DOC-02` requires `version_number = current_version + 1`, we need to decide how version 1 is handled (either backfilling initial version 1 when version 2 is uploaded, or re-enabling version 1 insertion on initial upload, or tracking `currentVersion` directly on `Document`).
  - **Assumption 2 (Change Summary):** `changeSummary` is optional or recommended in the multipart request (max 500 characters matching database column).
  - **Assumption 3 (HTTP Status):** Spec says "9. Returns HTTP 200 OK with new version details" (unlike initial upload which returns HTTP 201 Created).
</assumptions>

---

## 10. Impacted Files (Preliminary Scope)

<impacted_files>
  - `src/main/java/com/platform/app/document/domain/model/Document.java`
  - `src/main/java/com/platform/app/document/domain/model/DocumentVersion.java`
  - `src/main/java/com/platform/app/document/domain/exception/DocumentNotFoundException.java` (new)
  - `src/main/java/com/platform/app/document/domain/exception/DocumentAccessDeniedException.java` (new)
  - `src/main/java/com/platform/app/document/application/ports/inbound/UploadDocumentVersionUseCase.java` (new)
  - `src/main/java/com/platform/app/document/application/dto/UploadDocumentVersionCommand.java` (new)
  - `src/main/java/com/platform/app/document/application/dto/DocumentVersionResponseDto.java` (new)
  - `src/main/java/com/platform/app/document/application/event/DocumentVersionCreatedEvent.java` (new)
  - `src/main/java/com/platform/app/document/application/services/DocumentVersionService.java` (new)
  - `src/main/java/com/platform/app/document/application/ports/outbound/DocumentVersionRepositoryPort.java`
  - `src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentController.java`
  - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java`
  - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentVersionRepositoryAdapter.java`
  - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/repository/SpringDataDocumentVersionRepository.java`
  - Tests under `src/test/java/com/platform/app/document/**`
</impacted_files>

---

## 11. Open Decisions (For INNOVATE Phase)

<open_decisions>
  - **DEC-DOC-03 (Tracking Current Version & Version Number Calculation):**
    - Option A: Add `currentVersion` (INT, default 1) to `Document.java`, `DocumentJpaEntity`, and `documents` table. `nextVersion` is simply `document.getCurrentVersion() + 1`. Both `documents.current_version` and `document_versions` row are updated atomically.
    - Option B: Query `document_versions` dynamically using `SELECT MAX(version_number) FROM document_versions WHERE document_id = ?`.
    - Option C: Hybrid / Backfill: Re-introduce `currentVersion` to `Document` (default 1), and ensure version 1 snapshot exists or is initialized upon version 2 creation.
  - **DEC-DOC-04 (Endpoint & Response Contract):**
    - `POST /api/v1/documents/{id}/versions` returning `DocumentVersionResponseDto` (HTTP 200 OK) with `id`, `documentId`, `versionNumber`, `storageKey`, `fileSizeBytes`, `checksumSha256`, `changeSummary`, `createdAt`.
  - **DEC-DOC-05 (Ownership & Permission Verification Mechanism):**
    - Explicit ownership check in application layer: `document.getUploadedByUserId().equals(currentUserId)` or `ROLE_ADMIN` vs Spring Security Expression `@PreAuthorize`.
</open_decisions>

---

## 12. Research Exit Criteria (Gate G0 Checklist)

<research_exit_criteria>
  - [x] Current behavior and schema constraints verified against existing codebase.
  - [x] Basic and alternative execution flows traced end-to-end.
  - [x] Architectural boundaries, ports, and adapters identified.
  - [x] Security authorization (`write:documents`, owner/admin check) and business invariants (B1, B2, NF1) analyzed.
  - [x] Open decisions prepared with trade-off context for INNOVATE phase.
  - [x] No unresolved research blockers.
</research_exit_criteria>

</research_context>
