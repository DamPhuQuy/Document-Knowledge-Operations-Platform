# Task: [UC-DOC-03] Configure Document Access Control Matrix

<task_spec version="3.0" framework="RIPER-5">

<!-- ════════════════════════════════════════════
     SECTION 0 — TASK CONTROL (master state record)
     ════════════════════════════════════════════ -->
<task_control>
  <status>COMPLETED</status>  <!-- BACKLOG | ACTIVE | BLOCKED | PAUSED | REVIEW | COMPLETED | CANCELLED -->
  <spec_level>S3</spec_level>  <!-- S0=sketch | S1=defined | S2=verified | S3=locked -->
  <priority>P0</priority>  <!-- P0=urgent | P1=high | P2=normal | P3=low -->
  <risk>MEDIUM</risk>  <!-- LOW | MEDIUM | HIGH -->
  <estimated_story_points>3</estimated_story_points>  <!-- 1 SP ≈ 2-4 focused engineering hours -->
  <working_mode>DELEGATED</working_mode>  <!-- PAIR (default: halts at each gate for review) | DELEGATED (fast-track: continuous autonomous run) | MANUAL | DIAGNOSE-ONLY -->
  <current_phase>REVIEW</current_phase>  <!-- RESEARCH | INNOVATE | PLAN | EXECUTE | REVIEW -->
  <owner>@engineer</owner>
  <decision_owner>@engineer</decision_owner>
  <created>2026-09-17</created>
  <last_updated>2026-09-17</last_updated>
</task_control>

---

## 1. Specification (Pillar 1: Task / Spec)

<specification>
  <goal>
    Implement UC-DOC-03: Configure Document Access Control Matrix, allowing document authors, department managers, and system administrators possessing `manage:permissions` authority to configure the 4-tier security classification (`PUBLIC`, `INTERNAL`, `RESTRICTED`, `CONFIDENTIAL`) and manage explicit ACL entries (`VIEW`, `EDIT`, `ADMIN`) across users (`document_user_access`), departments (`document_department_access`), and roles (`document_role_access`) in an ACID transaction, publishing `DocumentAclUpdatedEvent` for audit logging (`UC-AUDIT-01`), and enabling pre-filtered authorization queries.
  </goal>

  <current_behavior>
    - `AccessLevel` enum only contains `INTERNAL`, `PUBLIC`, and `RESTRICTED` (missing `CONFIDENTIAL`).
    - Liquibase migration `003-create-document-tables.yaml` has already provisioned the database schema for `document_user_access`, `document_department_access`, and `document_role_access`.
    - No JPA entities, repositories, or domain models exist for explicit document ACL grants.
    - No use case interface or application service exists to read or update document ACL configurations.
    - No REST endpoints exist for `PUT /api/v1/documents/{id}/permissions` or `GET /api/v1/documents/{id}/permissions`.
    - No domain event `DocumentAclUpdatedEvent` exists.
  </current_behavior>

  <expected_behavior>
    - `AccessLevel` enum includes `PUBLIC`, `INTERNAL`, `RESTRICTED`, and `CONFIDENTIAL`.
    - `PermissionLevel` enum (`VIEW`, `EDIT`, `ADMIN`) is defined in domain layer.
    - Domain models and JPA entities exist for user, department, and role ACL grants.
    - Expose `PUT /api/v1/documents/{id}/permissions` accepting a JSON payload specifying `accessLevel` and lists of explicit user, department, and role grants.
    - Expose `GET /api/v1/documents/{id}/permissions` retrieving the active classification and ACL grants.
    - Verifies user authorization: caller must be the document owner (`uploadedByUserId == currentUserId`), or possess `manage:permissions` / `ROLE_ADMIN`.
    - Validates target document exists (returns HTTP 404 if not found).
    - If caller lacks required authorization, returns HTTP 403 Forbidden.
    - Executes ACL update within an ACID transaction:
      1. Updates `documents.access_level` and `updated_at`.
      2. Synchronizes / replaces explicit ACL rows in `document_user_access`, `document_department_access`, and `document_role_access`.
    - Dispatches domain event `DocumentAclUpdatedEvent` upon successful commit.
    - Returns HTTP 200 OK with the updated permission matrix.
  </expected_behavior>

  <actor_authorization>
    Primary Actor: Document Owner, Department Manager, or System Administrator (`ROLE_ADMIN`).
    Authorization Pre-condition: User possesses `manage:permissions` authority or owns the document (`uploadedByUserId == currentUserId`) or has administrative privileges.
  </actor_authorization>

  <invariants>
    - B1: `PUBLIC` is readable by all authenticated users.
    - B2: `INTERNAL` is readable only if `user.is_internal = TRUE`.
    - B3: `RESTRICTED` is readable only if `user.department_id = document.department_id`.
    - B4: `CONFIDENTIAL` requires explicit ACL in `document_user_access`, `document_department_access`, or `document_role_access`, or uploader ownership.
    - NF1: All ACL updates execute atomically in a single database transaction.
    - Clean Architecture: Domain models remain pure Java POJOs without framework/cloud SDK leaks.
  </invariants>

  <out_of_scope>
    - UC-DOC-04: Document Soft Deletion (`deleted_at` lifecycle).
    - AI & RAG Subsystem: Text parsing, chunking, OCR, and vector embeddings generation.
    - Frontend UI modal components (handled in frontend repository).
  </out_of_scope>

  <acceptance_criteria>
    - [x] AC-1: Update `AccessLevel` enum to support 4 tiers (`PUBLIC`, `INTERNAL`, `RESTRICTED`, `CONFIDENTIAL`).
    - [x] AC-2: Implement `PermissionLevel` enum (`VIEW`, `EDIT`, `ADMIN`) and domain models for user, department, and role access.
    - [x] AC-3: Implement JPA entities, Spring Data repositories, and outbound ports for document ACL entries.
    - [x] AC-4: Expose `PUT /api/v1/documents/{id}/permissions` accepting valid payload and updating `access_level` and ACL entries in a single transaction.
    - [x] AC-5: Expose `GET /api/v1/documents/{id}/permissions` returning the current access control matrix.
    - [x] AC-6: Non-owner without `manage:permissions` or `ROLE_ADMIN` attempting to update permissions receives HTTP 403 Forbidden.
    - [x] AC-7: Updating or viewing permissions for a non-existent document ID returns HTTP 404 Not Found.
    - [x] AC-8: Domain event `DocumentAclUpdatedEvent` is emitted upon successful transaction commit.
    - [x] AC-9: Full unit and integration test suite passes cleanly with zero regressions (`./gradlew test`).
  </acceptance_criteria>

  <!-- Definition-of-Ready (DoR) Gate -->
  <definition_of_ready>
    - [x] Target outcome and acceptance criteria are verifiable without guessing.
    - [x] Invariants and <out_of_scope> boundaries are explicit.
    - [x] Open questions identified for INNOVATE phase in research.md.
  </definition_of_ready>
