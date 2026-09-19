# Plan: PLAN-DOC-04 UC-DOC-04 Document Soft Deletion

<execution_plan task_id="UC-DOC-04" plan_id="PLAN-DOC-04" version="2.0" framework="RIPER-5">

<!-- PLAN PHASE. Plan artifacts only. No source-code changes. -->
<plan_status>
  <phase>PLAN</phase>
  <last_updated>2026-09-19</last_updated>
</plan_status>

---

## 1. Input Artifacts

<input_artifacts>
  <task_spec>`process/features/active/UC-DOC-04-document-soft-deletion/task.md`</task_spec>
  <research>`process/features/active/UC-DOC-04-document-soft-deletion/research.md`</research>
  <decision>`process/features/active/UC-DOC-04-document-soft-deletion/decision.md` — DEC-DOC-04, Option A</decision>
</input_artifacts>

---

## 2. Execution Constraints

<execution_constraints>
  <allowed_files>
    - `src/main/resources/db/changelog/changes/003-create-document-tables.yaml`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/entity/DocumentJpaEntity.java`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/repository/SpringDataDocumentRepository.java`
    - `src/main/java/com/platform/app/document/application/ports/outbound/DocumentRepositoryPort.java`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentRepositoryAdapter.java`
    - `src/main/java/com/platform/app/document/application/dto/SoftDeleteDocumentCommand.java`
    - `src/main/java/com/platform/app/document/application/event/DocumentSoftDeletedEvent.java`
    - `src/main/java/com/platform/app/document/application/ports/inbound/SoftDeleteDocumentUseCase.java`
    - `src/main/java/com/platform/app/document/application/services/DocumentSoftDeleteService.java`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentController.java`
    - `src/test/java/com/platform/app/document/application/services/DocumentSoftDeleteServiceTest.java`
    - `src/test/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentControllerTest.java`
  </allowed_files>
  <forbidden_files>
    - `src/main/java/com/platform/app/document/domain/model/Document.java` (Kept clean without deletedAt field per Option A decision)
    - `src/main/resources/db/changelog/changes/005-*.yaml` (Forbidden per user directive)
    - `src/main/resources/db/changelog/db.changelog-master.yaml` (No changes needed since 003 is already registered)
    - Any IAM or AI subsystem code
  </forbidden_files>
  <allowed_commands>
    - `./gradlew test`
    - `./gradlew compileJava`
    - `./gradlew check`
    - `git status`
    - `git diff`
  </allowed_commands>
  <restricted_operations>
    - Do not create a separate 005 migration file.
    - Do not add `deletedAt` field to `Document.java`.
    - No physical file deletion on S3 or SQL DELETE commands.
  </restricted_operations>
  <required_approvals>
    - Gate 2 signed with [AUTO: DELEGATED] per fast-track instruction.
  </required_approvals>
</execution_constraints>

---

## 3. Slice Summary

<slice_summary>

| ID | Behavior | Boundary | Files | AC | Verifier | Risk | Mode | Rollback |
|---|---|---|---|---|---|---|---|---|
| S1 | Schema & Entity mapping | DB Liquibase + JPA Entity | `003-create-document-tables.yaml`, `DocumentJpaEntity.java` | AC-1, AC-3 | `./gradlew test` | LOW | Write | `git checkout -- <files>` |
| S2 | Repository Port, Adapter & Query Exclusion | Persistence Adapter | `SpringDataDocumentRepository.java`, `DocumentRepositoryPort.java`, `DocumentRepositoryAdapter.java` | AC-3, AC-9 | `./gradlew test` | LOW | Write | `git checkout -- <files>` |
| S3 | Soft Delete Use Case & Domain Event | Application Service | `SoftDeleteDocumentCommand.java`, `DocumentSoftDeletedEvent.java`, `SoftDeleteDocumentUseCase.java`, `DocumentSoftDeleteService.java`, `DocumentSoftDeleteServiceTest.java` | AC-5, AC-6, AC-8 | `./gradlew test --tests com.platform.app.document.application.services.DocumentSoftDeleteServiceTest` | LOW | Write | `git checkout -- <files>` |
| S4 | REST Endpoint & HTTP Status Verification | REST Controller | `DocumentController.java`, `DocumentControllerTest.java` | AC-4, AC-5, AC-6, AC-7, AC-10 | `./gradlew test` | LOW | Write | `git checkout -- <files>` |

