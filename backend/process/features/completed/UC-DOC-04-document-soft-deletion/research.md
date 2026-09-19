# Research: UC-DOC-04 Document Soft Deletion

<research_context task_id="UC-DOC-04" version="1.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. No source modifications. No implementation decisions. -->
<research_status>
  <phase>RESEARCH</phase>
  <mode>READ-ONLY</mode>
  <research_owner>@engineer</research_owner>
  <last_updated>2026-09-19</last_updated>
</research_status>

---

## 1. Current Behavior & Existing Implementation

<current_behavior>
  - **Document Bounded Context (`com.platform.app.document`):**
    - `Document.java` represents the domain aggregate root with fields: `id`, `title`, `originalFileName`, `contentType`, `fileSizeBytes`, `checksumSha256`, `storageKey`, `currentVersion`, `status`, `uploadedByUserId`, `departmentId`, `accessLevel`, `createdAt`, `updatedAt`.
    - It has domain methods for lifecycle transitions (`applyNewVersion`, `markProcessing`, `markReady`, `markFailed`, `updateStatus`, `updateAccessLevel`), but **no** `deletedAt` field, `isDeleted()` predicate, or `softDelete(...)` domain method.
    - `DocumentJpaEntity.java` maps fields directly to table `documents`, lacking a `@Column(name = "deleted_at")` mapping.
    - `DocumentRepositoryPort` defines `save(Document)` and `findById(UUID)`.
    - `DocumentRepositoryAdapter` maps between `DocumentJpaEntity` and `Document` domain model, but does not handle `deleted_at`.
    - `SpringDataDocumentRepository` extends `JpaRepository<DocumentJpaEntity, UUID>` without soft deletion filters.
    - `DocumentController` currently exposes:
      - `POST /api/v1/documents` (UC-DOC-01: Document Upload)
      - `POST /api/v1/documents/{id}/versions` (UC-DOC-02: New Revision Upload)
      - `PUT /api/v1/documents/{id}/permissions` (UC-DOC-03: Update ACL Matrix)
      - `GET /api/v1/documents/{id}/permissions` (UC-DOC-03: View ACL Matrix)
      - There is **no** `DELETE /api/v1/documents/{id}` endpoint.
  - **Database Schema Status (`db/changelog/`):**
    - `003-create-document-tables.yaml` created table `documents` with fields up to `updated_at`, but omitted `deleted_at`.
    - `004-create-audit-tables.yaml` created table `audit_logs` for `UC-AUDIT-01`.
    - In `docs/specs/database/specification.md` (lines 38, 155, 425) and `docs/specs/database/modules/`, `documents` is explicitly specified with `deleted_at TIMESTAMPTZ` and index `idx_documents_deleted_at` / index filter `WHERE deleted_at IS NULL`.
    - A new Liquibase migration (e.g. `005-add-document-deleted-at.yaml`) is required to add `deleted_at TIMESTAMPTZ` and index to `documents`.
  - **Security & Authorization Status:**
    - Document ownership is verified by comparing `document.getUploadedByUserId().equals(currentUserId)`.
    - System administrator privilege is verified by role `ROLE_ADMIN`.
    - Pre-condition states: User has `delete:documents` permission or owns the document.
    - Existing endpoints in `DocumentController` extract user ID from `Authentication.getName()` and inspect granted authorities.
  - **Audit Logging Subsystem (`UC-AUDIT-01`):**
    - Audit events in other document use cases (`DocumentUploadedEvent`, `DocumentVersionCreatedEvent`, `DocumentAclUpdatedEvent`) are published via Spring's `ApplicationEventPublisher`.
    - A dedicated `DocumentSoftDeletedEvent` is required to notify downstream listeners for immutable audit logging of `DELETE_DOC`.
</current_behavior>

---

## 2. Execution Flows

<execution_flow>

### Flow 1: Soft Delete Document (`DELETE /api/v1/documents/{id}`)
```text
Actor (Document Owner / Admin / User with delete:documents)
  → DELETE /api/v1/documents/{id}
    → TraceIdFilter (logs correlation context)
    → Spring Security FilterChain (authenticates JWT; returns 401 if unauthenticated)
    → DocumentController.deleteDocument(id, authentication)
      ├── 1. Extract authenticated user ID, check isAdmin (ROLE_ADMIN) and hasDeletePermission (delete:documents)
      ├── 2. Build SoftDeleteDocumentCommand(documentId, userId, isAdmin, hasDeletePermission)
      └── 3. SoftDeleteDocumentUseCase.softDeleteDocument(command)
          ├── a. Retrieve document from DocumentRepositoryPort.findById(documentId)
          │      └── If document does not exist:
          │             -> throw DocumentNotFoundException (HTTP 404)
          │      └── If document already soft-deleted (deletedAt != null):
          │             -> throw DocumentNotFoundException (HTTP 404 per UC-DOC-04 alt path 1a)
          ├── b. Authorize caller:
          │      └── If caller != document.uploadedByUserId AND !isAdmin AND !hasDeletePermission:
          │             -> throw DocumentAccessDeniedException (HTTP 403)
          ├── c. [In @Transactional boundary]:
          │      ├── Execute domain action: document.softDelete(Instant.now())
          │      ├── Persist updated document state via DocumentRepositoryPort.save(document)
          │      └── Publish domain event: DocumentSoftDeletedEvent(documentId, userId, timestamp)
          └── d. Return HTTP 204 No Content
```

