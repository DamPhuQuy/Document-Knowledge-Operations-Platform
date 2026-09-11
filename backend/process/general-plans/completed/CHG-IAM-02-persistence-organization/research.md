# Research: CHG-IAM-02 Reorganize Persistence Secondary Adapter Structure

<research_context task_id="CHG-IAM-02" version="1.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. No source modifications. No implementation decisions. -->
<research_status>
  <phase>RESEARCH</phase>
  <mode>READ-ONLY</mode>
  <research_owner>@engineer</research_owner>
  <last_updated>2026-09-11</last_updated>
</research_status>

---

## 1. Current Behavior & Baseline Findings

<current_behavior>
  The `iam` subsystem's persistence adapter package (`src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence`) currently maintains 8 Java source files in a single flat directory:

  ### Confirmed Inventory & Categorization:
  1. **JPA ORM Entities:**
     - `UserJpaEntity.java` (table: `users`, `@ManyToMany` with `RoleJpaEntity`)
     - `RoleJpaEntity.java` (table: `roles`, `@ManyToMany` with `PermissionJpaEntity`)
     - `PermissionJpaEntity.java` (table: `permissions`)
     - `RefreshTokenJpaEntity.java` (table: `refresh_tokens`)
     *Role in Hexagonal Architecture:* Database-specific schema representations managed by Hibernate/JPA. Must not leak outside the persistence adapter layer.
  2. **Spring Data JPA Repository Interfaces:**
     - `SpringDataUserRepository.java` (extends `JpaRepository<UserJpaEntity, UUID>`, custom JPQL query `findByEmailIgnoreCaseWithRolesAndPermissions`)
     - `SpringDataRefreshTokenRepository.java` (extends `JpaRepository<RefreshTokenJpaEntity, UUID>`, finder `findByToken`)
     *Role in Hexagonal Architecture:* Framework-provided data access interfaces providing CRUD and query execution against relational tables.
  3. **Secondary Repository Adapters:**
     - `UserRepositoryAdapter.java` (`@Component`, implements `UserRepositoryPort`, injects `SpringDataUserRepository`, converts between JPA entities and Domain entities `User`, `Role`, `Permission`)
     - `RefreshTokenRepositoryAdapter.java` (`@Component`, implements `RefreshTokenRepositoryPort`, injects `SpringDataRefreshTokenRepository`, converts between JPA entities and Domain entity `RefreshToken`)
     *Role in Hexagonal Architecture:* Outbound secondary adapter implementations bridging application ports (`ports/outbound/`) to infrastructure technology.

  ### Confirmed Usage & Test Touchpoints:
  - `PersistenceAdaptersTest.java`: Directly tests `UserRepositoryAdapter` and `RefreshTokenRepositoryAdapter` with `@DataJpaTest` and `@Import`.
  - `AuthControllerTest.java`: An integration test that directly imports `UserJpaEntity`, `RoleJpaEntity`, `PermissionJpaEntity`, and `SpringDataUserRepository` for test database pre-seeding.
  - No domain model or application service imports anything from this persistence package (verified: zero clean architecture boundary violations).
  - Main application class `com.platform.app.AppApplication` is located at `com.platform.app`, meaning Spring Boot's default `@SpringBootApplication` component scan and entity scan cover all subpackages of `com.platform.app` automatically without requiring manual `@EntityScan` or `@EnableJpaRepositories` basePackage modifications.

  ### Architecture Documentation Status:
  - `process/context/architecture/architecture-template.md` currently models:
    ```
    └── secondary/
        ├── persistence/
        ├── messaging/
        └── external_services/
    ```
    It does not prescribe internal organization conventions for persistence, leading to inconsistent flat grouping where ORM entities, Spring Data repositories, and outbound port adapters mingle together.
  - `process/context/all-context.md` currently has a typographical path error:
    `<path>/contenxt/architecture/architecture.md</path>` which should point to `architecture/architecture-template.md`.
</current_behavior>

---

## 2. Identified Pain Points

<pain_points>
  1. **Cognitive Overhead & Clutter:** Finding entities vs. interfaces vs. adapters requires reading class names or declarations. As more aggregates (e.g. AuditLog, Department, Credentials) are added, a flat folder becomes unmanageable.
  2. **Architectural Ambiguity:** In Hexagonal Architecture, the "secondary adapter" is strictly the class implementing the Outbound Port (`UserRepositoryAdapter`). The JPA entity and Spring Data repository are implementation details internal to that adapter. Flat structure obscures this distinction.
  3. **Lack of Standard Blueprint:** Without an explicit blueprint in `architecture-template.md`, other developers or future AI agents might structure future modules differently (e.g., mixing or duplicating subpackage names).
</pain_points>

---

## 3. Invariants & Guardrails

<invariants>
  1. Domain models (`com.platform.app.iam.domain.model.*`) must remain 100% untouched and pure.
  2. Application outbound port interfaces (`com.platform.app.iam.application.ports.outbound.*`) must remain untouched.
  3. Adapters must remain Spring components implementing the respective outbound port contracts.
  4. Spring Boot component and repository scanning must continue resolving all entities and repositories seamlessly.
  5. Test suite must pass 100% after reorganization.
</invariants>

---

## 4. Exit Criteria

<exit_criteria>
  - [x] Full inventory of existing persistence files documented.
  - [x] Structural roles (Entity vs. Repository vs. Adapter) classified.
  - [x] External callers and test references mapped.
  - [x] Spring Boot scanning rules and architecture context documentation verified.
  - [x] Research exit criteria satisfied; ready for INNOVATE phase.
</exit_criteria>

</research_context>
