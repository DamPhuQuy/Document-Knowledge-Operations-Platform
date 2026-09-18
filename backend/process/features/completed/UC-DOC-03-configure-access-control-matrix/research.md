# Research: UC-DOC-03 Configure Document Access Control Matrix

<research_context task_id="UC-DOC-03" version="1.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. No source modifications. No implementation decisions. -->
<research_status>
  <phase>RESEARCH</phase>
  <mode>READ-ONLY</mode>
  <research_owner>@engineer</research_owner>
  <last_updated>2026-09-17</last_updated>
</research_status>

---

## 1. Current Behavior & Existing Implementation

<current_behavior>
  - **Document Bounded Context (`com.platform.app.document`):**
    - `Document.java` represents the document aggregate root with fields: `id`, `title`, `originalFileName`, `contentType`, `fileSizeBytes`, `checksumSha256`, `storageKey`, `currentVersion`, `status`, `uploadedByUserId`, `departmentId`, `accessLevel`, `createdAt`, `updatedAt`.
    - `AccessLevel.java` currently defines 3 values: `INTERNAL`, `PUBLIC`, `RESTRICTED`. It is missing the 4th tier: `CONFIDENTIAL`.
    - There is no `PermissionLevel.java` enum (`VIEW`, `EDIT`, `ADMIN`) in the domain layer.
    - There are no domain entities or JPA entities for `document_user_access`, `document_department_access`, and `document_role_access`.
    - `DocumentController` exposes `POST /api/v1/documents` (UC-DOC-01) and `POST /api/v1/documents/{id}/versions` (UC-DOC-02). It does not expose endpoints for viewing or updating document permissions (`/api/v1/documents/{id}/permissions`).
  - **Database Schema Status (`003-create-document-tables.yaml`):**
    - Changeset `003-create-documents-table` defines table `documents` with `access_level VARCHAR(50) NOT NULL DEFAULT 'INTERNAL'`.
    - Changeset `003-create-document-user-access-table` defines table `document_user_access` (`id UUID PK`, `document_id UUID FK`, `user_id UUID FK`, `permission_level VARCHAR(50) NOT NULL DEFAULT 'VIEW'`, `created_at TIMESTAMPTZ`, Unique on `(document_id, user_id)`).
    - Changeset `003-create-document-department-access-table` defines table `document_department_access` (`id UUID PK`, `document_id UUID FK`, `department_id UUID FK`, `permission_level VARCHAR(50) NOT NULL DEFAULT 'VIEW'`, `created_at TIMESTAMPTZ`, Unique on `(document_id, department_id)`).
    - Changeset `003-create-document-role-access-table` defines table `document_role_access` (`id UUID PK`, `document_id UUID FK`, `role_id UUID FK`, `permission_level VARCHAR(50) NOT NULL DEFAULT 'VIEW'`, `created_at TIMESTAMPTZ`, Unique on `(document_id, role_id)`).
    - Foreign keys and indices are already in place with `ON DELETE CASCADE` for parent document deletion.
  - **Security & Authorization Status:**
    - JWT authentication extracts authenticated user ID and authorities.
    - Standard authority checking uses `@PreAuthorize` or manual programmatic checks in domain services.
    - Existing endpoints verify `write:documents` or `ROLE_ADMIN`, and compare `document.getUploadedByUserId().equals(currentUserId)`.
</current_behavior>

---

## 2. Execution Flows

<execution_flow>

### Flow 1: Configure / Update Permissions (PUT /api/v1/documents/{id}/permissions)
```text
Actor (Owner / Admin / User with manage:permissions)
  → PUT /api/v1/documents/{id}/permissions (JSON body: accessLevel, userGrants, departmentGrants, roleGrants)
    → TraceIdFilter (logs trace context)
    → Spring Security FilterChain (authenticates JWT)
    → DocumentController / DocumentAclController.updatePermissions(id, request, authentication)
      ├── 1. Extract authenticated user ID and check authorities (ROLE_ADMIN, manage:permissions)
      ├── 2. Validate input payload:
      │      └── Check accessLevel != null, grants validity
      └── 3. ConfigureDocumentAclUseCase.configureAcl(command)
          ├── a. Load target document from DocumentRepositoryPort
          │      └── If not found -> throw DocumentNotFoundException (HTTP 404)
          ├── b. Authorize caller:
          │      └── If caller != document.uploadedByUserId AND !isAdmin AND !hasManagePermissions:
          │             -> throw DocumentAccessDeniedException (HTTP 403)
          ├── c. [In @Transactional boundary]:
          │      ├── Update document.accessLevel and save document
          │      ├── Synchronize/replace user grants in document_user_access
          │      ├── Synchronize/replace department grants in document_department_access
          │      └── Synchronize/replace role grants in document_role_access
          ├── d. Publish DocumentAclUpdatedEvent (for audit trail UC-AUDIT-01)
          └── e. Return DocumentPermissionsResponseDto (HTTP 200 OK)
```

### Flow 2: Get Permissions Matrix (GET /api/v1/documents/{id}/permissions)
```text
Actor (Owner / Admin / User with view permissions)
  → GET /api/v1/documents/{id}/permissions
    → Spring Security FilterChain (authenticates JWT)
    → DocumentController / DocumentAclController.getPermissions(id, authentication)
      ├── 1. Fetch document (or 404 if not found)
      ├── 2. Authorize caller (owner, admin, or user having view permissions)
      ├── 3. Load all active grants from DocumentAclRepositoryPort
      └── 4. Return DocumentPermissionsResponseDto (HTTP 200 OK)
```

</execution_flow>

---

## 3. Relevant Components

<components>

