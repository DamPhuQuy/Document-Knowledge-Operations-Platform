# Task: ARCH-CLEAN-SOLID-SIMPLIFY Streamline Clean Architecture & SOLID Principles

<task_spec version="3.0" framework="RIPER-5">

<!-- ════════════════════════════════════════════
     SECTION 0 — TASK CONTROL (master state record)
     ════════════════════════════════════════════ -->
<task_control>
  <status>COMPLETED</status>
  <spec_level>S3</spec_level>
  <priority>P1</priority>
  <risk>LOW</risk>
  <estimated_story_points>2</estimated_story_points>
  <working_mode>DELEGATED</working_mode>
  <current_phase>REVIEW</current_phase>
  <owner>@engineer</owner>
  <decision_owner>@engineer</decision_owner>
  <created>2026-09-13</created>
  <last_updated>2026-09-13</last_updated>
</task_control>

---

## 1. Specification (Pillar 1: Task / Spec)

<specification>
  <goal>
    Audit and streamline the backend architecture to eliminate incidental overengineering (anemic ID wrappers, unnecessary micro-ports, dual-model JPA-to-domain mapping ceremony, and redundant DTO transformations) while preserving core Clean Architecture layer boundaries, SOLID design principles, and 100% passing test coverage.
  </goal>

  <current_behavior>
    - The IAM module currently contains ~35+ Java source files for a single endpoint (`POST /api/v1/auth/login`).
    - Anemic 1-field record wrappers (`UserId`, `RoleId`, `DepartmentId`, `UserFlags`, `AuditMetadata`) introduce repetitive unwrapping/wrapping boilerplate (`user.getId().value()`, `user.getFlags().enabled()`).
    - Micro-ports (`PasswordEncoderPort`, `EventPublisherPort`, `TokenProviderPort`, `AccountLockoutPort`) wrap standard Spring/Java abstractions with single-implementation adapters, forcing `LoginService` to depend on 6 constructor parameters.
    - Strict theoretical Hexagonal DDD creates dual data hierarchies: every database entity has a JPA entity (`UserJpaEntity`), a Domain model (`User`), a Repository Port, an Adapter (`UserRepositoryAdapter`), and a REST DTO, requiring verbose manual mapping code.
    - Login flow traverses 4 data representations: `LoginRequest` -> `LoginCommand` -> `AuthTokensDto` -> `AuthResponse`.
  </current_behavior>

  <expected_behavior>
    - Clean Architecture is pragmatic, readable, and developer-friendly: clear separation between Web/REST (Inbound), Core Application/Domain Services, and Persistence/Security (Outbound).
    - SOLID principles (SRP, OCP, LSP, ISP, DIP) are naturally upheld without micro-port ceremony.
    - Domain models / entities are direct, readable, and expressive without Russian-doll record nesting.
    - End-to-end functionality remains intact and all existing 25 unit/integration tests continue to pass.
  </expected_behavior>

  <actor_authorization>
    System User and System Administrator via REST API endpoints.
  </actor_authorization>

  <invariants>
    - INVARIANT 1: Zero regression on authentication security rules (Rule B1: BCrypt >= 12, Rule B3: 30-day refresh token, Rule B4: 5 consecutive failed attempts lock for 15 min).
    - INVARIANT 2: Clean separation of concerns preserved (Web/Controller layer does not contain business logic; services do not depend directly on HTTP requests).
    - INVARIANT 3: All 25 existing unit/integration tests must pass cleanly (`./gradlew test`).
  </invariants>

  <out_of_scope>
    - Modifying database schemas or Liquibase migrations (`src/main/resources/db/changelog/`).
    - Adding new endpoints or premature business features.
    - Changing external REST API contracts (`/api/v1/auth/login` request/response JSON fields must remain identical).
  </out_of_scope>

  <acceptance_criteria>
    - [ ] AC-1: Research document thoroughly analyzes current codebase patterns, categorizing overengineering vs. essential Clean Architecture / SOLID patterns.
    - [ ] AC-2: Concrete simplification proposals formulated with trade-off matrix in `decision.md` (INNOVATE phase).
    - [ ] AC-3: Codebase file count and boilerplate reduced while keeping code intuitive, readable, and SOLID.
    - [ ] AC-4: `./gradlew test` passes 100% with zero regressions.
  </acceptance_criteria>

  <!-- Definition-of-Ready (DoR) Gate -->
  <definition_of_ready>
    - [x] Target outcome and acceptance criteria are verifiable without guessing.
    - [x] Invariants and <out_of_scope> boundaries are explicit.
    - [x] Open questions resolved or scheduled in decision.md.
  </definition_of_ready>
</specification>

---

## 2. Context Boundaries (Pillar 2: Context)

<context_boundaries>
  <target_files>
    - `src/main/java/com/platform/app/iam/` — [Audit and review current structure]
    - `src/test/java/com/platform/app/iam/` — [Existing test coverage verification]
  </target_files>

  <source_of_truth>
    <requirement>[`docs/specs/business/MVP.md`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/docs/specs/business/MVP.md)</requirement>
    <architecture>[`docs/specs/business/06_engineering_handoff.md`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/docs/specs/business/06_engineering_handoff.md)</architecture>
    <existing_behavior>[`backend/src/test/java/com/platform/app/iam/`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/test/java/com/platform/app/iam/)</existing_behavior>
  </source_of_truth>
</context_boundaries>

---

## 3. Verification Strategy

<verification_strategy>
| AC / Risk | Evidence required | Verifier |
|---|---|---|
| AC-1 | Completed `research.md` with evidence classification | Gate G0 Audit |
| AC-2 | Trade-off matrix and simplification options | `decision.md` in INNOVATE |
| AC-3 | Reduced class count and streamlined signatures | Code Diff Inspection |
| AC-4 | 25/25 tests passing | `./gradlew test` |
</verification_strategy>

</task_spec>
