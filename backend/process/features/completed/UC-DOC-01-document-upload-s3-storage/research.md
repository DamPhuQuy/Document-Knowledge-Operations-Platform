# Research: UC-DOC-01 Document Upload & S3 Object Storage

<research_context task_id="UC-DOC-01" version="2.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. No source modifications. No implementation decisions. -->
<research_status>
  <phase>RESEARCH</phase>
  <mode>READ-ONLY</mode>
  <research_owner>@engineer</research_owner>
  <last_updated>2026-09-14</last_updated>
</research_status>

---

## 1. Current Behavior

<current_behavior>
  - Database schema:
    - `003-create-document-tables.yaml` provisions PostgreSQL tables: `documents`, `document_versions`, `document_user_access`, `document_department_access`, `document_role_access`, `document_tags`, `document_tag_assignments`, and `document_chunks`.
    - `documents` has columns: `id`, `original_file_name`, `title`, `description`, `file_type`, `mime_type`, `file_size_bytes`, `checksum_sha256`, `storage_bucket`, `storage_key`, `is_s3_synced`, `processing_status`, `current_version`, `department_id`, `uploaded_by_user_id`, `access_level`, `metadata`, `created_at`, `updated_at`, `deleted_at`.
    - `document_versions` has columns: `id`, `document_id`, `version_number`, `storage_bucket`, `storage_key`, `file_size_bytes`, `checksum_sha256`, `is_s3_synced`, `change_summary`, `uploaded_by_user_id`, `created_at`.
    - `004-create-audit-tables.yaml` provisions `audit_logs`.
  - Source code status:
    - No Java classes exist under `com.platform.app.document.*`. The entire Document bounded context is currently unimplemented.
    - In `build.gradle`, no AWS S3 SDK (or MinIO client) is included. Only standard Spring WebMVC, Data JPA, Liquibase, Security, and JJWT are present.
    - In `application.yaml`, multipart upload limits and S3 configuration properties (`app.storage.s3.*`) are not yet defined.
    - Spring Boot default multipart file upload limit is 1MB (request max 10MB), whereas the business specification requires up to 50MB.
</current_behavior>

---

## 2. Execution Flow

<execution_flow>

### Flow 1: Successful Document Upload (Basic Path)
```text
Knowledge Worker (ROLE_STAFF with write:documents)
  → POST /api/v1/documents (multipart/form-data: file, title, description, accessLevel)
    → TraceIdFilter (injects traceId, X-Trace-Id)
    → Spring Security FilterChain (authenticates JWT, verifies write:documents or ROLE_ADMIN)
    → DocumentController.uploadDocument()
      → Validate file presence, non-empty, and parameter validity
      → Validate extension: .pdf, .docx, .txt, .xlsx (reject other -> 415)
      → Validate MIME type: application/pdf, application/vnd.openxmlformats-officedocument..., text/plain
      → Validate size <= 50MB (reject larger -> 413)
      → UploadDocumentUseCase.execute(command) (DocumentUploadService)
        ├── Resolve uploader userId and departmentId from Security Context
        ├── Compute SHA-256 checksum via streaming (NF2)
        ├── Generate new DocumentId (UUID) and storageKey: "documents/{docId}/v1/{sanitizedFileName}"
        ├── Stream binary to S3 Object Storage via ObjectStoragePort
        │     - If S3 upload fails -> throw StorageException (maps to HTTP 502)
        ├── Persist Document aggregate root in documents table:
        │     - current_version = 1
        │     - processing_status = "UPLOADED"
        │     - is_s3_synced = true
        │     - access_level = INTERNAL (default scoped to department)
        ├── Persist initial DocumentVersion in document_versions table:
        │     - version_number = 1
        │     - storage_bucket, storage_key, file_size_bytes, checksum_sha256
        ├── Publish DocumentUploadedEvent (for UC-AUDIT-01 asynchronous logging & downstream processing)
        └── Return DocumentResponseDto
    ← HTTP 201 Created with Location header /api/v1/documents/{docId} and JSON metadata
```

### Flow 2: Validation Rejections (Alternative Paths 3a, 3b)
```text
Client -> POST /api/v1/documents (file: "malicious.exe" or "archive.zip")
  -> DocumentController / Validator catches invalid extension / MIME type
  -> Throws UnsupportedMediaTypeException
  -> RestExceptionHandler maps to HTTP 415 Unsupported Media Type

Client -> POST /api/v1/documents (file > 50MB)
  -> Spring MultipartMaxUploadSizeExceededException or Validator
  -> RestExceptionHandler maps to HTTP 413 Payload Too Large
```

