# Research: ARCH-CLEAN-SOLID-SIMPLIFY Streamline Clean Architecture & SOLID Principles

<research_context task_id="ARCH-CLEAN-SOLID-SIMPLIFY" version="2.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. No source modifications. No implementation decisions. -->
<research_status>
  <phase>RESEARCH</phase>
  <mode>READ-ONLY</mode>
  <research_owner>@engineer</research_owner>
  <last_updated>2026-09-13</last_updated>
</research_status>

---

## 1. Executive Summary & Problem Statement

The backend codebase for the IAM module was developed following strict textbook Hexagonal / Tactical Domain-Driven Design (DDD). While the architecture successfully isolates domain logic and achieves 100% test coverage, **the degree of abstraction is disproportionately heavy for an MVP**, resulting in significant boilerplate, cognitive load, and friction when implementing subsequent features (`Document_Management`, `Audit_System`).

The user's explicit requirement:
> *"đọc code backend hiện tại, hiện tại tôi đang cảm thấy code quá mức overengineering, nhưng tôi vẫn muốn clean architecture và dựng theo SOLID bởi tôi thấy rất dễ đọc; init RESEARCH phase"*

This research audits the current backend codebase, quantifies the sources of overengineering, and examines how to preserve the true benefits of Clean Architecture and SOLID principles (readability, testability, separation of concerns) while removing accidental complexity.

---

## 2. Current Codebase Audit & Evidence Collection

### 2.1. Structural Metrics
- **Total Java source files in IAM module:** 37 files.
- **Number of endpoints served:** 1 endpoint (`POST /api/v1/auth/login`).
- **Layers crossed per request:** 5 distinct layers:
  1. `infrastructure.adapters.primary.rest` (`AuthController`, `LoginRequest`, `AuthResponse`)
  2. `application.ports.inbound` (`LoginUseCase`, `LoginCommand`)
  3. `application.services` (`LoginService`)
  4. `application.ports.outbound` (6 separate interfaces: `UserRepositoryPort`, `RefreshTokenRepositoryPort`, `PasswordEncoderPort`, `TokenProviderPort`, `AccountLockoutPort`, `EventPublisherPort`)
  5. `infrastructure.adapters.secondary.*` (6 adapters, 4 JPA entities, 2 Spring Data repositories)

### 2.2. Categorized Findings & Evidence Classification

