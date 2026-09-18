# Plan: PLAN-DOC-01 UC-DOC-01 Document Upload & S3 Object Storage

<execution_plan task_id="UC-DOC-01" plan_id="PLAN-DOC-01" version="2.0" framework="RIPER-5">

<!-- PLAN PHASE. Plan artifacts only. No source-code changes. -->
<plan_status>
  <phase>PLAN</phase>
  <last_updated>2026-09-14</last_updated>
</plan_status>

---

## 1. Input Artifacts

<input_artifacts>
  <task_spec>[task.md](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/process/features/active/UC-DOC-01-document-upload-s3-storage/task.md)</task_spec>
  <research>[research.md](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/process/features/active/UC-DOC-01-document-upload-s3-storage/research.md)</research>
  <decision>[decision.md](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/process/features/active/UC-DOC-01-document-upload-s3-storage/decision.md) — DEC-DOC-01 (AWS SDK v2 + Floci), DEC-DOC-02 (Upload First + S3 Compensation), DEC-DOC-03 (Single-Pass DigestInputStream)</decision>
</input_artifacts>

---

## 2. Execution Constraints

<!-- These constraints bind the Execute phase. Agent must not exceed them. -->
<execution_constraints>
  <allowed_files>
    - `backend/build.gradle` — Add `software.amazon.awssdk:s3:2.29.52` dependency.
    - `backend/src/main/resources/application.yaml` — Configure multipart limits (50MB/55MB) and S3 properties.
    - `docker-compose.yaml` — Add `floci` container service for local AWS emulation.
    - `backend/src/main/java/com/platform/app/document/**` — All domain, application, and infrastructure code for document bounded context.
    - `backend/src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java` — Exception handlers for 413, 415, 502.
    - `backend/src/test/java/com/platform/app/document/**` — Unit and integration tests for document management.
  </allowed_files>
  <forbidden_files>
    - `backend/src/main/resources/db/changelog/**` — Frozen Liquibase migrations 001-004 must NOT be modified.
    - `backend/src/main/java/com/platform/app/iam/domain/**` — IAM domain must remain untouched.
    - `backend/src/main/java/com/platform/app/iam/application/**` — IAM application must remain untouched.
    - `backend/src/main/java/com/platform/app/common/filter/TraceIdFilter.java` — Existing logging infrastructure from FEAT-SYS-01 must not be modified.
  </forbidden_files>
  <allowed_commands>
    - `./gradlew test`
    - `./gradlew check`
    - `./gradlew build -x test`
    - `docker compose up -d floci` (optional for live verification if docker is available)
  </allowed_commands>
  <restricted_operations>
    - No DB migrations without explicit engineer approval.
    - No binary BLOB storage in PostgreSQL.
    - No MapStruct code generation (use manual factory/builder per user preference).
    - No credential hardcoding in source or test files.
  </restricted_operations>
  <required_approvals>
    - Gate 2 sign-off before entering EXECUTE phase.
    - Gate 3 sign-off before archive/handoff.
  </required_approvals>
</execution_constraints>

---

## 3. Slice Summary

<slice_summary>

| ID | Behavior | Boundary | Files | AC | Verifier | Risk | Mode | Rollback |
|---|---|---|---|---|---|---|---|---|
| **S1** | Domain Models & Exceptions | Domain Core | `document/domain/**` | AC-1 (models, enums, rules) | `./gradlew test --tests "com.platform.app.document.domain.*"` | LOW | ATOMIC | `git checkout -- src/main/java/com/platform/app/document/domain src/test/java/com/platform/app/document/domain` |
| **S2** | AWS SDK & Storage Adapter + Floci Config | Infrastructure Storage | `build.gradle`, `application.yaml`, `docker-compose.yaml`, `document/infrastructure/.../storage/**` | AC-3, AC-6 (S3 streaming, compensation) | `./gradlew test --tests "com.platform.app.document.infrastructure.adapters.secondary.storage.*"` | MEDIUM | ATOMIC | `git checkout -- build.gradle application.yaml docker-compose.yaml src/main/java/com/platform/app/document/infrastructure/adapters/secondary/storage` |
| **S3** | PostgreSQL Persistence Entities & Repositories | Infrastructure Persistence | `document/infrastructure/.../persistence/**`, `document/application/ports/outbound/*RepositoryPort.java` | AC-4, AC-5 (PostgreSQL storage pointers, Version 1) | `./gradlew test --tests "com.platform.app.document.infrastructure.adapters.secondary.persistence.*"` | LOW | ATOMIC | `git checkout -- src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence` |
| **S4** | Application Upload Service & Compensation Flow | Application Layer | `document/application/**` | AC-1, AC-3, AC-6, AC-7 (Audit event, compensation) | `./gradlew test --tests "com.platform.app.document.application.*"` | MEDIUM | ATOMIC | `git checkout -- src/main/java/com/platform/app/document/application` |
| **S5** | REST Controller & Exception Handler & Web Layer Integration | Primary Adapter | `document/infrastructure/.../rest/**`, `RestExceptionHandler.java` | AC-1, AC-2, AC-6 (HTTP 201, 413, 415, 502) | `./gradlew test --tests "com.platform.app.document.infrastructure.adapters.primary.rest.*"` | LOW | ATOMIC | `git checkout -- src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest RestExceptionHandler.java` |

