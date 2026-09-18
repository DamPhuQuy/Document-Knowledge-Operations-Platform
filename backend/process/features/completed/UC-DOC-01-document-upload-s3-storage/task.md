# Task: [UC-DOC-01] Document Upload & S3 Object Storage

<task_spec version="3.0" framework="RIPER-5">

<!-- ════════════════════════════════════════════
     SECTION 0 — TASK CONTROL (master state record)
     ════════════════════════════════════════════ -->
<task_control>
  <status>COMPLETED</status>  <!-- BACKLOG | ACTIVE | BLOCKED | PAUSED | REVIEW | COMPLETED | CANCELLED -->
  <spec_level>S3</spec_level>  <!-- S0=sketch | S1=defined | S2=verified | S3=locked -->
  <priority>P0</priority>  <!-- P0=urgent | P1=high | P2=normal | P3=low -->
  <risk>MEDIUM</risk>  <!-- LOW | MEDIUM | HIGH -->
  <estimated_story_points>5</estimated_story_points>  <!-- 1 SP ≈ 2-4 focused engineering hours -->
  <working_mode>DELEGATED</working_mode>  <!-- PAIR (default: halts at each gate for review) | DELEGATED (fast-track: continuous autonomous run) | MANUAL | DIAGNOSE-ONLY -->
  <current_phase>REVIEW</current_phase>  <!-- RESEARCH | INNOVATE | PLAN | EXECUTE | REVIEW -->
  <owner>@engineer</owner>
  <decision_owner>@engineer</decision_owner>
  <created>2026-09-14</created>
  <last_updated>2026-09-14</last_updated>
</task_control>

---

## 1. Specification (Pillar 1: Task / Spec)

<specification>
  <goal>
    Implement base use case UC-DOC-01: Document Upload & S3 Object Storage, enabling authenticated Knowledge Workers (`ROLE_STAFF` with `write:documents` permission) to upload raw binary files (PDF, DOCX, TXT, XLSX up to 50MB) via `multipart/form-data`, stream-upload them to S3 Object Storage (`EXT-01`), compute streaming SHA-256 integrity checksums, persist Version 1 metadata in PostgreSQL (`documents` and `document_versions`), publish audit events for UC-AUDIT-01, and return HTTP 201 Created with document metadata.
  </goal>

  <current_behavior>
    - The backend contains no Java domain models, use cases, ports, or adapters under `com.platform.app.document`.
    - Database migration `003-create-document-tables.yaml` has already provisioned `documents`, `document_versions`, and related tables in Liquibase.
    - Database migration `004-create-audit-tables.yaml` has provisioned `audit_logs`.
    - No S3 / Object Storage SDK or adapter exists in `backend/build.gradle`.
    - No endpoint exists for `POST /api/v1/documents`.
  </current_behavior>

  <expected_behavior>
    - Endpoint `POST /api/v1/documents` accepts `multipart/form-data` with file part (`file`) and metadata (`title`, `description`, optional `accessLevel`).
    - Validates file type extension (only `.pdf`, `.docx`, `.txt`, `.xlsx` permitted) and corresponding MIME types. If unsupported, returns HTTP 415 Unsupported Media Type.
    - Validates file size ($\le 50\text{ MB}$). If oversized, returns HTTP 413 Payload Too Large.
    - Computes SHA-256 checksum in a streaming fashion (NF2) without buffering large payloads in memory.
    - Streams binary payload to S3-compatible Object Storage (`EXT-01`) at key `documents/{doc_id}/v1/{file_name}`.
    - If S3 upload fails or times out, rolls back transaction, cleans up any remote storage artifacts, and returns HTTP 502 Bad Gateway.
    - Persists document entity in `documents` with `current_version = 1`, `processing_status = 'UPLOADED'`, `is_s3_synced = true`, and access level (default `INTERNAL` scoped to uploader department).
    - Persists initial version in `document_versions` with `version_number = 1`.
    - Dispatches domain event for `UC-AUDIT-01` to record `UPLOAD_DOC` event in `audit_logs`.
    - Returns HTTP 201 Created with JSON document metadata and `Location: /api/v1/documents/{doc_id}`.
  </expected_behavior>

  <actor_authorization>
    Primary Actor: Knowledge Worker / Staff (`ROLE_STAFF`).
    Authorization Requirement: Authenticated user must possess `write:documents` authority (or `ROLE_ADMIN`).
  </actor_authorization>

  <invariants>
    - B1: Binary BLOBs are strictly prohibited in PostgreSQL; only S3 storage pointers (`storage_bucket`, `storage_key`) are stored.
    - B2: Default access level is `INTERNAL` scoped to the uploader's `department_id`.
    - B3: Checksum SHA-256 must be calculated and stored to guarantee binary integrity.
    - Clean Architecture: Domain entities in `document/domain/model/*` must remain pure POJOs without AWS SDK or JPA dependencies.
    - Zero Credential Leakage: AWS credentials or sensitive token claims must never appear in logs.
    - Transactional Integrity: Failed S3 uploads or database failures must not leave orphaned records or orphaned S3 objects.
  </invariants>

  <out_of_scope>
    - `UC-DOC-02` (Manage Document Versioning - uploading revision v2+).
    - `UC-DOC-03` (Configure Access Control Matrix / ACL grants).
    - Document text extraction, OCR, chunking, or embedding generation (handled under AI & RAG Subsystem).
    - Presigned URL generation for downloads (covered in document retrieval use cases).
  </out_of_scope>

  <acceptance_criteria>
    - [ ] AC-1: `POST /api/v1/documents` accepts valid PDF, DOCX, TXT, XLSX files up to 50MB with title and description, returning HTTP 201 Created and response body with document details.
    - [ ] AC-2: Uploaded binary is stored in S3 at `documents/{doc_id}/v1/{file_name}` and SHA-256 checksum is computed via streaming.
    - [ ] AC-3: Records are created in `documents` (`current_version = 1`, `processing_status = 'UPLOADED'`, `access_level = 'INTERNAL'`) and `document_versions` (`version_number = 1`).
    - [ ] AC-4: Unsupported file types return HTTP 415 Unsupported Media Type.
    - [ ] AC-5: File sizes exceeding 50MB return HTTP 413 Payload Too Large.
    - [ ] AC-6: S3 storage failures or connection timeouts result in HTTP 502 Bad Gateway and clean rollback.
    - [ ] AC-7: Access is protected: unauthorized requests without `write:documents` return HTTP 403 Forbidden (or 401 if unauthenticated).
    - [ ] AC-8: Domain event is emitted for asynchronous audit logging (`UC-AUDIT-01`).
    - [ ] AC-9: All automated unit and integration tests pass cleanly (`./gradlew test`, `./gradlew check`).
  </acceptance_criteria>

  <!-- Definition-of-Ready (DoR) Gate -->
  <definition_of_ready>
    - [x] Target outcome and acceptance criteria are verifiable without guessing.
    - [x] Invariants and <out_of_scope> boundaries are explicit.
    - [x] Open questions identified for resolution in INNOVATE phase (decision.md).
  </definition_of_ready>