</specification>

---

## 2. Context Boundaries (Pillar 2: Context)

<context_boundaries>
  <target_files>
    - `src/main/java/com/platform/app/document/domain/model/AccessLevel.java`
    - `src/main/java/com/platform/app/document/domain/model/PermissionLevel.java`
    - `src/main/java/com/platform/app/document/domain/model/DocumentUserAccess.java`
    - `src/main/java/com/platform/app/document/domain/model/DocumentDepartmentAccess.java`
    - `src/main/java/com/platform/app/document/domain/model/DocumentRoleAccess.java`
    - `src/main/java/com/platform/app/document/domain/model/Document.java`
    - `src/main/java/com/platform/app/document/application/dto/ConfigureDocumentAclCommand.java`
    - `src/main/java/com/platform/app/document/application/dto/DocumentPermissionsResponseDto.java`
    - `src/main/java/com/platform/app/document/application/dto/UpdateDocumentPermissionsRequest.java`
    - `src/main/java/com/platform/app/document/application/event/DocumentAclUpdatedEvent.java`
    - `src/main/java/com/platform/app/document/application/ports/inbound/ConfigureDocumentAclUseCase.java`
    - `src/main/java/com/platform/app/document/application/ports/inbound/GetDocumentPermissionsUseCase.java`
    - `src/main/java/com/platform/app/document/application/ports/outbound/DocumentAclRepositoryPort.java`
    - `src/main/java/com/platform/app/document/application/services/DocumentAclService.java`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/entity/DocumentUserAccessJpaEntity.java`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/entity/DocumentDepartmentAccessJpaEntity.java`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/entity/DocumentRoleAccessJpaEntity.java`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/repository/SpringDataDocumentUserAccessRepository.java`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/repository/SpringDataDocumentDepartmentAccessRepository.java`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/repository/SpringDataDocumentRoleAccessRepository.java`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentAclRepositoryAdapter.java`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/primary/rest/DocumentAclController.java` (or endpoint in `DocumentController.java`)
  </target_files>

  <context_groups>
    - `process/context/all-context.md`
    - `docs/specs/business/use_cases/document/02_document_management.md`
    - `docs/specs/database/schema.dbml`
  </context_groups>

  <source_of_truth>
    <requirement>`docs/specs/business/use_cases/document/02_document_management.md#UC-DOC-03`</requirement>
    <architecture>`docs/specs/business/03_use_case_architecture.md`</architecture>
    <existing_behavior>`src/main/resources/db/changelog/changes/003-create-document-tables.yaml`</existing_behavior>
    <tests>`src/test/java/com/platform/app/document/`</tests>
  </source_of_truth>