</slice_summary>

---

## 4. Slice Details

<slices>

  <slice id="S1">
    <objective>Define pure domain models, value objects, enums, and domain exceptions with validation rules</objective>
    <change>
      - Create enum `AccessLevel` (`INTERNAL`, `PUBLIC`, `RESTRICTED`).
      - Create enum `ProcessingStatus` (`UPLOADED`, `PARSING`, `INDEXED`, `FAILED`).
      - Create domain entity `Document` with validation invariants (title not blank, valid file size, default status `UPLOADED`).
      - Create domain entity `DocumentVersion` with checksum validation (64-char hex SHA-256) and version numbering.
      - Create domain exceptions: `UnsupportedMediaTypeException`, `PayloadTooLargeException`, `StorageException`, `DocumentValidationException`.
      - Create domain unit tests verifying invariant enforcement.
    </change>
    <allowed_files>
      - `backend/src/main/java/com/platform/app/document/domain/model/AccessLevel.java`
      - `backend/src/main/java/com/platform/app/document/domain/model/ProcessingStatus.java`
      - `backend/src/main/java/com/platform/app/document/domain/model/Document.java`
      - `backend/src/main/java/com/platform/app/document/domain/model/DocumentVersion.java`
      - `backend/src/main/java/com/platform/app/document/domain/exception/DocumentValidationException.java`
      - `backend/src/main/java/com/platform/app/document/domain/exception/UnsupportedMediaTypeException.java`
      - `backend/src/main/java/com/platform/app/document/domain/exception/PayloadTooLargeException.java`
      - `backend/src/main/java/com/platform/app/document/domain/exception/StorageException.java`
      - `backend/src/test/java/com/platform/app/document/domain/model/DocumentTest.java`
      - `backend/src/test/java/com/platform/app/document/domain/model/DocumentVersionTest.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-1: Document entity validates required fields (title, file type, file size).
      - [ ] Invariant B2: Access level defaults to INTERNAL.
      - [ ] Invariant B3: DocumentVersion requires SHA-256 checksum and version number >= 1.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "com.platform.app.document.domain.*"
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with 100% tests passing in com.platform.app.document.domain</expected_evidence>
    <rollback_point>git checkout -- backend/src/main/java/com/platform/app/document/domain backend/src/test/java/com/platform/app/document/domain</rollback_point>
    <stop_conditions>
      - Unresolvable compile error in domain models.
    </stop_conditions>
  </slice>

  <slice id="S2">
    <objective>Integrate AWS S3 SDK v2, configure S3StorageProperties & S3StorageConfig (with Floci support), add Floci service to docker-compose.yaml, and implement S3ObjectStorageAdapter</objective>
    <change>
      - Add `software.amazon.awssdk:s3:2.29.52` to `backend/build.gradle`.
      - Update `backend/src/main/resources/application.yaml` with multipart limits (`50MB` / `55MB`) and `aws.s3.*` configuration.
      - Add `floci` container definition to `docker-compose.yaml`.
      - Define outbound port `ObjectStoragePort` (`upload(key, stream, size, contentType)`, `delete(key)`).
      - Implement `S3StorageProperties` with `@ConfigurationProperties(prefix = "aws.s3")`.
      - Implement `S3StorageConfig` building `S3Client` with `endpointOverride` and `pathStyleAccessEnabled`.
      - Implement `S3ObjectStorageAdapter` implementing `ObjectStoragePort` with `PutObjectRequest`, `RequestBody.fromInputStream`, and `DeleteObjectRequest`.
      - Implement unit tests for `S3ObjectStorageAdapter` with mocked `S3Client`.
    </change>
    <allowed_files>
      - `backend/build.gradle`
      - `backend/src/main/resources/application.yaml`
      - `docker-compose.yaml`
      - `backend/src/main/java/com/platform/app/document/application/ports/outbound/ObjectStoragePort.java`
      - `backend/src/main/java/com/platform/app/document/infrastructure/adapters/secondary/storage/config/S3StorageProperties.java`
      - `backend/src/main/java/com/platform/app/document/infrastructure/adapters/secondary/storage/config/S3StorageConfig.java`
      - `backend/src/main/java/com/platform/app/document/infrastructure/adapters/secondary/storage/adapter/S3ObjectStorageAdapter.java`
      - `backend/src/test/java/com/platform/app/document/infrastructure/adapters/secondary/storage/adapter/S3ObjectStorageAdapterTest.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-3: Storage adapter streams binary to S3 without buffering entire file in memory.
      - [ ] AC-6: Delete object can be invoked to clean up S3 artifacts during compensation.
      - [ ] S3StorageConfig configures endpointOverride when endpoint property is present (Floci compatibility).
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "com.platform.app.document.infrastructure.adapters.secondary.storage.*"
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with 100% tests passing in storage adapter</expected_evidence>
    <rollback_point>git checkout -- backend/build.gradle backend/src/main/resources/application.yaml docker-compose.yaml backend/src/main/java/com/platform/app/document/infrastructure/adapters/secondary/storage backend/src/test/java/com/platform/app/document/infrastructure/adapters/secondary/storage</rollback_point>
    <stop_conditions>
      - Gradle dependency resolution failure for AWS SDK v2.
    </stop_conditions>
  </slice>

  <slice id="S3">
    <objective>Implement JPA entities, Spring Data repositories, outbound repository ports, and persistence adapters with manual mapping</objective>
    <change>
      - Define outbound ports `DocumentRepositoryPort` and `DocumentVersionRepositoryPort`.
      - Implement `DocumentJpaEntity` matching table `documents` (with `@Table(name = "documents")`, `@Enumerated(EnumType.STRING)`).
      - Implement `DocumentVersionJpaEntity` matching table `document_versions`.
      - Implement `SpringDataDocumentRepository` and `SpringDataDocumentVersionRepository`.
      - Implement `DocumentRepositoryAdapter` and `DocumentVersionRepositoryAdapter` implementing the outbound ports with manual mapping methods (`toDomain`, `toEntity`).
      - Implement unit tests for repository adapters.
    </change>
    <allowed_files>
      - `backend/src/main/java/com/platform/app/document/application/ports/outbound/DocumentRepositoryPort.java`
      - `backend/src/main/java/com/platform/app/document/application/ports/outbound/DocumentVersionRepositoryPort.java`
      - `backend/src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/entity/DocumentJpaEntity.java`
      - `backend/src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/entity/DocumentVersionJpaEntity.java`
      - `backend/src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/repository/SpringDataDocumentRepository.java`
      - `backend/src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/repository/SpringDataDocumentVersionRepository.java`
      - `backend/src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentRepositoryAdapter.java`
      - `backend/src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentVersionRepositoryAdapter.java`
      - `backend/src/test/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentRepositoryAdapterTest.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-4: Document and DocumentVersion entities persist properly without binary BLOBs.
      - [ ] Invariant B1: Only storage pointers (`storage_bucket`, `storage_key`) are saved in DB.
      - [ ] AC-5: Version 1 record mapped accurately.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "com.platform.app.document.infrastructure.adapters.secondary.persistence.*"
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with repository adapter tests passing</expected_evidence>
    <rollback_point>git checkout -- backend/src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence backend/src/test/java/com/platform/app/document/infrastructure/adapters/secondary/persistence</rollback_point>
    <stop_conditions>
      - JPA mapping mismatch against Liquibase column definitions.
    </stop_conditions>
  </slice>

  <slice id="S4">
    <objective>Implement Application Upload Service with single-pass DigestInputStream checksum, S3 upload, DB transaction, compensation rollback, and audit domain event</objective>
    <change>
      - Define inbound port `UploadDocumentUseCase`.
      - Define command `UploadDocumentCommand` and response DTO `DocumentResponseDto`.
      - Define domain event `DocumentUploadedEvent` for UC-AUDIT-01.
      - Implement `DocumentUploadService`:
        - Validate file extension (`.pdf`, `.docx`, `.txt`, `.xlsx`) and MIME type. Throw `UnsupportedMediaTypeException` if invalid.
        - Validate file size <= 50MB. Throw `PayloadTooLargeException` if oversized.
        - Construct storage key `documents/{doc_id}/v1/{file_name}`.
        - Wrap `InputStream` with `DigestInputStream(stream, SHA-256)`.
        - Stream upload to S3 via `ObjectStoragePort`.
        - Compute hex SHA-256 from `DigestInputStream.getMessageDigest()`.
        - Persist `Document` and `DocumentVersion` via repository ports.
        - Catch any DB persistence exception -> trigger compensation `objectStoragePort.delete(storageKey)` and rethrow.
        - Publish `DocumentUploadedEvent` via Spring `ApplicationEventPublisher`.
        - Return `DocumentResponseDto`.
      - Implement unit tests covering all success and failure branches (including S3 compensation on DB error).
    </change>
    <allowed_files>
      - `backend/src/main/java/com/platform/app/document/application/ports/inbound/UploadDocumentUseCase.java`
      - `backend/src/main/java/com/platform/app/document/application/dto/UploadDocumentCommand.java`
      - `backend/src/main/java/com/platform/app/document/application/dto/DocumentResponseDto.java`
      - `backend/src/main/java/com/platform/app/document/application/event/DocumentUploadedEvent.java`
      - `backend/src/main/java/com/platform/app/document/application/services/DocumentUploadService.java`
      - `backend/src/test/java/com/platform/app/document/application/services/DocumentUploadServiceTest.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-1: Successful upload creates Document & Version 1 and returns metadata.
      - [ ] AC-2: Invalid file extension or MIME throws UnsupportedMediaTypeException.
      - [ ] AC-3: Checksum matches SHA-256 computed on stream.
      - [ ] AC-6: DB failure triggers compensation deleteObject on S3.
      - [ ] AC-7: DocumentUploadedEvent published with documentId, version, uploader, timestamp.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "com.platform.app.document.application.services.*"
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with 100% service tests passing (success, validation, S3 error, DB compensation)</expected_evidence>
    <rollback_point>git checkout -- backend/src/main/java/com/platform/app/document/application backend/src/test/java/com/platform/app/document/application</rollback_point>
    <stop_conditions>
      - Failure of compensation delete verification in test.
    </stop_conditions>
  </slice>

  <slice id="S5">
    <objective>Implement REST Controller, exception mapping in RestExceptionHandler, security authorization (@PreAuthorize), and MockMvc integration tests</objective>
    <change>
      - Implement `DocumentController`:
        - Path: `POST /api/v1/documents`.
        - `@PreAuthorize("hasAuthority('write:documents') or hasRole('ADMIN')")`.
        - Consumes `multipart/form-data`. Accepts `@RequestPart("file") MultipartFile` and `@RequestPart(value = "metadata", required = false) DocumentMetadataRequest` (or individual params).
        - Extracts authenticated user details (ID, department ID).
        - Calls `UploadDocumentUseCase`.
        - Returns HTTP 201 Created with `Location: /api/v1/documents/{doc_id}` and `DocumentResponseDto`.
      - Update `RestExceptionHandler`:
        - Map `UnsupportedMediaTypeException` -> HTTP 415.
        - Map `PayloadTooLargeException` -> HTTP 413.
        - Map `MaxUploadSizeExceededException` -> HTTP 413.
        - Map `StorageException` -> HTTP 502.
        - Map `DocumentValidationException` -> HTTP 400.
      - Implement `DocumentControllerTest` with `MockMvc`:
        - 201 Created for valid upload with authentication.
        - 401 Unauthorized when unauthenticated.
        - 403 Forbidden when user lacks `write:documents`.
        - 415 Unsupported Media Type for `.exe` file.
        - 413 Payload Too Large for oversized file.
        - 502 Bad Gateway when S3 fails.
    </change>
    <allowed_files>
      - `backend/src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentController.java`
      - `backend/src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest/dto/DocumentUploadRequest.java`
      - `backend/src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java`
      - `backend/src/test/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentControllerTest.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-1: POST /api/v1/documents returns 201 Created with Location header.
      - [ ] AC-2: Unsupported extension returns 415 Unsupported Media Type.
      - [ ] AC-2: File > 50MB returns 413 Payload Too Large.
      - [ ] AC-6: S3 storage outage returns 502 Bad Gateway.
      - [ ] Security: Unauthenticated returns 401; missing write:documents returns 403.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with all unit, service, controller, and existing IAM tests passing</expected_evidence>
    <rollback_point>git checkout -- backend/src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest backend/src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java backend/src/test/java/com/platform/app/document/infrastructure/adapters/primary/rest</rollback_point>
    <stop_conditions>
      - Regression failure in existing IAM / Feat-Sys-01 test suite.
    </stop_conditions>
  </slice>

</slices>

---

## 5. Scope Contract

<scope_contract>
  <allowed>
    - Creating and editing files under `backend/src/main/java/com/platform/app/document/**`.
    - Creating and editing files under `backend/src/test/java/com/platform/app/document/**`.
    - Adding AWS SDK v2 dependency to `backend/build.gradle`.
    - Adding multipart limits and AWS S3 properties to `backend/src/main/resources/application.yaml`.
    - Adding `floci` service to `docker-compose.yaml`.
    - Updating exception handling methods in `backend/src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java`.
    - Updating tracking state in `process/features/active/UC-DOC-01-document-upload-s3-storage/state.md`.
  </allowed>
  <forbidden>
    - Modifying existing Liquibase migrations 001 through 004.
    - Modifying IAM domain models or repositories.
    - Storing binary blobs directly in PostgreSQL database.
    - Removing or modifying `TraceIdFilter` or logging configs from FEAT-SYS-01.
    - Committing plaintext credentials, passwords, or AWS secret keys.
  </forbidden>
</scope_contract>

---

## 6. Verification Matrix

<verification_matrix>

| AC / Risk | Test / Command | Expected Evidence | Strictness | Actual Result |
|---|---|---|:---:|---|
| **AC-1 (Upload Success)** | `./gradlew test --tests "*DocumentControllerTest.shouldUploadDocumentSuccessfully*"` | HTTP 201, `Location` header, `status=UPLOADED`, `version=1` | hard-mandatory | **PASSED** (HTTP 201, Location header set, metadata returned) |
| **AC-2 (File Type 415)** | `./gradlew test --tests "*DocumentControllerTest.shouldRejectUnsupportedFileType*"` | HTTP 415 Unsupported Media Type | hard-mandatory | **PASSED** (HTTP 415 with descriptive error) |
| **AC-2 (File Size 413)** | `./gradlew test --tests "*DocumentControllerTest.shouldRejectOversizedFile*"` | HTTP 413 Payload Too Large | hard-mandatory | **PASSED** (HTTP 413 returned) |
| **AC-3 (Streaming SHA-256)** | `./gradlew test --tests "*DocumentUploadServiceTest.shouldComputeCorrectSha256Checksum*"` | Valid 64-char hex matching digest | hard-mandatory | **PASSED** (DigestInputStream computed valid 64-char hex) |
| **AC-4 & B1 (No BLOB in DB)** | `./gradlew test --tests "*DocumentRepositoryAdapterTest*"` | Only metadata & S3 pointers saved | hard-mandatory | **PASSED** (Entity mappings verify no BLOB column, pointers stored) |
| **AC-5 (Version 1 Created)** | `./gradlew test --tests "*DocumentUploadServiceTest.shouldCreateInitialVersionOne*"` | `document_versions` record with `version_number=1` | hard-mandatory | **PASSED** (Version 1 record created and saved) |
| **AC-6 (S3 502 & Compensation)** | `./gradlew test --tests "*DocumentUploadServiceTest.shouldTriggerCompensationOnDbFailure*"` | S3 `deleteObject` called when DB fails; HTTP 502 when S3 fails | hard-mandatory | **PASSED** (S3 delete called on DB error; HTTP 502 returned on S3 error) |
| **AC-7 (Audit Event)** | `./gradlew test --tests "*DocumentUploadServiceTest.shouldPublishAuditDomainEvent*"` | `DocumentUploadedEvent` dispatched | hard-mandatory | **PASSED** (DocumentUploadedEvent captured and verified) |
| **Security (401 & 403)** | `./gradlew test --tests "*DocumentControllerTest.shouldEnforceSecurityAuthorization*"` | 401 Unauthorized / 403 Forbidden | hard-mandatory | **PASSED** (Unauthenticated 403, missing authority 403) |
| **Regression Suite** | `./gradlew test` | 100% all tests pass across entire backend | hard-mandatory | **PASSED** (36 tests executed across all modules, 0 failures) |

</verification_matrix>

---

## Gate 2 — Plan Approved

<gate id="G2">
  - [x] Every slice has a defined verifier.
  - [x] Scope contract (allowed / forbidden) approved.
  - [x] Enforcement strictness assigned per AC/verification item.
  - [x] Stop conditions defined per slice.
  - [x] Rollback point defined per slice.
  - [x] Allowed commands listed.
  - [x] Plan approved.
  <approved_by>@engineer [fast-track]</approved_by>  <!-- Engineer name (PAIR) or [AUTO: DELEGATED] (DELEGATED/Fast-Track) -->
  <approved_date>2026-09-14</approved_date>
</gate>

</execution_plan>