</specification>

---

## 2. Context Boundaries (Pillar 2: Context)

<context_boundaries>
  <target_files>
    - `src/main/java/com/platform/app/document/domain/model/*` — Document and DocumentVersion aggregates, AccessLevel, ProcessingStatus
    - `src/main/java/com/platform/app/document/domain/exception/*` — Domain exceptions (UnsupportedMediaTypeException, PayloadTooLargeException, StorageException)
    - `src/main/java/com/platform/app/document/application/ports/inbound/*` — UploadDocumentUseCase, UploadDocumentCommand
    - `src/main/java/com/platform/app/document/application/ports/outbound/*` — DocumentRepositoryPort, DocumentVersionRepositoryPort, ObjectStoragePort
    - `src/main/java/com/platform/app/document/application/services/*` — DocumentUploadService
    - `src/main/java/com/platform/app/document/application/dto/*` — DocumentResponseDto, DocumentUploadedEvent
    - `src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest/*` — DocumentController, request/response DTOs
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/*` — JPA entities, Spring Data repositories, adapters
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/storage/*` — S3 / Object Storage secondary adapter
    - `src/main/resources/application*.yaml` — S3 storage configuration properties
    - `backend/build.gradle` — AWS S3 SDK dependency
    - `src/test/java/com/platform/app/document/*` — Unit and integration tests
  </target_files>

  <context_groups>
    - `specs/business/use_cases/02_document_management.md` (Source of Truth specification)
    - `process/context/architecture/architecture-template.md` (Module standards)
    - `process/development-protocols/implementation-standards.md`
  </context_groups>

  <source_of_truth>
    <requirement>[`docs/specs/business/use_cases/02_document_management.md`](docs/specs/business/use_cases/02_document_management.md) — UC-DOC-01</requirement>
    <schema>[`src/main/resources/db/changelog/changes/003-create-document-tables.yaml`](src/main/resources/db/changelog/changes/003-create-document-tables.yaml)</schema>
    <architecture>[`process/context/architecture/architecture-template.md`](process/context/architecture/architecture-template.md)</architecture>
  </source_of_truth>
</context_boundaries>

---

## 3. Verification Strategy

<verification_strategy>

| AC / Risk | Evidence required | Verifier |
|---|---|---|
| AC-1, AC-3 | Successful upload produces HTTP 201 Created and persisted database rows in both `documents` and `document_versions` | `./gradlew test --tests "*DocumentControllerTest*"` |
| AC-2 | S3 storage key matches pattern `documents/{id}/v1/{filename}` and SHA-256 checksum matches stream digest | `./gradlew test --tests "*DocumentUploadServiceTest*"` |
| AC-4 | Unsupported extension (.exe, .zip) returns HTTP 415 | `./gradlew test --tests "*DocumentControllerTest*"` |
| AC-5 | Files over 50MB return HTTP 413 | `./gradlew test --tests "*DocumentControllerTest*"` |
| AC-6 | S3 failure triggers compensation and returns HTTP 502 | `./gradlew test --tests "*DocumentUploadServiceTest*"` |
| AC-7 | Unauthenticated / unauthorized callers receive HTTP 401 / 403 | `./gradlew test --tests "*DocumentControllerSecurityTest*"` |
| AC-8 | `DocumentUploadedEvent` is dispatched upon successful commit | `./gradlew test --tests "*DocumentUploadServiceTest*"` |
| AC-9 | Clean regression and style check | `./gradlew check` |

</verification_strategy>

---

## 4. Decisions

<decisions>
  <approved_decisions>
    <!-- Populated after Gate 1 approval -->
  </approved_decisions>

  <open_decisions>
    | ID | Question | Why it matters | Owner | Blocking? |
    |---|---|---|---|---|
    | DEC-DOC-01 | AWS S3 SDK v2 vs MinIO Java Client | Determines runtime dependency in `build.gradle` and local vs AWS compatibility | @engineer | YES (at G1) |
    | DEC-DOC-02 | S3 Upload vs DB Transaction ordering | Ensures atomic consistency and prevents orphaned S3 files or ghost DB rows | @engineer | YES (at G1) |
    | DEC-DOC-03 | Multipart streaming vs temp file buffering for SHA-256 & S3 | Trade-off between heap memory usage and disk I/O | @engineer | YES (at G1) |
  </open_decisions>
</decisions>

---

## 5. RIPER-5 Execution Plan (Pillar 4: Loop)

<execution_plan>
  <phase name="Research" order="1">
    - [x] Ingest UC-DOC-01 use case spec, schema constraints, and invariants.
    - [x] Audit existing Liquibase tables (`documents`, `document_versions`, `audit_logs`).
    - [x] Trace execution flow (normal upload, validation rejection, S3 failure).
    - [x] Produce `research.md` artifact with classified evidence and open decisions.
    <gate id="G0" label="Research Complete">
      - [x] Current behavior and gaps documented.
      - [x] Execution flows mapped.
      - [x] Open decisions identified.
    </gate>
  </phase>

  <phase name="Innovate" order="2">
    - [x] Formulate 2-3 options in `decision.md` covering S3 Client selection, transactional consistency, and streaming hash computation.
    - [x] Sign Gate 1.
    <gate id="G1" label="Gate 1 — Decision Approved">
      - [x] Options analyzed with trade-off matrix.
      - [x] Selected option approved.
    </gate>
  </phase>

  <phase name="Plan" order="3">
    - [x] Slice implementation plan with verifiers and scope contract in `plan.md`.
    - [x] Sign Gate 2.
    <gate id="G2" label="Gate 2 — Plan Approved">
      - [x] Slices, verifiers, and allowed files defined.
    </gate>
  </phase>

  <phase name="Execute" order="4">
    - [x] Implement domain models, ports, services, S3 adapter, JPA adapter, REST controller.
    - [x] Execute tests slice by slice and record in `state.md`.
  </phase>

  <phase name="Review" order="5">
    - [x] Comprehensive review across behavior, architecture, data, security, regression in `review.md`.
    - [x] Sign Gate 3 and produce `handoff.md`.
    <gate id="G3" label="Gate 3 — Review Passed">
      - [x] All AC verified with evidence.
    </gate>
  </phase>
</execution_plan>

---

## 6. Guardrails & Escalation (Pillar 3 & 4: Harness)

<guardrails>
  <stop_conditions>
    - Direct storage of file binary blobs into PostgreSQL.
    - Leaking AWS access keys or secrets in source code or logs.
    - Altering frozen Liquibase migrations `001` through `004`.
    - Retry budget exhausted (3 attempts).
  </stop_conditions>

  <retry_budget max_attempts="3">
    Maximum 3 consecutive attempts per distinct failure symptom before halting.
  </retry_budget>
</guardrails>

</task_spec>