</context_boundaries>

---

## 3. Verification Strategy

<verification_strategy>

| AC / Risk | Evidence required | Verifier |
|---|---|---|
| AC-1 | `AccessLevel` enum contains `CONFIDENTIAL` | Unit test `AccessLevelTest` |
| AC-2 | `PermissionLevel` and domain entities enforce non-null invariants | Unit tests for ACL domain models |
| AC-3 | JPA entities and adapter properly map and persist ACL records | Repository adapter unit tests |
| AC-4 | `PUT /api/v1/documents/{id}/permissions` replaces ACL entries atomically | Integration/MockMvc test |
| AC-5 | `GET /api/v1/documents/{id}/permissions` returns configured ACL entries | Integration/MockMvc test |
| AC-6 | Unauthorized user receives HTTP 403 Forbidden | Security / Controller test |
| AC-7 | Non-existent document ID returns HTTP 404 Not Found | Controller test |
| AC-8 | `DocumentAclUpdatedEvent` is captured by event listener/publisher | Service unit test |
| AC-9 | `./gradlew test` passes 100% cleanly | `./gradlew test` |

</verification_strategy>

---

## 4. Decisions

<decisions>
  <approved_decisions>
    <!-- Recorded during INNOVATE phase -->
  </approved_decisions>

  <open_decisions>
    <!-- Handled during INNOVATE phase -->
  </open_decisions>
</decisions>

---

## 5. RIPER-5 Execution Plan (Pillar 4: Loop)

<execution_plan>
  <phase name="Research" order="1">
    - [ ] Ingest task spec, domain invariants, and out-of-scope boundaries.
    - [ ] Read corresponding database schemas, Liquibase changesets, and security configurations.
    - [ ] Establish execution flow, boundaries, and source-of-truth conflicts.
    - [ ] Produce `research.md` artifact.
    <gate id="G0" label="Research Complete">
      - [ ] Current behavior understood and documented.
      - [ ] Execution flow traced.
      - [ ] No unresolved research blocker.
    </gate>
  </phase>

  <phase name="Innovate" order="2">
    - [ ] Generate 2–3 alternative approaches with trade-off matrix.
    - [ ] Produce `decision.md` artifact.
    <gate id="G1" label="Gate 1 — Decision Approved">
      - [ ] Options reviewed and trade-offs analyzed.
      - [ ] Selected option recorded in `decision.md`.
      - [ ] No blocking decision remains open.
      <approved_by>[AUTO: DELEGATED]</approved_by>
      <approved_date>2026-09-17</approved_date>
    </gate>
  </phase>

  <phase name="Plan" order="3">
    - [ ] Decompose into vertical slices with verifiers and rollback points.
    - [ ] Populate `plan.md` with scope contract and verification matrix.
    <gate id="G2" label="Gate 2 — Plan Approved">
      - [ ] Every slice has a verifier.
      - [ ] Allowed/forbidden file scope is defined.
      - [ ] Rollback point defined per slice.
      - [ ] Stop conditions defined.
      - [ ] Plan approved.
      <approved_by>[AUTO: DELEGATED]</approved_by>
      <approved_date>2026-09-17</approved_date>
    </gate>
  </phase>

  <phase name="Execute" order="4">
    - [ ] Implement each slice atomically.
    - [ ] Run verifier after each slice.
    - [ ] Inspect diff after each slice.
    - [ ] Update `state.md` after each slice.
  </phase>

  <phase name="Review" order="5">
    - [ ] Review full diff, behavior, architecture, data, security, regression.
    - [ ] Produce `review.md` with findings and verification matrix.
    <gate id="G3" label="Gate 3 — Review Passed">
      - [ ] All AC verified with evidence.
      - [ ] Residual risk accepted.
      - [ ] Review decision: PASS.
      - [ ] Ready for handoff.
      <approved_by>[AUTO: DELEGATED]</approved_by>
      <approved_date>2026-09-17</approved_date>
    </gate>
  </phase>
</execution_plan>

---

## 6. Guardrails & Escalation (Pillar 3 & 4: Harness)

<guardrails>
  <stop_conditions>
    - Missing business or policy decision.
    - Public API / DB schema change not declared in this spec.
    - New external dependency not declared in this spec.
    - Security policy change required.
    - Scope expansion beyond `<out_of_scope>`.
    - Retry budget exhausted on a recurring failure.
  </stop_conditions>

  <retry_budget max_attempts="3">
    Maximum 3 consecutive attempts per distinct failure symptom before halting.
  </retry_budget>
</guardrails>

</task_spec>