### Flow 2: Query / Retrieval Exclusion Enforcement (`WHERE deleted_at IS NULL`)
```text
Query / Retrieval Caller (API queries, RAG pipeline, version upload UC-DOC-02, ACL UC-DOC-03)
  → DocumentRepositoryPort.findById(documentId)
    → SQL executed: SELECT * FROM documents WHERE id = ? AND deleted_at IS NULL
    → If document has deleted_at != null:
         Returns Optional.empty()
         Calling use cases automatically treat it as 404 Not Found
```

</execution_flow>

---

## 3. Relevant Components

<components>

| File / Symbol | Role | Evidence | Confidence |
|---|---|---|---|
| `Document.java` | Domain Aggregate Root | Needs `deletedAt` field, `isDeleted()`, and `softDelete(Instant)` domain method | Confirmed |
| `SoftDeleteDocumentCommand.java` | Application DTO / Command | To be created with `documentId`, `currentUserId`, `isAdmin`, `hasDeletePermission` | Confirmed |
| `SoftDeleteDocumentUseCase.java` | Inbound Port | Interface defining `void softDeleteDocument(SoftDeleteDocumentCommand)` | Confirmed |
| `DocumentSoftDeleteService.java` | Application Service | Use case implementation orchestrating retrieval, authorization, deletion, and event publishing | Confirmed |
| `DocumentSoftDeletedEvent.java` | Domain Event | Emitted on transaction commit to satisfy `UC-AUDIT-01` (`DELETE_DOC`) | Confirmed |
| `DocumentJpaEntity.java` | JPA Entity | Needs `@Column(name = "deleted_at") private Instant deletedAt;` | Confirmed |
| `SpringDataDocumentRepository.java` | Spring Data Repository | Needs query filter excluding soft-deleted documents (`findByIdAndDeletedAtIsNull` or Hibernate filter) | Confirmed |
| `DocumentRepositoryPort.java` | Outbound Port | Contract for persisting and retrieving documents; must enforce `deleted_at IS NULL` invariant | Confirmed |
| `DocumentRepositoryAdapter.java` | Persistence Adapter | Maps `deletedAt` and delegates retrieval with soft-delete exclusion | Confirmed |
| `DocumentController.java` | REST Adapter | Exposes `DELETE /api/v1/documents/{id}` returning HTTP 204 | Confirmed |
| `005-add-document-deleted-at.yaml` | Liquibase Changelog | Adds column `deleted_at TIMESTAMPTZ` and index filter `WHERE deleted_at IS NULL` | Confirmed |
| `db.changelog-master.yaml` | Master Changelog | Includes changeset `005-add-document-deleted-at.yaml` | Confirmed |

</components>

---

## 4. Dependencies & Boundaries

<boundaries>
  <callers>Document Management UI (Delete button & confirmation modal), REST API consumers</callers>
  <callees>DocumentRepositoryPort, ApplicationEventPublisher</callees>
  <persistence>Table `documents` (column `deleted_at TIMESTAMPTZ`)</persistence>
  <external_systems>AWS S3: Preserved untouched per Business Rule B1 (zero physical deletion)</external_systems>
  <transaction_boundary>`@Transactional` boundary around `SoftDeleteDocumentUseCase.softDeleteDocument(...)`</transaction_boundary>
  <security_boundary>JWT Authentication; caller must be Owner (`document.uploadedByUserId == caller.id`), or have `ROLE_ADMIN`, or have `delete:documents` authority</security_boundary>
</boundaries>

---

## 5. Existing Tests & Coverage Gaps

<existing_tests>

| Test | Behavior covered | Gap |
|---|---|---|
| `DocumentUploadServiceTest` | UC-DOC-01 upload orchestration | Does not test soft deletion state |
| `DocumentVersionServiceTest` | UC-DOC-02 versioning orchestration | Should verify soft-deleted document cannot receive a new version (returns 404) |
| `DocumentAclServiceTest` | UC-DOC-03 ACL configuration | Should verify soft-deleted document cannot have ACL updated/viewed (returns 404) |
| `DocumentControllerTest` | UC-DOC-01, UC-DOC-02, UC-DOC-03 endpoints | Completely missing tests for `DELETE /api/v1/documents/{id}` |
| `DocumentTest` | Unit tests for `Document` model invariants | Missing tests for `deletedAt`, `isDeleted()`, and `softDelete` |

</existing_tests>

---

## 6. Runtime / Configuration

