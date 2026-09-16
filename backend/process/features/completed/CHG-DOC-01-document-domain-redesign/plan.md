# Plan: PLAN-DOC-01 Document Domain Model & Schema Redesign

<execution_plan task_id="CHG-DOC-01" plan_id="PLAN-DOC-01" version="2.0" framework="RIPER-5">

<!-- PLAN PHASE. Plan artifacts only. No source-code changes. -->
<plan_status>
  <phase>PLAN</phase>
  <last_updated>2026-09-16</last_updated>
</plan_status>

---

## 1. Input Artifacts

<input_artifacts>
  <task_spec>process/features/active/CHG-DOC-01-document-domain-redesign/task.md</task_spec>
  <decision>process/features/active/CHG-DOC-01-document-domain-redesign/decision.md (DEC-DOC-02, Option A Approved)</decision>
</input_artifacts>

---

## 2. Execution Constraints

<execution_constraints>
  <allowed_files>
    - `src/main/java/com/platform/app/document/domain/model/Document.java` — Core domain aggregate
    - `src/main/java/com/platform/app/document/domain/model/DocumentStatus.java` — Clean lifecycle enum replacing ProcessingStatus
    - `src/main/java/com/platform/app/document/domain/model/ProcessingStatus.java` — Clean up / replace
    - `src/main/resources/db/changelog/changes/003-create-document-tables.yaml` — Streamlined documents table definition
    - `docs/specs/database/schema.dbml` — Master database schema specification
    - `docs/specs/database/modules/02_document_management.dbml` — Module database schema specification
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/entity/DocumentJpaEntity.java` — JPA entity
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentRepositoryAdapter.java` — Entity/Domain mapper
    - `src/main/java/com/platform/app/document/application/dto/DocumentResponseDto.java` — Output DTO
    - `src/main/java/com/platform/app/document/application/dto/UploadDocumentCommand.java` — Input command
    - `src/main/java/com/platform/app/document/application/event/DocumentUploadedEvent.java` — Domain audit event
    - `src/main/java/com/platform/app/document/application/ports/inbound/StoreMetadataUseCase.java` — Inbound port signature
    - `src/main/java/com/platform/app/document/application/services/DocumentMetadataService.java` — Metadata persistence service
    - `src/main/java/com/platform/app/document/application/services/DocumentUploadService.java` — Upload orchestrator
    - `src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentController.java` — REST controller
    - `src/test/java/com/platform/app/document/**` — Unit and slice tests
  </allowed_files>
  <forbidden_files>
    - `src/main/java/com/platform/app/iam/**` — IAM subsystem is strictly out of scope
    - `src/main/java/com/platform/app/audit/**` — Audit subsystem is strictly out of scope
    - `src/main/resources/db/changelog/changes/001-create-iam-tables.yaml` — Frozen
    - `src/main/resources/db/changelog/changes/002-create-departments-table.yaml` — Frozen
    - `src/main/resources/db/changelog/changes/004-create-audit-tables.yaml` — Frozen
  </forbidden_files>
  <allowed_commands>
    - `./gradlew test --tests "*Document*"`
    - `./gradlew test`
    - `./gradlew spotlessApply`
    - `./gradlew spotlessCheck`
    - `./gradlew check`
  </allowed_commands>
  <restricted_operations>
    - No changes to frozen Liquibase files 001, 002, 004.
    - No new third-party dependencies in build.gradle.
  </restricted_operations>
  <required_approvals>
    - Gate 2 approval by engineer before advancing to EXECUTE mode.
  </required_approvals>
</execution_constraints>

---

## 3. Slice Summary

<slice_summary>

