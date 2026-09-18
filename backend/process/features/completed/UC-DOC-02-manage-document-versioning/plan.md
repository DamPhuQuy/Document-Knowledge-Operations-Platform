# Plan: PLAN-DOC-02 Manage Document Versioning

<execution_plan task_id="UC-DOC-02" plan_id="PLAN-DOC-02" version="2.0" framework="RIPER-5">

<!-- PLAN PHASE. Plan artifacts only. No source-code changes. -->
<plan_status>
  <phase>PLAN</phase>
  <last_updated>2026-09-16</last_updated>
</plan_status>

---

## 1. Input Artifacts

<input_artifacts>
  <task_spec>process/features/active/UC-DOC-02-manage-document-versioning/task.md</task_spec>
  <research>process/features/active/UC-DOC-02-manage-document-versioning/research.md</research>
  <decision>process/features/active/UC-DOC-02-manage-document-versioning/decision.md (DEC-DOC-03: Option A)</decision>
</input_artifacts>

---

## 2. Execution Constraints

<execution_constraints>
  <allowed_files>
    - `src/main/java/com/platform/app/document/domain/**`
    - `src/main/java/com/platform/app/document/application/**`
    - `src/main/java/com/platform/app/document/infrastructure/**`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java`
    - `src/main/resources/db/changelog/changes/003-create-document-tables.yaml`
    - `../docs/specs/database/**`
    - `src/test/java/com/platform/app/document/**`
  </allowed_files>
  <forbidden_files>
    - `src/main/java/com/platform/app/iam/domain/**`
    - `src/main/java/com/platform/app/iam/application/**`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/**`
    - `src/main/resources/db/changelog/changes/001-*`
    - `src/main/resources/db/changelog/changes/002-*`
    - `src/main/resources/db/changelog/changes/004-*`
  </forbidden_files>
  <allowed_commands>
    - `./gradlew compileJava compileTestJava`
    - `./gradlew test --tests ...`
    - `./gradlew test`
    - `./gradlew spotlessApply`
    - `./gradlew check`
  </allowed_commands>
  <restricted_operations>
    - Pure POJO Domain Model: No framework or AWS SDK dependencies inside `com.platform.app.document.domain`.
    - No changes outside allowed paths.
  </restricted_operations>
</execution_constraints>

---

## 3. Slice Summary

<slice_summary>

| ID | Behavior | Boundary | Files | AC | Verifier | Risk | Mode | Rollback |
|---|---|---|---|---|---|---|---|---|
| S1 | Add `currentVersion` & `applyNewVersion` to `Document`, update Entity, DDL, DBML | Domain, DB, Entity | `Document.java`, `DocumentJpaEntity.java`, `003-create-document-tables.yaml`, DBML, tests | AC-1, AC-3 | `./gradlew test --tests "*DocumentTest*" --tests "*DocumentRepositoryAdapterTest*"` | LOW | READ/WRITE | `git checkout -- src/main/resources/db/changelog/changes/003-create-document-tables.yaml src/main/java/com/platform/app/document/domain/model/Document.java` |
| S2 | Define Ports, Commands, DTOs & Events for version upload | Application Ports & DTOs | `UploadDocumentVersionUseCase.java`, `UploadDocumentVersionCommand.java`, `DocumentVersionResponseDto.java`, `DocumentVersionCreatedEvent.java` | AC-1, AC-7 | `./gradlew compileJava compileTestJava` | LOW | READ/WRITE | `git clean -fd src/main/java/com/platform/app/document/application/` |
| S3 | Implement `DocumentVersionService` with validation, ownership check, S3 streaming, compensation, and events | Application Service | `DocumentVersionService.java`, `DocumentVersionServiceTest.java` | AC-1, AC-2, AC-3, AC-4, AC-5, AC-6, AC-7 | `./gradlew test --tests "*DocumentVersionServiceTest*"` | MEDIUM | READ/WRITE | `rm -f src/main/java/com/platform/app/document/application/services/DocumentVersionService.java` |
| S4 | Expose `POST /api/v1/documents/{id}/versions` endpoint, configure `RestExceptionHandler`, integration tests | Primary Adapter & Error Handling | `DocumentController.java`, `RestExceptionHandler.java`, `DocumentControllerTest.java` | AC-1, AC-4, AC-5, AC-6, AC-8 | `./gradlew test --tests "*DocumentControllerTest*" && ./gradlew check` | LOW | READ/WRITE | `git checkout -- src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest/` |

</slice_summary>

---

## 4. Slice Details

<slices>

  <slice id="S1">
    <objective>Incorporate `currentVersion` attribute into `Document` domain model and persistence schema</objective>
    <change>
      - Update `Document.java`: add `currentVersion` (int, default 1), add method `applyNewVersion(int newVersion, String storageKey, String checksumSha256, long fileSizeBytes, String contentType, String originalFileName)`.
      - Add `DocumentNotFoundException.java` and `DocumentAccessDeniedException.java` in `document.domain.exception`.
      - Update `DocumentJpaEntity.java`: add `current_version` column.
      - Update `003-create-document-tables.yaml`: add `current_version` column with defaultValue="1" to `documents` table.
      - Update `schema.dbml` and `02_document_management.dbml`.
      - Update existing `DocumentTest.java` and `DocumentRepositoryAdapterTest.java` to verify mapping and invariants.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/document/domain/model/Document.java`
      - `src/main/java/com/platform/app/document/domain/exception/DocumentNotFoundException.java`
      - `src/main/java/com/platform/app/document/domain/exception/DocumentAccessDeniedException.java`
      - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/entity/DocumentJpaEntity.java`
      - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentRepositoryAdapter.java`
      - `src/main/resources/db/changelog/changes/003-create-document-tables.yaml`
      - `../docs/specs/database/schema.dbml`
      - `../docs/specs/database/modules/02_document_management.dbml`
      - `src/test/java/com/platform/app/document/domain/model/DocumentTest.java`
      - `src/test/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentRepositoryAdapterTest.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-1: Document aggregate root tracks currentVersion and encapsulates applyNewVersion.
      - [ ] AC-3: JPA entity and Liquibase 003 persist current_version.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "*DocumentTest*" --tests "*DocumentRepositoryAdapterTest*"
      ```
    </verifier>
    <expected_evidence>Tests execute and pass with 0 failures.</expected_evidence>
    <rollback_point>git checkout -- src/main/java/com/platform/app/document/domain/model/Document.java</rollback_point>
  </slice>

  <slice id="S2">
    <objective>Define Inbound Port, Command, DTO, and Domain Event for document version upload</objective>
    <change>
      - Create `UploadDocumentVersionUseCase.java` inbound port: `DocumentVersionResponseDto uploadVersion(UploadDocumentVersionCommand command)`.
      - Create `UploadDocumentVersionCommand.java` with docId, userId, inputStream, originalFileName, contentType, fileSize, changeSummary, isAdmin.
      - Create `DocumentVersionResponseDto.java` containing version snapshot metadata.
      - Create `DocumentVersionCreatedEvent.java` domain event.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/document/application/ports/inbound/UploadDocumentVersionUseCase.java`
      - `src/main/java/com/platform/app/document/application/dto/UploadDocumentVersionCommand.java`
      - `src/main/java/com/platform/app/document/application/dto/DocumentVersionResponseDto.java`
      - `src/main/java/com/platform/app/document/application/event/DocumentVersionCreatedEvent.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-1: Inbound contracts and DTOs established.
      - [ ] AC-7: Event payload defined.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew compileJava compileTestJava
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with all new classes compiled cleanly.</expected_evidence>
    <rollback_point>git clean -fd src/main/java/com/platform/app/document/application/</rollback_point>
  </slice>

  <slice id="S3">
    <objective>Implement DocumentVersionService orchestrating version uploads with S3 streaming, atomic DB save, S3 compensation, and events</objective>
    <change>
      - Create `DocumentVersionService.java` implementing `UploadDocumentVersionUseCase`:
        1. Validates file presence, extension, size <= 50MB.
        2. Retrieves parent `Document` from `DocumentRepositoryPort`; if missing throws `DocumentNotFoundException`.
        3. Verifies ownership: uploader is document owner or `command.isAdmin()`. If not, throws `DocumentAccessDeniedException`.
        4. Calculates `nextVersion = document.getCurrentVersion() + 1`.
        5. Computes S3 key: `documents/{docId}/v{nextVersion}/{sanitizedFileName}`.
        6. Streams to S3 via `ObjectStoragePort.upload` wrapping input stream with `DigestInputStream` (SHA-256).
        7. In `@Transactional` block: saves `DocumentVersion` snapshot, applies new version to `Document`, saves updated `Document`.
        8. If DB fails: catches exception and invokes `objectStoragePort.delete(storageKey)` compensation.
        9. Emits `DocumentVersionCreatedEvent`.
        10. Returns `DocumentVersionResponseDto`.
      - Write unit test `DocumentVersionServiceTest.java` verifying all flows (success, 403 ownership rejection, 404 not found, 415 invalid ext, 413 oversized, S3 compensation).
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/document/application/services/DocumentVersionService.java`
      - `src/test/java/com/platform/app/document/application/services/DocumentVersionServiceTest.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-1: Next version calculated and uploaded.
      - [ ] AC-2: S3 key follows `documents/{docId}/v{nextVersion}/{fileName}`.
      - [ ] AC-3: Atomic persistence in document_versions and documents.
      - [ ] AC-4: Non-owner rejected with DocumentAccessDeniedException.
      - [ ] AC-5: Missing document throws DocumentNotFoundException.
      - [ ] AC-6: Unsupported media type / size handled.
      - [ ] AC-7: DocumentVersionCreatedEvent published.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "*DocumentVersionServiceTest*"
      ```
    </verifier>
    <expected_evidence>All service unit tests pass.</expected_evidence>
    <rollback_point>rm -f src/main/java/com/platform/app/document/application/services/DocumentVersionService.java</rollback_point>
  </slice>

  <slice id="S4">
    <objective>Expose REST endpoint `POST /api/v1/documents/{id}/versions` and configure error handling</objective>
    <change>
      - Add endpoint `POST /api/v1/documents/{id}/versions` in `DocumentController.java`:
        - Accepts `id` path variable, `file` multipart part, optional `changeSummary` request param.
        - Checks `@PreAuthorize("hasAuthority('write:documents') or hasAuthority('WRITE:DOCUMENTS') or hasRole('ADMIN')")`.
        - Resolves `isAdmin` flag from Authentication authorities.
        - Delegates to `UploadDocumentVersionUseCase.uploadVersion(...)`.
        - Returns `ResponseEntity.ok(responseDto)` (HTTP 200 OK).
      - Add exception handlers in `RestExceptionHandler.java` for `DocumentNotFoundException` (404) and `DocumentAccessDeniedException` (403).
      - Update `DocumentControllerTest.java` to test:
        - Successful version upload (200 OK).
        - Non-owner without admin receiving 403 Forbidden.
        - Non-existent document receiving 404 Not Found.
        - Unsupported extension receiving 415.
        - Oversized file receiving 413.
      - Format code with spotless and run full check.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentController.java`
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java`
      - `src/test/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentControllerTest.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-1: Endpoint returns HTTP 200 OK with version details.
      - [ ] AC-4: HTTP 403 for unauthorized users.
      - [ ] AC-5: HTTP 404 for missing documents.
      - [ ] AC-8: Full regression `./gradlew check` passes.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "*DocumentControllerTest*" && ./gradlew check
      ```
    </verifier>
    <expected_evidence>Full test suite passes and spotless passes.</expected_evidence>
    <rollback_point>git checkout -- src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentController.java</rollback_point>
  </slice>

</slices>

---

## 5. Scope Contract

<scope_contract>
  <allowed>
    - Add versioning endpoint, use cases, DTOs, service, domain events, exceptions, entity fields, and test suites.
  </allowed>
  <forbidden>
    - Modifying IAM domain models, credentials, or unrelated tables.
    - Storing binary blobs in database.
  </forbidden>
</scope_contract>

---

## Gate 2 — Plan Approved

<gate id="G2">
  - [x] All slices have clear verifier commands and expected evidence.
  - [x] File boundaries and rollback points are explicit.
  - [x] Zero unaddressed edge cases.
  <approved_by>[AUTO: DELEGATED]</approved_by>
  <approved_date>2026-09-16</approved_date>
</gate>

</execution_plan>
