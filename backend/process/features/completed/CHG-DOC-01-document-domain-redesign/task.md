# Task: [CHG-DOC-01] Document Domain Model & Schema Redesign

<task_spec version="3.0" framework="RIPER-5">

<!-- ════════════════════════════════════════════
     SECTION 0 — TASK CONTROL (master state record)
     ════════════════════════════════════════════ -->
<task_control>
  <status>COMPLETED</status>  <!-- BACKLOG | ACTIVE | BLOCKED | PAUSED | REVIEW | COMPLETED | CANCELLED -->
  <spec_level>S3</spec_level>  <!-- S0=sketch | S1=defined | S2=verified | S3=locked -->
  <priority>P0</priority>  <!-- P0=urgent | P1=high | P2=normal | P3=low -->
  <risk>MEDIUM</risk>  <!-- LOW | MEDIUM | HIGH -->
  <estimated_story_points>3</estimated_story_points>
  <working_mode>DELEGATED</working_mode>  <!-- PAIR | DELEGATED (fast-track: continuous autonomous run) -->
  <current_phase>REVIEW</current_phase>  <!-- RESEARCH | INNOVATE | PLAN | EXECUTE | REVIEW -->
  <owner>@engineer</owner>
  <decision_owner>@engineer</decision_owner>
  <created>2026-09-16</created>
  <last_updated>2026-09-16</last_updated>
</task_control>

---

## 1. Specification (Pillar 1: Task / Spec)

<specification>
  <goal>
    Redesign the over-engineered Document domain model (originally having ~20 concepts), aligning the Domain POJO, JPA Entity, database schema (Liquibase), Application DTOs/services, and automated tests strictly with the core domain principle: "A document needs at least what information so that the system could comprehend it", modeling strictly what active use cases (UC-DOC-01) actually need.
  </goal>

  <current_behavior>
    - Document.java and DocumentJpaEntity.java have 19 attributes.
    - fileType ("PDF") duplicates mimeType ("application/pdf").
    - storageBucket is hardcoded in every DB row, leaking infrastructure config.
    - isS3Synced is a dead boolean flag.
    - metadata is an untyped "{}" JSON string without domain rules.
    - description and deletedAt are unused by active processing or use cases.
    - document_versions clones all file metadata from documents on initial upload without active revision use cases.
  </current_behavior>

  <expected_behavior>
    - Document domain model trimmed to 11 essential concepts required for system comprehension: id, title, originalFileName, contentType, fileSizeBytes, checksumSha256, storageKey, status, uploadedByUserId, departmentId, accessLevel, createdAt, updatedAt.
    - Redundant fields (fileType, storageBucket, isS3Synced, metadata, description, currentVersion, deletedAt) eliminated from Domain, JPA Entity, and Liquibase documents table.
    - Duplicate version insertion in initial upload removed or simplified.
    - Invariants and validations in Document.java remain strict and expressive.
    - Full regression test suite passes cleanly.
  </expected_behavior>

  <invariants>
    - Pure POJO Domain Model: No Spring, JPA, or AWS SDK imports in com.platform.app.document.domain.model.Document.
    - Streaming SHA-256 and S3 storage keys preserved.
    - AccessLevel (PUBLIC, INTERNAL, RESTRICTED, CONFIDENTIAL) and Department scoping preserved for authorization.
    - Zero data leakage, zero regression in existing test suite.
  </invariants>

  <acceptance_criteria>
    - [x] AC-1: Document.java contains only the essential 11 comprehension concepts and domain methods (markProcessing, markReady, markFailed).
    - [x] AC-2: DocumentJpaEntity.java and Liquibase changelog reflect the streamlined schema.
    - [x] AC-3: DocumentUploadService, DocumentMetadataService, and DocumentResponseDto updated consistently.
    - [x] AC-4: S3 storage upload and compensation logic intact.
    - [x] AC-5: All unit and slice tests updated and passing (./gradlew test).
    - [x] AC-6: Spotless code formatting passes (./gradlew spotlessCheck).
  </acceptance_criteria>

  <definition_of_ready>
    - [x] Target outcome and acceptance criteria are verifiable without guessing.
    - [x] Invariants and boundaries are explicit.
    - [x] Options formulated for INNOVATE phase.
  </definition_of_ready>
</specification>

---

## 2. Context Boundaries (Pillar 2: Context)

<context_boundaries>
  <target_files>
    - `src/main/java/com/platform/app/document/domain/model/Document.java`
    - `src/main/java/com/platform/app/document/domain/model/DocumentStatus.java`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/entity/DocumentJpaEntity.java`
    - `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentRepositoryAdapter.java`
    - `src/main/java/com/platform/app/document/application/services/DocumentMetadataService.java`
    - `src/main/java/com/platform/app/document/application/services/DocumentUploadService.java`
    - `src/main/java/com/platform/app/document/application/dto/DocumentResponseDto.java`
    - `src/main/java/com/platform/app/document/application/event/DocumentUploadedEvent.java`
    - `src/main/resources/db/changelog/changes/003-create-document-tables.yaml`
    - `docs/specs/database/schema.dbml`
    - `docs/specs/database/modules/02_document_management.dbml`
    - `src/test/java/com/platform/app/document/**`
  </target_files>
</context_boundaries>

</task_spec>
