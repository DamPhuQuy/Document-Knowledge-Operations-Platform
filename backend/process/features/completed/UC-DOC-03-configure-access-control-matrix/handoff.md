# Handoff: UC-DOC-03 Configure Document Access Control Matrix

<handoff task_id="UC-DOC-03" version="2.0" framework="RIPER-5">

<!-- Final projection. Short. Do not duplicate research/plan/review artifacts. -->
<!-- Answer: What changed? Why? What proves it? What remains risky? -->

<handoff_status>
  <review_decision>PASS</review_decision>
  <review_artifact>process/features/completed/UC-DOC-03-configure-access-control-matrix/review.md</review_artifact>
  <completed_date>2026-09-17</completed_date>
</handoff_status>

---

## 1. What Changed

<what_changed>
  Implemented UC-DOC-03: Configure Document Access Control Matrix, extending security classification to 4 tiers (`PUBLIC`, `INTERNAL`, `RESTRICTED`, `CONFIDENTIAL`), providing domain models, JPA entities, Spring Data repositories, and an outbound port adapter for explicit user, department, and role ACL grants with permission levels (`VIEW`, `EDIT`, `ADMIN`). Added `PUT /api/v1/documents/{id}/permissions` and `GET /api/v1/documents/{id}/permissions` in `DocumentController`, protected with JWT authentication and ownership/admin authorization checks, executing updates atomically within `@Transactional` boundaries, and emitting `DocumentAclUpdatedEvent` for audit logging (`UC-AUDIT-01`).
</what_changed>

<main_changes>
  - `AccessLevel.java`: Added `CONFIDENTIAL` tier to complete 4-tier classification.
  - `PermissionLevel.java`: Added enum for `VIEW`, `EDIT`, `ADMIN`.
  - `Document.java`: Added `updateAccessLevel(AccessLevel)` domain method.
  - `DocumentUserAccess.java`, `DocumentDepartmentAccess.java`, `DocumentRoleAccess.java`: Domain entities for explicit ACL grants.
  - `DocumentUserAccessJpaEntity.java`, `DocumentDepartmentAccessJpaEntity.java`, `DocumentRoleAccessJpaEntity.java`: JPA entities mapped to PostgreSQL tables.
  - `DocumentAclRepositoryPort.java` & `DocumentAclRepositoryAdapter.java`: Outbound persistence port and adapter providing atomic replacement of permissions and permission checks.
  - `ConfigureDocumentAclUseCase.java`, `GetDocumentPermissionsUseCase.java` & `DocumentAclService.java`: Use case orchestration verifying authorization, updating access level, replacing ACL rows, and publishing domain events.
  - `DocumentAclUpdatedEvent.java`: Domain event emitted on successful ACL update.
  - `DocumentController.java`: Exposed `PUT` and `GET /api/v1/documents/{id}/permissions`.
  - Added comprehensive test suites: `AccessLevelTest`, `PermissionLevelTest`, `DocumentAccessModelTest`, `DocumentAclRepositoryAdapterTest`, `DocumentAclServiceTest`, and updated `DocumentControllerTest`.
</main_changes>

---

## 2. Why

<why>
  UC-DOC-03 enforces granular, enterprise-grade access control across documents, allowing document owners and administrators to restrict visibility to specific users, departments, or roles, meeting confidentiality compliance and preparing pre-filtered access rules for downstream RAG AI retrieval pipelines.
</why>

---

## 3. What Proves It

<evidence>

| AC | Verifier | Result |
|---|---|---|
| AC-1 | `./gradlew test --tests "*AccessLevel*"` | PASS |
| AC-2 | `./gradlew test --tests "*Access*"` | PASS |
| AC-3 | `./gradlew test --tests "*DocumentAclRepositoryAdapter*"` | PASS |
| AC-4, AC-5, AC-6 | `./gradlew test --tests "*DocumentControllerTest*"` | PASS |
| AC-7, AC-8 | `./gradlew test --tests "*DocumentAclServiceTest*"` | PASS |
| AC-9 | `./gradlew test` | PASS |

<!-- To reproduce: -->
```bash
./gradlew test
```

</evidence>

---

## 4. What Remains Risky

<residual_risk>
  - None. In-flight SQL pre-filtered RAG search queries should consume `document_user_access`, `document_department_access`, and `document_role_access` when vector search and retrieval pipelines are connected.
</residual_risk>

---

## 5. Knowledge Promoted to Durable Context

<durable_knowledge>
  - `AccessLevel` 4-tier values: `PUBLIC`, `INTERNAL`, `RESTRICTED`, `CONFIDENTIAL`.
  - `PermissionLevel` values: `VIEW`, `EDIT`, `ADMIN`.
  - Document ACL endpoints: `PUT /api/v1/documents/{id}/permissions` and `GET /api/v1/documents/{id}/permissions`.
</durable_knowledge>

</handoff>