| ID | Behavior | Boundary | Files | AC | Verifier | Risk | Mode | Rollback |
|---|---|---|---|---|---|---|---|---|
| S1 | Core Domain POJO Redesign: 11 fields, DocumentStatus enum, rich state-transition methods | Domain | `Document.java`, `DocumentStatus.java`, `ProcessingStatus.java`, `DocumentTest.java` | AC-1 | `./gradlew test --tests "*DocumentTest*"` | LOW | READ/WRITE | `git checkout -- src/main/java/com/platform/app/document/domain/` |
| S2 | Liquibase Schema & JPA Entity Redesign: 11 columns, clean indexes, repository adapter mapping | Infrastructure / Persistence | `003-create-document-tables.yaml`, `DocumentJpaEntity.java`, `DocumentRepositoryAdapter.java`, `DocumentRepositoryAdapterTest.java` | AC-2 | `./gradlew test --tests "*DocumentRepositoryAdapterTest*"` | MEDIUM | READ/WRITE | `git checkout -- src/main/resources/db/changelog/changes/003-create-document-tables.yaml src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/` |
| S3 | Application Ports, Services, Event & REST Controller Synchronization | Application & REST | `DocumentResponseDto.java`, `DocumentUploadedEvent.java`, `StoreMetadataUseCase.java`, `DocumentMetadataService.java`, `DocumentUploadService.java`, `DocumentController.java`, tests | AC-3, AC-4 | `./gradlew test --tests "*DocumentUploadServiceTest*" --tests "*DocumentControllerTest*"` | MEDIUM | READ/WRITE | `git checkout -- src/main/java/com/platform/app/document/application/` |
| S4 | Full Regression & Code Quality Gate | Platform | Full test suite | AC-5, AC-6 | `./gradlew check` | LOW | READ-ONLY (commands) | Fix formatting / compile errors |

</slice_summary>

---

## 4. Slice Details

<slices>

  <slice id="S1">
    <objective>Redesign Document domain model to exactly 11 core concepts with clean invariants</objective>
    <change>
      - Create `DocumentStatus` (UPLOADED, PROCESSING, READY, FAILED) and replace `ProcessingStatus`.
      - Trim `Document.java` to: `id`, `title`, `originalFileName`, `contentType`, `fileSizeBytes`, `checksumSha256`, `storageKey`, `status`, `uploadedByUserId`, `departmentId`, `accessLevel`, `createdAt`, `updatedAt`.
      - Remove: `fileType`, `storageBucket`, `isS3Synced`, `metadata`, `description`, `currentVersion`, `deletedAt`.
      - Add domain methods: `markProcessing()`, `markReady()`, `markFailed()`.
      - Update `DocumentTest.java` to test invariants on the 11 core fields.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/document/domain/model/Document.java`
      - `src/main/java/com/platform/app/document/domain/model/DocumentStatus.java`
      - `src/main/java/com/platform/app/document/domain/model/ProcessingStatus.java`
      - `src/test/java/com/platform/app/document/domain/model/DocumentTest.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-1: Document.java contains only the 11 core comprehension fields and passes all domain invariant tests.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "*DocumentTest*"
      ```
    </verifier>
    <expected_evidence>All tests in DocumentTest pass successfully.</expected_evidence>
    <rollback_point>git checkout -- src/main/java/com/platform/app/document/domain/ src/test/java/com/platform/app/document/domain/</rollback_point>
    <stop_conditions>
      - Invariant conflict or failure to compile domain model.
    </stop_conditions>
  </slice>

  <slice id="S2">
    <objective>Streamline Liquibase schema, DocumentJpaEntity, and DocumentRepositoryAdapter</objective>
    <change>
      - Update `003-create-document-tables.yaml` changeSet `003-create-documents-table` to drop unused columns (`file_type`, `mime_type` replaced by `content_type`, `storage_bucket`, `is_s3_synced`, `metadata`, `description`, `current_version`, `deleted_at`).
      - Drop unnecessary GIN index on metadata.
      - Update `DocumentJpaEntity.java` with exact 11 fields matching the database schema.
      - Update `DocumentRepositoryAdapter.java` entity-domain bidirectional mapping.
      - Update `DocumentRepositoryAdapterTest.java`.
    </change>
    <allowed_files>
      - `src/main/resources/db/changelog/changes/003-create-document-tables.yaml`
      - `docs/specs/database/schema.dbml`
      - `docs/specs/database/modules/02_document_management.dbml`
      - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/entity/DocumentJpaEntity.java`
      - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentRepositoryAdapter.java`
      - `src/test/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentRepositoryAdapterTest.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-2: Schema and entity reflect the streamlined 11 columns and repository adapter persists and reconstructs domain models cleanly.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "*DocumentRepositoryAdapterTest*"
      ```
    </verifier>
    <expected_evidence>Liquibase executes cleanly against H2/Postgres and adapter tests pass.</expected_evidence>
    <rollback_point>git checkout -- src/main/resources/db/changelog/changes/003-create-document-tables.yaml src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/ src/test/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/</rollback_point>
    <stop_conditions>
      - Liquibase parsing failure or SQL dialect incompatibilities.
    </stop_conditions>
  </slice>

  <slice id="S3">
    <objective>Synchronize Application Services, Ports, Event, DTOs, and REST Controller</objective>
    <change>
      - Update `DocumentResponseDto.java`: remove unused fields, retain 11 core attributes.
      - Update `DocumentUploadedEvent.java`: align with core document properties.
      - Update `StoreMetadataUseCase.java` and `DocumentMetadataService.java`: persist clean Document without redundant document_versions duplicate inserts.
      - Update `DocumentUploadService.java`: pass contentType and streamlined parameters.
      - Update `DocumentController.java`, `DocumentUploadServiceTest.java`, and `DocumentControllerTest.java`.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/document/application/dto/DocumentResponseDto.java`
      - `src/main/java/com/platform/app/document/application/dto/UploadDocumentCommand.java`
      - `src/main/java/com/platform/app/document/application/event/DocumentUploadedEvent.java`
      - `src/main/java/com/platform/app/document/application/ports/inbound/StoreMetadataUseCase.java`
      - `src/main/java/com/platform/app/document/application/services/DocumentMetadataService.java`
      - `src/main/java/com/platform/app/document/application/services/DocumentUploadService.java`
      - `src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentController.java`
      - `src/test/java/com/platform/app/document/application/services/DocumentUploadServiceTest.java`
      - `src/test/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentControllerTest.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-3: Application services and DTOs synchronized cleanly with domain model.
      - [ ] AC-4: S3 streaming upload, checksum calculation, and compensation rollback remain intact.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "*DocumentUploadServiceTest*" --tests "*DocumentControllerTest*"
      ```
    </verifier>
    <expected_evidence>All service and controller tests pass with HTTP 201 Created and expected DTO output.</expected_evidence>
    <rollback_point>git checkout -- src/main/java/com/platform/app/document/application/ src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest/ src/test/java/com/platform/app/document/</rollback_point>
    <stop_conditions>
      - Service compilation error or broken S3 upload flow.
    </stop_conditions>
  </slice>

  <slice id="S4">
    <objective>Full Regression & Code Quality Gate</objective>
    <change>
      - Run spotlessApply and full test suite.
    </change>
    <allowed_files>
      - All modified files
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-5: All automated unit and integration tests pass cleanly.
      - [ ] AC-6: Spotless code formatting passes.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew check
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with 0 failures across all test suites.</expected_evidence>
    <rollback_point>Re-inspect git diff and apply targeted fixes.</rollback_point>
    <stop_conditions>
      - Failure on any unrelated subsystem test.
    </stop_conditions>
  </slice>