### Flow 3: S3 Storage Failure & Compensation (Alternative Path 5a)
```text
DocumentUploadService
  -> Attempts upload to S3 via ObjectStoragePort
  -> S3 times out / throws SdkClientException / S3Exception
  -> Service catches error, aborts DB transaction, cleans up any partially written S3 object
  -> Throws StorageException("S3 upload failed", cause)
  -> RestExceptionHandler catches StorageException
  -> Logs WARN/ERROR with traceId
  <- Returns HTTP 502 Bad Gateway
```

</execution_flow>

---

## 3. Relevant Components

<components>

| Component | Layer | Role | Evidence / Confidence |
|---|---|---|---|
| `003-create-document-tables.yaml` | Liquibase DDL | Database tables for `documents` and `document_versions` | CONFIRMED in `src/main/resources/db/changelog/changes/` |
| `004-create-audit-tables.yaml` | Liquibase DDL | Database table for `audit_logs` | CONFIRMED in `src/main/resources/db/changelog/changes/` |
| `Document` | Domain Model | Core aggregate root representing document entity | Architectural requirement |
| `DocumentVersion` | Domain Model | Entity representing specific version snapshot | Architectural requirement |
| `AccessLevel` | Domain Enum | `PUBLIC`, `INTERNAL`, `RESTRICTED`, `CONFIDENTIAL` | Spec Rule B2 |
| `ProcessingStatus` | Domain Enum | `UPLOADED`, `PROCESSING`, `COMPLETED`, `FAILED` | DB Schema check constraint |
| `UploadDocumentUseCase` | Inbound Port | Command interface for document upload | Clean Architecture |
| `DocumentRepositoryPort` | Outbound Port | Database SPI for document persistence | Clean Architecture |
| `DocumentVersionRepositoryPort` | Outbound Port | Database SPI for version persistence | Clean Architecture |
| `ObjectStoragePort` | Outbound Port | Storage abstraction for S3 / Object Storage upload | Clean Architecture & Rule B1 |
| `DocumentUploadService` | Application Service | Coordinates validation, hashing, S3 upload, DB save, and events | Core orchestrator |
| `DocumentController` | Primary Adapter | Multipart REST controller handling `POST /api/v1/documents` | Primary REST entrypoint |
| `S3ObjectStorageAdapter` | Secondary Adapter | Implements `ObjectStoragePort` via AWS S3 SDK (or S3-compatible client) | External infrastructure |
| `DocumentRepositoryAdapter` | Secondary Adapter | Implements `DocumentRepositoryPort` via Spring Data JPA | Secondary persistence |

</components>

---

## 4. Dependencies & Boundaries

<boundaries>
  <callers>
    - Knowledge Workers / Staff invoking `POST /api/v1/documents` with `multipart/form-data`.
  </callers>

  <callees>
    - PostgreSQL database (`documents`, `document_versions`).
    - AWS S3 / MinIO Object Storage (`EXT-01`).
    - Spring `ApplicationEventPublisher` (publishing `DocumentUploadedEvent` for UC-AUDIT-01).
  </callees>

  <persistence>
    - Writes to `documents` table (INSERT document metadata).
    - Writes to `document_versions` table (INSERT version 1 record).
    - Rule B1 strictly forbids storing binary file blobs in PostgreSQL; only bucket and storage_key are persisted.
  </persistence>

  <security_boundary>
    - Access requires `ROLE_STAFF` with `write:documents` authority (or `ROLE_ADMIN`).
    - Authentication context is resolved to extract uploader `userId` and `departmentId`.
    - Access level defaults to `INTERNAL` scoped to uploader department.
  </security_boundary>

  <storage_boundary>
    - Object storage bucket: Configured via property `app.storage.s3.bucket-name`.
    - Key structure: `documents/{documentId}/v{versionNumber}/{originalFileName}`.
    - S3 interaction must be isolated behind `ObjectStoragePort` to preserve Clean Architecture and enable mock testing without requiring live AWS infrastructure.
  </storage_boundary>
</boundaries>

---

## 5. Existing Tests

<existing_tests>

| Test Suite | Scope | Gap for UC-DOC-01 |
|---|---|---|
| `AuthControllerTest.java` | IAM Auth | No document tests exist |
| `DepartmentControllerTest.java` | IAM Departments | No document tests exist |
| `TraceIdFilterTest.java` | Shared Tracing | Will automatically trace document requests |