| File / Symbol | Role | Evidence | Confidence |
|---|---|---|---|
| `AccessLevel.java` | Security classification enum | Needs `CONFIDENTIAL` | Confirmed |
| `PermissionLevel.java` | Granular ACL permission enum | Needs creation (`VIEW`, `EDIT`, `ADMIN`) | Confirmed |
| `Document.java` | Aggregate Root | Needs `updateAccessLevel` domain method | Confirmed |
| `DocumentUserAccess.java` | Domain entity for user ACL | To be created in domain model | Confirmed |
| `DocumentDepartmentAccess.java` | Domain entity for department ACL | To be created in domain model | Confirmed |
| `DocumentRoleAccess.java` | Domain entity for role ACL | To be created in domain model | Confirmed |
| `DocumentAclRepositoryPort.java` | Outbound port for ACL persistence | To be created in application ports | Confirmed |
| `DocumentAclService.java` | Use case orchestrator for ACL | To be created in application services | Confirmed |
| `DocumentController.java` | REST adapter | Endpoint `PUT/GET /api/v1/documents/{id}/permissions` | Confirmed |
| `003-create-document-tables.yaml` | Liquibase changelog | Tables already present in DB schema | Confirmed |

</components>

---

## 4. Dependencies & Boundaries

<boundaries>
  <callers>Web Frontend (ACL Modal), Authorized API clients</callers>
  <callees>DocumentRepositoryPort, DocumentAclRepositoryPort, ApplicationEventPublisher</callees>
  <persistence>Tables `documents`, `document_user_access`, `document_department_access`, `document_role_access`</persistence>
  <external_systems>None (Internal database transaction + Spring Event publisher)</external_systems>
  <transaction_boundary>`@Transactional` boundary around `ConfigureDocumentAclUseCase.configureAcl(...)`</transaction_boundary>
  <security_boundary>JWT Authentication; Caller must be Owner (`document.uploadedByUserId == caller.id`), or have `ROLE_ADMIN`, or have `manage:permissions` authority</security_boundary>
</boundaries>

---

## 5. Existing Tests

<existing_tests>

| Test | Behavior covered | Gap |
|---|---|---|
| `DocumentUploadServiceTest` | UC-DOC-01 upload orchestration | No ACL grant configuration tests |
| `DocumentVersionServiceTest` | UC-DOC-02 versioning orchestration | Only checks basic owner/admin check |
| `DocumentControllerTest` | UC-DOC-01 / UC-DOC-02 REST endpoints | Needs endpoints tests for `PUT/GET /api/v1/documents/{id}/permissions` |
| `AccessLevelTest` | Unit tests for AccessLevel enum | Needs coverage for `CONFIDENTIAL` |

</existing_tests>

---

## 6. Runtime / Configuration

<runtime_config>
  - Liquibase migrations are managed in `backend/src/main/resources/db/changelog/changes/003-create-document-tables.yaml`.
  - JPA Hibernate ddl-auto is set to `validate` in `application.yaml`.
  - Spring Security validates JWT bearer tokens via `JwtAuthenticationFilter`.
</runtime_config>

---

## 7. Source-of-Truth Analysis

<source_of_truth_analysis>

| Source | Says | Authority | Conflict |
|---|---|---|---|
| `docs/specs/business/use_cases/document/02_document_management.md` | UC-DOC-03: 4 tiers (`PUBLIC`, `INTERNAL`, `RESTRICTED`, `CONFIDENTIAL`), explicit grants (`VIEW`, `EDIT`, `ADMIN`) | High (Business Spec) | None |
| `docs/specs/business/05_requirements_traceability_matrix.md` | `PUT /api/v1/documents/{id}/permissions` | High (Contract) | None |
| `docs/specs/database/schema.dbml` | Tables `document_user_access`, `document_dept_access`, `document_role_access` | High (Schema) | Note: DB changeset named table `document_department_access`, matching Liquibase |
| `backend/src/main/resources/db/changelog/changes/003-create-document-tables.yaml` | Implements schema with table names `document_user_access`, `document_department_access`, `document_role_access` | High (Code) | None |

</source_of_truth_analysis>

---

## 8. Evidence Classification

<evidence>
  <confirmed>
    - `003-create-document-tables.yaml` provides tables `document_user_access`, `document_department_access`, `document_role_access`.
    - `AccessLevel.java` has only 3 values (`INTERNAL`, `PUBLIC`, `RESTRICTED`) and requires `CONFIDENTIAL`.
    - No ACL JPA entities or repository adapters exist yet.
  </confirmed>
  <observed>
    - `DocumentVersionService` enforces authorization by checking `document.getUploadedByUserId().equals(command.getUserId()) || command.isAdmin()`.
    - `DocumentController` uses `@PreAuthorize("hasAuthority('write:documents') or hasAuthority('WRITE:DOCUMENTS') or hasRole('ADMIN')")`.
  </observed>
  <hypothesized>
    - Full synchronization (delete existing grants for document and insert new list) is the cleanest, most deterministic approach for ACL updates.
  </hypothesized>
</evidence>

---

## 9. Open Decisions for INNOVATE Phase

<open_decisions>
  1. Should ACL persistence be modeled with separate JPA repositories per table or a consolidated `DocumentAclRepositoryPort`?
  2. Should ACL update replace all existing grants for the document in one transaction, or support patch/merge semantics?
  3. Should ACL endpoints be located inside `DocumentController` or in a dedicated `DocumentAclController`?
</open_decisions>

---

## 10. Research Exit Criteria (Gate 0)

<gate id="G0" label="Research Complete">
  - [x] Current behavior understood and documented.
  - [x] Execution flow traced.
  - [x] Source-of-truth analysis completed with zero unresolved conflicts.
  - [x] Ready to advance to INNOVATE phase.
</gate>

</research_context>