<runtime_config>
  - Liquibase migrations are executed against PostgreSQL on application startup. Master changelog is `src/main/resources/db/changelog/db.changelog-master.yaml`.
  - Hibernate `ddl-auto` is configured as `validate`. Any entity changes to `DocumentJpaEntity` MUST strictly match the Liquibase migration.
  - Spring Security validates JWT bearer tokens via `JwtAuthenticationFilter` and extracts authorities into `SecurityContextHolder`.
  - HTTP 204 No Content is the required HTTP response for successful soft deletion per UC-DOC-04 Basic Path Step 4.
</runtime_config>

---

## 7. Source-of-Truth Analysis

<source_of_truth_analysis>

| Source | Says | Authority | Conflict |
|---|---|---|---|
| `docs/specs/business/use_cases/document/02_document_management.md#UC-DOC-04` | Soft deletion sets `deleted_at = CURRENT_TIMESTAMP`, excludes from SQL/RAG queries, preserves physical DB & S3 files, emits audit event, returns HTTP 204 | High (Business Spec) | None |
| `docs/specs/business/05_requirements_traceability_matrix.md` | `DELETE /api/v1/documents/{id}` mapped to `UC-DOC-04` | High (Requirements) | None |
| `docs/specs/database/specification.md` | Line 38 & 155: `documents` table has `deleted_at TIMESTAMPTZ` with index, all queries enforce `WHERE deleted_at IS NULL` | High (Schema Spec) | Existing Liquibase `003-create-document-tables.yaml` omitted `deleted_at`, resolved via new changeset `005` |
| `docs/specs/business/use_cases/audit/03_audit_trail.md#UC-AUDIT-01` | Included by UC-DOC-04 to log `DELETE_DOC` | High (Audit Spec) | Handled via decoupled domain event `DocumentSoftDeletedEvent` |

</source_of_truth_analysis>

---

## 8. Evidence Classification

<evidence>
  <confirmed>
    - Table `documents` in `003-create-document-tables.yaml` does NOT have `deleted_at`.
    - Neither `Document.java` nor `DocumentJpaEntity.java` currently tracks `deleted_at`.
    - No `DELETE /api/v1/documents/{id}` endpoint or use case exists.
    - All tests currently pass (`./gradlew test`).
    - The existing architecture uses inbound ports (`UseCase`), outbound ports (`RepositoryPort`), application services, and Spring domain events (`eventPublisher.publishEvent`).
  </confirmed>
  <observed>
    - `DocumentVersionService` and `DocumentAclService` use `documentRepositoryPort.findById(id)`. If `findById` filters `WHERE deleted_at IS NULL`, they will naturally reject soft-deleted documents with `DocumentNotFoundException` (HTTP 404).
    - `DocumentController` extracts user ID via `UUID.fromString(authentication.getName())` and inspects `ROLE_ADMIN` via `authentication.getAuthorities()`.
  </observed>
  <hypothesized>
    - Using explicit repository query method `findByIdAndDeletedAtIsNull` in `SpringDataDocumentRepository` combined with `Optional<Document> findById(UUID id)` in `DocumentRepositoryPort` is safer and less intrusive than global Hibernate `@SQLRestriction("deleted_at IS NULL")`, which could interfere with explicit administrative audit queries or entity update flushes. This will be evaluated in the INNOVATE phase.
  </hypothesized>
</evidence>

---

## 9. Open Decisions for INNOVATE Phase

<open_decisions>
  1. **Filtering Strategy for Soft-Deleted Documents:**
     - *Option A:* Hibernate entity-level annotation `@SQLRestriction("deleted_at IS NULL")` on `DocumentJpaEntity`.
     - *Option B:* Explicit Spring Data JPA query methods (`findByIdAndDeletedAtIsNull(UUID id)`) exposed through `DocumentRepositoryPort.findById(UUID id)`.
     - *Option C:* Hybrid approach: explicit query methods for standard port lookups, with an additional optional port method (`findAnyById`) if historical/audit lookup is ever required.
  2. **Index Configuration for NF1 Performance Invariant:**
     - *Option A:* Standard B-Tree index on `deleted_at` (`CREATE INDEX idx_documents_deleted_at ON documents(deleted_at)`).
     - *Option B:* Partial index optimized for active records (`CREATE INDEX idx_documents_active_id ON documents(id) WHERE deleted_at IS NULL`).
     - *Option C:* Both a standard index on `deleted_at` and partial index on active records.
  3. **Service Architecture & Inbound Port Placement:**
     - *Option A:* Dedicated `DocumentSoftDeleteService` implementing `SoftDeleteDocumentUseCase` (strict Single Responsibility Principle).
     - *Option B:* Consolidate into a broader document lifecycle service.
</open_decisions>

---

## 10. Research Exit Criteria (Gate 0)

<gate id="G0" label="Research Complete">
  - [x] Current behavior understood and documented.
  - [x] Execution flow traced for normal path and edge cases (401, 403, 404, 204).
  - [x] Source-of-truth analysis completed with zero unresolved conflicts.
  - [x] Open decisions documented with clear trade-offs ready for INNOVATE phase.
  - [x] All research exit criteria verified.
</gate>

</research_context>