</existing_tests>

---

## 6. Runtime & Configuration

<runtime_config>
  - **JDK:** Java 25 (`JavaLanguageVersion.of(25)`).
  - **Framework:** Spring Boot `4.0.7`, Spring Security 7.x, Hibernate 7.2.
  - **Multipart Configuration:**
    - Need to configure `spring.servlet.multipart.max-file-size=50MB` and `spring.servlet.multipart.max-request-size=55MB`.
  - **AWS S3 Dependency:**
    - `software.amazon.awssdk:s3:2.29.x` (or official AWS SDK v2 S3 module) is required for communicating with S3 / S3-compatible endpoints.
  - **Storage Properties in `application.yaml`:**
    - `app.storage.s3.bucket-name`: Target S3 bucket.
    - `app.storage.s3.region`: AWS region (e.g. `ap-southeast-1` or `us-east-1`).
    - `app.storage.s3.endpoint`: Optional custom endpoint URL for local MinIO/LocalStack development.
    - `app.storage.s3.access-key`: S3 Access Key.
    - `app.storage.s3.secret-key`: S3 Secret Key.
</runtime_config>

---

## 7. Source-of-Truth Analysis

<source_of_truth_analysis>

| Source | Specification / Rule | Authority | Impact |
|---|---|---|---|
| `docs/specs/business/use_cases/02_document_management.md` | UC-DOC-01: Upload PDF, DOCX, TXT, XLSX $\le 50$MB to S3, compute SHA-256, create `documents` and `document_versions` rows, invoke UC-AUDIT-01. | User Requirement / SoT | Primary functional requirement. |
| `003-create-document-tables.yaml` | `documents` columns: `storage_bucket`, `storage_key`, `checksum_sha256`, `is_s3_synced`, `processing_status`, `current_version`, `department_id`, `uploaded_by_user_id`, `access_level`. | DB Schema Contract | Implementation must match existing DDL constraints exactly. |
| `004-create-audit-tables.yaml` | `audit_logs` columns: `user_id`, `action`, `resource_type`, `resource_id`, `ip_address`, `user_agent`, `status`. | DB Schema Contract | Event payload must capture these fields for UC-AUDIT-01. |
| `process/context/architecture/architecture-template.md` | Clean Hexagonal Architecture: Domain -> Application -> Infrastructure. Domain model must not depend on S3 SDK or JPA. | Architectural Standard | Enforces `ObjectStoragePort` and `DocumentRepositoryPort` interfaces. |

</source_of_truth_analysis>

---

## 8. Evidence Classification

<evidence>
  <confirmed>
    - Tables `documents` and `document_versions` already exist in Liquibase migrations with complete column definitions and indices.
    - Table `audit_logs` exists in `004-create-audit-tables.yaml`.
    - No existing document classes exist in `src/main/java/com/platform/app/`.
    - Spring Boot 4.0.7 is active on Java 25.
  </confirmed>

  <observed>
    - AWS SDK v2 is not yet present in `build.gradle`.
    - `application.yaml` does not have S3 properties or 50MB multipart limits configured.
  </observed>

  <hypothesized>
    - Adding `software.amazon.awssdk:s3` to `build.gradle` will provide clean streaming upload support (`RequestBody.fromInputStream(stream, length)`) without buffering files into JVM heap.
    - Decoupling S3 through `ObjectStoragePort` allows unit/integration testing with an in-memory or mock storage adapter without needing live AWS credentials in CI/CD.
  </hypothesized>
</evidence>

---

## 9. Assumptions & Uncertainty