</slices>

---

## 5. Scope Contract

<scope_contract>
  <allowed>
    - Refactoring `com.platform.app.document` domain models, JPA entity, repository adapter, application services, DTOs, and REST controller.
    - Updating Liquibase migration `003-create-document-tables.yaml` for `documents` table definition.
    - Updating database specification files `docs/specs/database/schema.dbml` and `docs/specs/database/modules/02_document_management.dbml`.
    - Updating related test files under `src/test/java/com/platform/app/document/`.
  </allowed>
  <forbidden>
    - Modifying IAM subsystem files (`com.platform.app.iam`).
    - Modifying Audit subsystem files (`com.platform.app.audit`).
    - Modifying Liquibase migrations 001, 002, 004.
    - Adding new dependencies to `build.gradle`.
  </forbidden>
</scope_contract>

---

## 6. Verification Matrix

<verification_matrix>

| AC / Risk | Test / Command | Expected Evidence | Strictness | Actual Result |
|---|---|---|---|---|
| AC-1: Domain Model Invariants | `./gradlew test --tests "*DocumentTest*"` | 100% tests pass | hard-mandatory | pending |
| AC-2: Schema & Adapter Persistence | `./gradlew test --tests "*DocumentRepositoryAdapterTest*"` | 100% tests pass | hard-mandatory | pending |
| AC-3: Services & S3 Flow | `./gradlew test --tests "*DocumentUploadServiceTest*"` | 100% tests pass | hard-mandatory | pending |
| AC-4: REST Controller API | `./gradlew test --tests "*DocumentControllerTest*"` | HTTP 201 Created and valid JSON response | hard-mandatory | pending |
| AC-5: Full Regression | `./gradlew test` | 123+ tests pass | hard-mandatory | pending |
| AC-6: Code Formatting | `./gradlew spotlessCheck` | No format violations | hard-mandatory | pending |

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
  <approved_by>@engineer [FAST-TRACK]</approved_by>
  <approved_date>2026-09-16</approved_date>
</gate>

</execution_plan>