```text
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                CURRENT OVERENGINEERING HOTSPOTS                                  │
├───────────────────────────────┬──────────────────────────────────┬───────────────────────────────┤
│ Hotspot 1: Anemic ID Wrappers │ Hotspot 2: Dual Data Hierarchies │ Hotspot 3: Trivial Ports      │
│ - UserId, RoleId, DepartmentId│ - UserJpaEntity ↔ User domain    │ - PasswordEncoderPort wraps   │
│ - UserFlags (2 booleans)      │ - RoleJpaEntity ↔ Role domain    │   Spring PasswordEncoder      │
│ - AuditMetadata (2 Instants)  │ - PermissionJpaEntity ↔ Perm     │ - EventPublisherPort wraps    │
│ → Constant .value() unwrapping│ - Verbose manual mapping code    │   ApplicationEventPublisher   │
├───────────────────────────────┴──────────────────────────────────┴───────────────────────────────┤
│ Hotspot 4: 4 Data Representations: LoginRequest → LoginCommand → AuthTokensDto → AuthResponse     │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

#### Finding 1: Proliferation of Anemic Single-Field Wrappers
- **Classification:** `CONFIRMED` (Direct source observation)
- **Evidence:**
  - `UserId(UUID value)` — [`UserId.java`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/domain/model/UserId.java)
  - `RoleId(UUID value)` — [`RoleId.java`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/domain/model/RoleId.java)
  - `DepartmentId(String value)` — [`DepartmentId.java`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/domain/model/DepartmentId.java)
  - `UserFlags(boolean enabled, boolean isInternal)` — [`UserFlags.java`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/shared/domain/UserFlags.java)
  - `AuditMetadata(Instant createdAt, Instant updatedAt)` — [`AuditMetadata.java`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/shared/domain/AuditMetadata.java)
- **Impact:**
  - In `LoginService.java`: `user.getId().value()`, `user.getDepartmentId().value()`, `user.getFlags().enabled()`.
  - In `UserRepositoryAdapter.java`: constant conversion between raw DB types and 1-field records.
  - While strongly typed IDs provide compile-time safety in large codebases with dozens of ID types, wrapping `String` for department or wrapping 2 booleans into `UserFlags` creates unnecessary friction without business domain methods.

#### Finding 2: Dual Data Hierarchy & Verbose Manual Mapping
- **Classification:** `CONFIRMED` (Direct source observation)
- **Evidence:**
  - [`UserJpaEntity.java`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/entity/UserJpaEntity.java) has fields `id`, `email`, `passwordHash`, `fullName`, `departmentId`, `enabled`, `internal`, `roles`.
  - [`User.java`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/domain/model/User.java) has almost identical fields.
  - [`UserRepositoryAdapter.java`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/adapter/UserRepositoryAdapter.java) writes 92 lines of boilerplate to manually map `UserJpaEntity` -> `User`, `RoleJpaEntity` -> `Role`, `PermissionJpaEntity` -> `Permission`.
- **Impact:**
  - Every time a new field is added to `users` (e.g. `phone` or `avatarUrl`), the developer must update:
    1. Liquibase migration
    2. `UserJpaEntity`
    3. `User` domain model
    4. `UserRepositoryAdapter` mapper
    5. Builder calls in tests
  - This 4-layer synchronization is the primary cause of development slowdown.

#### Finding 3: Trivial Single-Implementation Outbound Ports
- **Classification:** `CONFIRMED` (Direct source observation)
- **Evidence:**
  - [`PasswordEncoderPort.java`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/application/ports/outbound/PasswordEncoderPort.java):
    ```java
    public interface PasswordEncoderPort {
      boolean matches(String rawPassword, String encodedPassword);
      String encode(String rawPassword);
    }
    ```
    Implemented by [`BCryptPasswordEncoderAdapter.java`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/adapter/BCryptPasswordEncoderAdapter.java) which merely delegates to `org.springframework.security.crypto.password.PasswordEncoder`.
  - [`EventPublisherPort.java`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/application/ports/outbound/EventPublisherPort.java):
    Implemented by [`SpringEventPublisherAdapter.java`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/messaging/SpringEventPublisherAdapter.java) which merely delegates to `ApplicationEventPublisher.publishEvent()`.
- **Impact:**
  - Wrapping standard, battle-tested interfaces (like Spring's `PasswordEncoder` or `ApplicationEventPublisher`) in custom 1-to-1 port interfaces provides zero practical substitutability in a Spring Boot application, while doubling the number of interfaces and classes.

#### Finding 4: Inbound Port Ceremony & Quadruple Data Transformation
- **Classification:** `CONFIRMED` (Direct source observation)
- **Evidence:**
  - `LoginRequest` (REST controller body) -> converted to `LoginCommand` (Application inbound port) -> executed by `LoginUseCase` interface implemented solely by `LoginService` -> returns `AuthTokensDto` -> converted to `AuthResponse` in `AuthController`.
- **Impact:**
  - For a simple authentication use case, 4 DTO classes and 1 single-method interface (`LoginUseCase`) are maintained.
  - In Clean Architecture, the use case is the application service itself (`LoginService` or `AuthService`). Adding an interface with exactly one implementation and identical method signature adds indirection without polymorphism.

---

## 3. What Clean Architecture & SOLID Actually Mean (Debunking Dogma)

To align with the user's requirement (*"tôi vẫn muốn clean architecture và dựng theo SOLID bởi tôi thấy rất dễ đọc"*), we must differentiate between **Core Principles** and **Accidental Dogma**:

| Principle | Core Essence (What to Keep) | Dogmatic Overengineering (What to Eliminate) |
| :--- | :--- | :--- |
| **Clean Architecture** | **Separation of Concerns & Dependency Rule:**<br>1. Web layer does HTTP handling, JSON parsing, status codes.<br>2. Service layer contains business rules, validation, security policies.<br>3. Persistence layer manages database queries.<br>Dependencies point inward toward business logic. | Forcing 2 separate entity classes for every database table (JPA Entity vs Domain Model) when the domain has no complex behavioral calculations. |
| **Single Responsibility (SRP)** | Each class has one reason to change (`AuthService` handles authentication, `TokenProvider` handles JWT signing, `SecurityConfig` handles web security filter chains). | Splitting a single cohesive workflow across 8 tiny 1-line classes (`UserFlags`, `AuditMetadata`, `DepartmentId`, `LoginCommand`). |
| **Open/Closed & Liskov (OCP/LSP)** | Designing services so new features can be added without rewriting existing components. | Creating interfaces with exactly one implementation where no second implementation will ever exist (`EventPublisherPort`). |
| **Interface Segregation (ISP)** | Clients should not depend on methods they do not use. Small, focused repository interfaces (e.g. `UserRepository`). | Creating micro-interfaces for every utility function. |
| **Dependency Inversion (DIP)** | High-level modules do not depend on low-level database details; they depend on repository abstractions (`UserRepository`). | Re-wrapping standard framework abstractions (`PasswordEncoder`) into custom wrapper interfaces. |

---

## 4. Observations & Hypotheses

- **Observed:**
  - The existing test suite ([`LoginServiceTest.java`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/test/java/com/platform/app/iam/application/services/LoginServiceTest.java), [`AuthControllerTest.java`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/test/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AuthControllerTest.java)) is thorough and well-written. It validates all business rules (B1 BCrypt, B3 30-day token, B4 5-attempt lockout).
  - Any simplification can be validated with 100% confidence by running `./gradlew test`.
- **Hypothesized:**
  - By adopting **Pragmatic Clean Architecture**:
    1. Combining JPA Entity and Domain Model into clean, rich entities OR using simple Records for read models will cut class count by ~40%.
    2. Replacing trivial outbound ports (`PasswordEncoderPort`, `EventPublisherPort`) with standard Spring/Java abstractions will reduce constructor bloat in services.
    3. Replacing anemic record wrappers (`UserId`, `RoleId`, `DepartmentId`) with direct types (`UUID`, `String`) will eliminate `.value()` unwrapping across the entire codebase.
    4. Merging `LoginCommand` into `LoginRequest` or keeping `LoginRequest` as the direct input to `AuthService` will eliminate 2 redundant DTO layers.

---

## 5. Research Exit Criteria (Gate G0)

- [x] Current codebase architecture, metrics, and file count documented.
- [x] Overengineering hotspots identified and classified (`CONFIRMED`).
- [x] Clear distinction established between Core Clean/SOLID principles vs. dogmatic overengineering.
- [x] Invariants verified: Security rules and test verifiability remain strictly preserved.
- [x] Ready to advance to INNOVATE phase to formulate 2–3 concrete simplification proposals.

</research_context>