<assumptions>
  - **Assumption 1 (File Extensions & MIME Types):** Allowed extensions: `.pdf` (`application/pdf`), `.docx` (`application/vnd.openxmlformats-officedocument.wordprocessingml.document`), `.txt` (`text/plain`), `.xlsx` (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`).
  - **Assumption 2 (S3 Storage Portability):** The S3 client adapter will support custom endpoint configuration so it works interchangeably with AWS S3, local MinIO, or LocalStack.
  - **Assumption 3 (Audit Logging via Domain Event):** Following the proven pattern established in IAM (`LoginService`, `DepartmentService`), `DocumentUploadService` will publish a `DocumentUploadedEvent` through Spring's `ApplicationEventPublisher`.
</assumptions>

---

## 10. Impacted Files

<impacted_files>
  - `backend/build.gradle` — Add AWS S3 SDK dependency
  - `backend/src/main/resources/application.yaml` — Add multipart limits (50MB) and S3 properties
  - `backend/src/main/resources/application-prod.yaml` — Production S3 properties
  - `backend/src/main/resources/application-staging.yaml` — Staging S3 properties
  - `src/main/java/com/platform/app/document/domain/model/Document.java` (new)
  - `src/main/java/com/platform/app/document/domain/model/DocumentVersion.java` (new)
  - `src/main/java/com/platform/app/document/domain/model/AccessLevel.java` (new)
  - `src/main/java/com/platform/app/document/domain/model/ProcessingStatus.java` (new)
  - `src/main/java/com/platform/app/document/domain/exception/UnsupportedMediaTypeException.java` (new)
  - `src/main/java/com/platform/app/document/domain/exception/PayloadTooLargeException.java` (new)
  - `src/main/java/com/platform/app/document/domain/exception/StorageException.java` (new)
  - `src/main/java/com/platform/app/document/application/ports/inbound/UploadDocumentUseCase.java` (new)
  - `src/main/java/com/platform/app/document/application/ports/inbound/UploadDocumentCommand.java` (new)
  - `src/main/java/com/platform/app/document/application/ports/outbound/DocumentRepositoryPort.java` (new)
  - `src/main/java/com/platform/app/document/application/ports/outbound/DocumentVersionRepositoryPort.java` (new)
  - `src/main/java/com/platform/app/document/application/ports/outbound/ObjectStoragePort.java` (new)
  - `src/main/java/com/platform/app/document/application/services/DocumentUploadService.java` (new)
  - `src/main/java/com/platform/app/document/application/dto/DocumentResponseDto.java` (new)
  - `src/main/java/com/platform/app/document/application/dto/DocumentUploadedEvent.java` (new)
  - `src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentController.java` (new)
  - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/entity/DocumentJpaEntity.java` (new)
  - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/entity/DocumentVersionJpaEntity.java` (new)
  - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/repository/SpringDataDocumentRepository.java` (new)
  - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/repository/SpringDataDocumentVersionRepository.java` (new)
  - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentRepositoryAdapter.java` (new)
  - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentVersionRepositoryAdapter.java` (new)
  - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/storage/adapter/S3ObjectStorageAdapter.java` (new)
  - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/storage/config/S3StorageProperties.java` (new)
  - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/storage/config/S3StorageConfig.java` (new)
  - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java` — Add handlers for `UnsupportedMediaTypeException` (415), `PayloadTooLargeException` (413), and `StorageException` (502)
  - Test files under `src/test/java/com/platform/app/document/*`
</impacted_files>

---

## 11. Open Decisions (For INNOVATE Phase)

<open_decisions>
  - **DEC-DOC-01 (AWS SDK Dependency):** Standard `software.amazon.awssdk:s3:2.29.x` vs Spring Cloud AWS vs MinIO client.
    - *Preview:* Official AWS SDK v2 (`software.amazon.awssdk:s3`) is the enterprise standard, supports custom endpoint URLs for local MinIO / LocalStack, and avoids Spring Cloud version locking.
  - **DEC-DOC-02 (S3 Upload vs DB Transaction Coordination):**
    - S3 upload before DB commit vs DB insert first then upload?
    - *Preview:* Upload to S3 first with streaming hash, then commit DB in a `@Transactional` block. If DB insert fails, issue compensation S3 `deleteObject`. If S3 upload fails, DB is never touched.
  - **DEC-DOC-03 (Streaming Hash & Upload Strategy):**
    - How to satisfy NF2 (streaming SHA-256 calculation without full memory buffering).
    - *Preview:* Use `DigestInputStream` wrapping the incoming `MultipartFile.getInputStream()`.
</open_decisions>

---

## 12. Research Exit Criteria (Gate G0)

<research_exit_criteria>
  - [x] Current behavior and schema constraints verified against Liquibase DDL.
  - [x] Basic and alternative execution flows traced end-to-end.
  - [x] Architectural boundaries, ports, and adapters specified.
  - [x] Security authorization (`write:documents`) and business invariants (B1, B2, B3, NF1, NF2) analyzed.
  - [x] Open decisions prepared for INNOVATE phase.
  - [x] No unresolved research blockers.
</research_exit_criteria>

</research_context>
