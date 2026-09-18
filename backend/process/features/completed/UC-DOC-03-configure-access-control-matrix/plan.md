# Plan: PLAN-DOC-03 Configure Document Access Control Matrix

<execution_plan task_id="UC-DOC-03" plan_id="PLAN-DOC-03" version="2.0" framework="RIPER-5">

<!-- PLAN PHASE. Plan artifacts only. No source-code changes. -->
<plan_status>
  <phase>PLAN</phase>
  <last_updated>2026-09-17</last_updated>
</plan_status>

---

## 1. Input Artifacts

<input_artifacts>
  <task_spec>process/features/active/UC-DOC-03-configure-access-control-matrix/task.md</task_spec>
  <research>process/features/active/UC-DOC-03-configure-access-control-matrix/research.md</research>
  <decision>process/features/active/UC-DOC-03-configure-access-control-matrix/decision.md (DEC-DOC-03: Option A Unified Port & Atomic Replacement)</decision>
</input_artifacts>

---

## 2. Execution Constraints

<execution_constraints>
  <allowed_files>
    - `src/main/java/com/platform/app/document/domain/**`
    - `src/main/java/com/platform/app/document/application/**`
    - `src/main/java/com/platform/app/document/infrastructure/**`
    - `src/test/java/com/platform/app/document/**`
    - `process/features/active/UC-DOC-03-configure-access-control-matrix/**`
  </allowed_files>
  <forbidden_files>
    - `src/main/resources/db/**` (Schema already provisioned in changeset 003)
    - `src/main/java/com/platform/app/iam/**`
    - `src/main/java/com/platform/app/shared/**`
  </forbidden_files>
  <allowed_commands>
    - `./gradlew test`
    - `./gradlew compileJava`
  </allowed_commands>
  <restricted_operations>
    - No DB migrations without explicit approval.
    - No third-party dependency additions.
    - Zero modification to existing business logic of UC-DOC-01 and UC-DOC-02.
  </restricted_operations>
  <required_approvals>
    - Gate 2 (Plan Approval) before starting execution.
  </required_approvals>
</execution_constraints>

---

## 3. Slice Summary

<slice_summary>

| ID | Behavior | Boundary | Files | AC | Verifier | Risk | Mode | Rollback |
|---|---|---|---|---|---|---|---|---|
| S1 | Domain Models & Enums (`AccessLevel.CONFIDENTIAL`, `PermissionLevel`, `DocumentUserAccess`, `DocumentDepartmentAccess`, `DocumentRoleAccess`) | Domain layer | 6 files | AC-1, AC-2 | `./gradlew test --tests "*AccessLevel*" --tests "*Permission*"` | LOW | ATOMIC | Git revert |
| S2 | JPA Entities, Repositories & Adapter (`DocumentAclRepositoryPort` & Adapter) | Infrastructure layer | 8 files | AC-3 | `./gradlew test --tests "*DocumentAclRepositoryAdapterTest*"` | LOW | ATOMIC | Git revert |
| S3 | Application Services, Commands, Event & Orchestration (`DocumentAclService`, `DocumentAclUpdatedEvent`) | Application layer | 6 files | AC-7, AC-8 | `./gradlew test --tests "*DocumentAclServiceTest*"` | MEDIUM | ATOMIC | Git revert |
| S4 | REST API Endpoints (`PUT` & `GET /api/v1/documents/{id}/permissions`) & Security checks | Primary adapter layer | 3 files | AC-4, AC-5, AC-6 | `./gradlew test --tests "*DocumentControllerTest*"` | LOW | ATOMIC | Git revert |
| S5 | Full Test Suite Execution & Regression Verification | Entire project | 0 files | AC-9 | `./gradlew test` | LOW | READ-ONLY | None |

</slice_summary>

---

## 4. Slice Details