</slice_summary>

---

## 4. Slice Details

<slices>

  <slice id="S1">
    <objective>Add `deleted_at TIMESTAMPTZ` column and index to table `documents` in `003-create-document-tables.yaml` and map it in `DocumentJpaEntity.java`</objective>
    <change>
      - Edit `003-create-document-tables.yaml` changeSet `003-create-documents-table` to include `deleted_at` column and create index `idx_documents_deleted_at`.
      - Edit `DocumentJpaEntity.java` to add `@Column(name = "deleted_at") private Instant deletedAt;`.
    </change>
    <allowed_files>
      - `src/main/resources/db/changelog/changes/003-create-document-tables.yaml`
      - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/entity/DocumentJpaEntity.java`
    </allowed_files>
    <acceptance_criteria>
      - [x] AC-1: Database schema has `deleted_at TIMESTAMPTZ` and index on `documents(deleted_at)`.
      - [x] AC-3: `DocumentJpaEntity` maps `deleted_at` field.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew compileJava
      ./gradlew test
      ```
    </verifier>
    <expected_evidence>Liquibase executes successfully and existing tests continue to pass.</expected_evidence>
    <rollback_point>`git checkout -- src/main/resources/db/changelog/changes/003-create-document-tables.yaml src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/entity/DocumentJpaEntity.java`</rollback_point>
    <stop_conditions>
      - Liquibase fails to validate changeset or Hibernate schema validation fails.
    </stop_conditions>
  </slice>

  <slice id="S2">
    <objective>Enforce soft deletion filtering and add update query in `SpringDataDocumentRepository`, `DocumentRepositoryPort`, and `DocumentRepositoryAdapter`</objective>
    <change>
      - Add `Optional<DocumentJpaEntity> findByIdAndDeletedAtIsNull(UUID id)` and soft-delete `@Modifying` update method in `SpringDataDocumentRepository`.
      - Update `DocumentRepositoryPort` with `findById(UUID id)` (mapping to active-only lookup) and `boolean softDelete(UUID id, Instant deletedAt)`.
      - Update `DocumentRepositoryAdapter` implementing these methods.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/repository/SpringDataDocumentRepository.java`
      - `src/main/java/com/platform/app/document/application/ports/outbound/DocumentRepositoryPort.java`
      - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentRepositoryAdapter.java`
    </allowed_files>
    <acceptance_criteria>
      - [x] AC-3: Persistence adapter executes soft delete.
      - [x] AC-9: Standard `findById` returns empty when document has `deleted_at IS NOT NULL`.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test
      ```
    </verifier>
    <expected_evidence>All existing repository tests and service tests pass.</expected_evidence>
    <rollback_point>`git checkout -- src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/`</rollback_point>
    <stop_conditions>
      - Any breakage in existing document query tests.
    </stop_conditions>
  </slice>

  <slice id="S3">
    <objective>Create inbound port, command, domain event, and application service for document soft deletion</objective>
    <change>
      - Create `SoftDeleteDocumentCommand.java`, `DocumentSoftDeletedEvent.java`, and `SoftDeleteDocumentUseCase.java`.
      - Implement `DocumentSoftDeleteService.java` orchestrating validation, authorization, persistence, and audit event dispatch.
      - Create comprehensive unit test `DocumentSoftDeleteServiceTest.java`.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/document/application/dto/SoftDeleteDocumentCommand.java`
      - `src/main/java/com/platform/app/document/application/event/DocumentSoftDeletedEvent.java`
      - `src/main/java/com/platform/app/document/application/ports/inbound/SoftDeleteDocumentUseCase.java`
      - `src/main/java/com/platform/app/document/application/services/DocumentSoftDeleteService.java`
      - `src/test/java/com/platform/app/document/application/services/DocumentSoftDeleteServiceTest.java`
    </allowed_files>
    <acceptance_criteria>
      - [x] AC-5: Return 404 if document is not found or already deleted.
      - [x] AC-6: Return 403 if unauthorized user attempts deletion.
      - [x] AC-8: Domain event `DocumentSoftDeletedEvent` is emitted.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests com.platform.app.document.application.services.DocumentSoftDeleteServiceTest
      ```
    </verifier>
    <expected_evidence>`DocumentSoftDeleteServiceTest` passes 100%.</expected_evidence>
    <rollback_point>`git checkout -- src/main/java/com/platform/app/document/application/ src/test/java/com/platform/app/document/application/`</rollback_point>
    <stop_conditions>
      - Test assertion failures or event publishing issues.
    </stop_conditions>
  </slice>

  <slice id="S4">
    <objective>Expose `DELETE /api/v1/documents/{id}` endpoint and implement controller tests</objective>
    <change>
      - In `DocumentController.java`: inject `SoftDeleteDocumentUseCase` and add `@DeleteMapping("/{id}")`.
      - In `DocumentControllerTest.java`: add tests covering 204, 401, 403, and 404 responses.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentController.java`
      - `src/test/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentControllerTest.java`
    </allowed_files>
    <acceptance_criteria>
      - [x] AC-4: HTTP 204 No Content returned on successful deletion.
      - [x] AC-5: HTTP 404 Not Found when not found / already deleted.
      - [x] AC-6: HTTP 403 Forbidden for unauthorized user.
      - [x] AC-7: HTTP 401 Unauthorized when unauthenticated.
      - [x] AC-10: Full test suite `./gradlew test` passes.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test
      ```
    </verifier>
    <expected_evidence>All controller tests and whole test suite pass cleanly.</expected_evidence>
    <rollback_point>`git checkout -- src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest/ src/test/java/com/platform/app/document/infrastructure/adapters/primary/rest/`</rollback_point>
    <stop_conditions>
      - Controller security filter errors or HTTP status mismatches.
    </stop_conditions>
  </slice>

</slices>

---

## 5. Scope Contract

<scope_contract>
  <allowed>
    - `src/main/resources/db/changelog/changes/003-create-document-tables.yaml`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/entity/DocumentJpaEntity.java`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/repository/SpringDataDocumentRepository.java`
    - `src/main/java/com/platform/app/document/application/ports/outbound/DocumentRepositoryPort.java`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentRepositoryAdapter.java`
    - `src/main/java/com/platform/app/document/application/dto/SoftDeleteDocumentCommand.java`
    - `src/main/java/com/platform/app/document/application/event/DocumentSoftDeletedEvent.java`
    - `src/main/java/com/platform/app/document/application/ports/inbound/SoftDeleteDocumentUseCase.java`
    - `src/main/java/com/platform/app/document/application/services/DocumentSoftDeleteService.java`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentController.java`
    - `src/test/java/com/platform/app/document/application/services/DocumentSoftDeleteServiceTest.java`
    - `src/test/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentControllerTest.java`
  </allowed>
  <forbidden>
    - `src/main/java/com/platform/app/document/domain/model/Document.java`
    - `src/main/resources/db/changelog/changes/005-*.yaml`
    - Any changes outside `com.platform.app.document` or `003-create-document-tables.yaml`
  </forbidden>
</scope_contract>

---

## 6. Verification Matrix

<verification_matrix>

| AC / Risk | Test / Command | Expected Evidence | Strictness | Actual Result |
|---|---|---|---|---|
| AC-1 | `./gradlew test` | Changeset 003 runs with deleted_at column and index | hard-mandatory | Pending |
| AC-3 | Unit & Adapter tests | `DocumentJpaEntity` and `DocumentRepositoryAdapter` map and update `deleted_at` | hard-mandatory | Pending |
| AC-4 | `DocumentControllerTest` | `DELETE /api/v1/documents/{id}` returns 204 No Content | hard-mandatory | Pending |
| AC-5 | `DocumentControllerTest` | Already deleted / non-existent document returns 404 | hard-mandatory | Pending |
| AC-6 | `DocumentControllerTest` | Unauthorized caller returns 403 Forbidden | hard-mandatory | Pending |
| AC-7 | `DocumentControllerTest` | Unauthenticated request returns 401 Unauthorized | hard-mandatory | Pending |
| AC-8 | `DocumentSoftDeleteServiceTest` | `DocumentSoftDeletedEvent` captured and verified | hard-mandatory | Pending |
| AC-9 | `DocumentRepositoryAdapterTest` | `findById` excludes soft-deleted records | hard-mandatory | Pending |
| AC-10 | `./gradlew test` | Zero failures, all test suites green | hard-mandatory | Pending |

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
  <approved_by>[AUTO: DELEGATED]</approved_by>
  <approved_date>2026-09-19</approved_date>
</gate>

</execution_plan>
