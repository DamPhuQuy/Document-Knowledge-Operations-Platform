# Handoff: UC-DOC-01 Document Upload & S3 Object Storage

<handoff task_id="UC-DOC-01" version="2.0" framework="RIPER-5">

<handoff_status>
  <review_decision>PASS</review_decision>
  <review_artifact>[review.md](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/process/features/active/UC-DOC-01-document-upload-s3-storage/review.md)</review_artifact>
  <completed_date>2026-09-14</completed_date>
</handoff_status>

---

## 1. What Changed

<what_changed>
  Delivered base use case UC-DOC-01: Document Upload & S3 Object Storage. Enabled authenticated staff users (`ROLE_STAFF` with `write:documents` authority) to upload raw documents (PDF, DOCX, TXT, XLSX up to 50MB) via `POST /api/v1/documents`. The backend streams binaries directly to S3/Floci object storage, computes on-the-fly SHA-256 integrity checksums using `DigestInputStream`, commits Version 1 metadata in PostgreSQL, and dispatches audit events for UC-AUDIT-01.
</what_changed>

<main_changes>
  - `backend/build.gradle` — Added AWS S3 SDK v2 dependency (`software.amazon.awssdk:s3:2.29.52`).
  - `backend/src/main/resources/application.yaml` — Configured multipart size (50MB/55MB) and AWS S3 properties with endpoint override support.
  - `docker-compose.yaml` — Added lightweight Floci (`floci/floci:latest`) container emulator on port 4566.
  - `src/main/java/com/platform/app/document/domain/**` — Pure domain models (`Document`, `DocumentVersion`), enums (`AccessLevel`, `ProcessingStatus`), and domain exceptions.
  - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/storage/**` — `ObjectStoragePort`, `S3StorageProperties`, `S3StorageConfig`, and `S3ObjectStorageAdapter`.
  - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/**` — `DocumentJpaEntity`, `DocumentVersionJpaEntity`, Spring Data repositories, and manual mapping adapters.
  - `src/main/java/com/platform/app/document/application/**` — `UploadDocumentUseCase`, `DocumentUploadService`, commands/DTOs, and `DocumentUploadedEvent`.
  - `src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest/**` — `DocumentController` with `@PreAuthorize` security.
  - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java` — Mapped HTTP 413, 415, and 502 status responses.
  - `src/test/java/com/platform/app/document/**` — 29 comprehensive unit, service, persistence, and MockMvc integration tests.
</main_changes>

---

## 2. Why

<why>
  UC-DOC-01 is the entry point of the Document Management bounded context. It decouples document binary storage from relational metadata storage (B1 invariant) using AWS S3 / Floci, protects JVM heap memory from large file buffering (NF2 invariant), and lays the foundation for subsequent versioning (`UC-DOC-02`), search, and OCR/LLM pipelines.
</why>

---

## 3. What Proves It

<evidence>

| AC | Verifier | Result |
|---|---|:---:|
| AC-1 (Upload Success) | `./gradlew test --tests "*DocumentControllerTest.shouldUploadDocumentSuccessfully*"` | **PASS** |
| AC-2 (File Type 415) | `./gradlew test --tests "*DocumentControllerTest.shouldRejectUnsupportedFileType*"` | **PASS** |
| AC-2 (File Size 413) | `./gradlew test --tests "*DocumentControllerTest.shouldRejectOversizedFile*"` | **PASS** |
| AC-3 (Streaming SHA-256) | `./gradlew test --tests "*DocumentUploadServiceTest.shouldUploadDocumentSuccessfully*"` | **PASS** |
| AC-4 & B1 (No BLOB in DB) | `./gradlew test --tests "*DocumentRepositoryAdapterTest*"` | **PASS** |
| AC-5 (Version 1 Created) | `./gradlew test --tests "*DocumentUploadServiceTest.shouldUploadDocumentSuccessfully*"` | **PASS** |
| AC-6 (S3 502 & Compensation) | `./gradlew test --tests "*DocumentUploadServiceTest.shouldTriggerCompensationOnDbFailure*"` | **PASS** |
| AC-7 (Audit Event) | `./gradlew test --tests "*DocumentUploadServiceTest.shouldUploadDocumentSuccessfully*"` | **PASS** |
| Security (401 & 403) | `./gradlew test --tests "*DocumentControllerTest.shouldReject*"` | **PASS** |
| Full Regression Suite | `./gradlew test && ./gradlew check` | **PASS** |

```bash
./gradlew test
./gradlew check
```

</evidence>

---

## 4. What Remains Risky

<residual_risk>
  - Edge-case orphan files: If JVM crashes between S3 upload and DB commit before compensation executes, a periodic S3 reconciliation cron can be added in future maintenance.
</residual_risk>

---

## 5. Decisions & Assumptions

<decisions_and_assumptions>
  - DEC-DOC-01: AWS SDK v2 + Floci emulator selected. Floci provides an ultra-lightweight (~13MB RAM, ~24ms boot), MIT-licensed AWS S3 emulator.
  - DEC-DOC-02: Upload to S3 occurs outside the database transaction; if PostgreSQL commit fails, compensation call `deleteObject` is triggered.
  - DEC-DOC-03: Single-pass `DigestInputStream` used to compute SHA-256 while streaming to S3, using < 64KB heap RAM.
</decisions_and_assumptions>

---

## 6. Next Action

<next_action>
  Proceed to `UC-DOC-02` (Manage Document Versioning) to handle document revisions, downloads, and version history.
</next_action>

</handoff>