<slices>

  <slice id="S1">
    <objective>Extend AccessLevel with CONFIDENTIAL, implement PermissionLevel enum, domain entities for User, Department, and Role access, and add updateAccessLevel to Document.</objective>
    <change>
      - Modify `AccessLevel.java` to add `CONFIDENTIAL`.
      - Create `PermissionLevel.java` (`VIEW`, `EDIT`, `ADMIN`).
      - Create `DocumentUserAccess.java`, `DocumentDepartmentAccess.java`, `DocumentRoleAccess.java`.
      - Add `updateAccessLevel(AccessLevel)` method to `Document.java`.
      - Add unit tests validating invariants.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/document/domain/**`
      - `src/test/java/com/platform/app/document/domain/**`
    </allowed_files>
    <acceptance_criteria>
      - AC-1: AccessLevel supports PUBLIC, INTERNAL, RESTRICTED, CONFIDENTIAL.
      - AC-2: PermissionLevel supports VIEW, EDIT, ADMIN with non-null validation.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "com.platform.app.document.domain.*"
      ```
    </verifier>
    <expected_evidence>All domain unit tests pass with zero failures.</expected_evidence>
    <rollback_point>git checkout -- src/main/java/com/platform/app/document/domain</rollback_point>
    <stop_conditions>Domain validation failure on existing tests.</stop_conditions>
  </slice>

  <slice id="S2">
    <objective>Create JPA entities, Spring Data repositories, and DocumentAclRepositoryAdapter implementing DocumentAclRepositoryPort.</objective>
    <change>
      - Create JPA entities for `document_user_access`, `document_department_access`, `document_role_access`.
      - Create Spring Data JPA repositories with deleteByDocumentId and findByDocumentId methods.
      - Define `DocumentAclRepositoryPort` and implement in `DocumentAclRepositoryAdapter`.
      - Add unit test for adapter.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/document/application/ports/outbound/DocumentAclRepositoryPort.java`
      - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/**`
      - `src/test/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/**`
    </allowed_files>
    <acceptance_criteria>
      - AC-3: JPA entities map cleanly to database tables and adapter performs atomic synchronization.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "com.platform.app.document.infrastructure.adapters.secondary.persistence.*"
      ```
    </verifier>
    <expected_evidence>Adapter unit tests pass successfully.</expected_evidence>
    <rollback_point>git checkout -- src/main/java/com/platform/app/document/infrastructure/adapters/secondary</rollback_point>
    <stop_conditions>Entity mapping errors or schema validation mismatches.</stop_conditions>
  </slice>

  <slice id="S3">
    <objective>Implement DocumentAclService, use case ports, DTOs, and DocumentAclUpdatedEvent.</objective>
    <change>
      - Create DTOs: `UpdateDocumentPermissionsRequest`, `DocumentPermissionsResponseDto`, `UserGrantDto`, `DepartmentGrantDto`, `RoleGrantDto`.
      - Create command: `ConfigureDocumentAclCommand`.
      - Create use case interfaces: `ConfigureDocumentAclUseCase`, `GetDocumentPermissionsUseCase`.
      - Create domain event: `DocumentAclUpdatedEvent`.
      - Implement `DocumentAclService` with `@Transactional`, validating ownership/permissions, updating document, replacing ACLs, and publishing event.
      - Create comprehensive unit tests in `DocumentAclServiceTest`.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/document/application/**`
      - `src/test/java/com/platform/app/document/application/**`
    </allowed_files>
    <acceptance_criteria>
      - AC-6, AC-7, AC-8: Ownership validation, 403 on unauthorized, 404 on missing document, event emitted.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "com.platform.app.document.application.services.DocumentAclServiceTest"
      ```
    </verifier>
    <expected_evidence>DocumentAclServiceTest passes covering all business and authorization paths.</expected_evidence>
    <rollback_point>git checkout -- src/main/java/com/platform/app/document/application</rollback_point>
    <stop_conditions>Transactional or authorization logic failure.</stop_conditions>
  </slice>

  <slice id="S4">
    <objective>Expose REST endpoints PUT /api/v1/documents/{id}/permissions and GET /api/v1/documents/{id}/permissions in DocumentController with tests.</objective>
    <change>
      - In `DocumentController.java`, wire `ConfigureDocumentAclUseCase` and `GetDocumentPermissionsUseCase`.
      - Add `@PutMapping("/{id}/permissions")` and `@GetMapping("/{id}/permissions")`.
      - Update and add controller unit/integration tests in `DocumentControllerTest`.
    </change>
    <allowed_files>
      - `src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentController.java`
      - `src/test/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentControllerTest.java`
    </allowed_files>
    <acceptance_criteria>
      - AC-4: PUT /api/v1/documents/{id}/permissions returns 200 with updated permissions.
      - AC-5: GET /api/v1/documents/{id}/permissions returns 200 with current permissions.
      - AC-6: Unauthorized caller returns 403.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "com.platform.app.document.infrastructure.adapters.primary.rest.DocumentControllerTest"
      ```
    </verifier>
    <expected_evidence>DocumentControllerTest passes with all permission endpoint assertions.</expected_evidence>
    <rollback_point>git checkout -- src/main/java/com/platform/app/document/infrastructure/adapters/primary</rollback_point>
    <stop_conditions>Endpoint route conflicts or HTTP status code mismatches.</stop_conditions>
  </slice>

  <slice id="S5">
    <objective>Run complete regression test suite across the entire application.</objective>
    <change>None (verification only).</change>
    <allowed_files>[]</allowed_files>
    <acceptance_criteria>
      - AC-9: Entire test suite passes cleanly with zero regressions.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with 100% test pass rate.</expected_evidence>
    <rollback_point>N/A</rollback_point>
    <stop_conditions>Any test failure in test suite.</stop_conditions>
  </slice>

</slices>

---

## 5. Scope Contract

<scope_contract>
  <allowed>
    - Modifications to `com.platform.app.document` package.
    - Addition of domain models, JPA entities, repositories, use cases, services, DTOs, and REST controller methods.
    - Corresponding unit and integration tests.
  </allowed>
  <forbidden>
    - Modification of database Liquibase changesets or application properties.
    - Changes to `iam` package or security filters.
    - Breaking changes to existing UC-DOC-01 / UC-DOC-02 endpoints or behaviors.
  </forbidden>
</scope_contract>

---

## 6. Verification Matrix

<verification_matrix>

| AC | Slice | Target Test | Verifier Command |
|---|---|---|---|
| AC-1 | S1 | `AccessLevelTest` | `./gradlew test --tests "*AccessLevel*"` |
| AC-2 | S1 | `DocumentAccessModelTest` | `./gradlew test --tests "*Access*"` |
| AC-3 | S2 | `DocumentAclRepositoryAdapterTest` | `./gradlew test --tests "*DocumentAclRepositoryAdapter*"` |
| AC-4, AC-5, AC-6 | S4 | `DocumentControllerTest` | `./gradlew test --tests "*DocumentControllerTest*"` |
| AC-7, AC-8 | S3 | `DocumentAclServiceTest` | `./gradlew test --tests "*DocumentAclServiceTest*"` |
| AC-9 | S5 | All test suites | `./gradlew test` |

</verification_matrix>

---

## 7. Gate 2 — Plan Approved

<gate id="G2" label="Gate 2 — Plan Approved">
  - [x] Every slice has a verifier.
  - [x] Allowed/forbidden file scope is defined.
  - [x] Rollback point defined per slice.
  - [x] Stop conditions defined.
  - [x] Plan approved.
  <approved_by>[AUTO: DELEGATED]</approved_by>
  <approved_date>2026-09-17</approved_date>
</gate>

</execution_plan>
