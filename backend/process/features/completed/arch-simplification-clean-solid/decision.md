# Architecture Decision: ARCH-CLEAN-SOLID-SIMPLIFY Streamline Clean Architecture & SOLID Principles

<decision_context task_id="ARCH-CLEAN-SOLID-SIMPLIFY" version="2.0" framework="RIPER-5">

<!-- INNOVATE PHASE. Formulate 2-3 viable options. Auto-certify Gate G1 in DELEGATED mode. -->
<decision_status>
  <phase>INNOVATE</phase>
  <mode>DELEGATED</mode>
  <decision_owner>@engineer</decision_owner>
  <last_updated>2026-09-13</last_updated>
</decision_status>

---

## 1. Problem Framing & Architectural Objectives

The objective is to eliminate accidental complexity, repetitive boilerplate, and layer bloat in the Spring Boot backend while strictly preserving:
1. **Clean Architecture layer boundaries** (Web Controller -> Application Service -> Repository / Infrastructure).
2. **SOLID design principles** (Single Responsibility, Dependency Inversion, Open-Closed).
3. **100% test pass rate** with zero regression on security rules (BCrypt >= 12, 5-failure lockout, 30-day refresh token, JWT claims).

---

## 2. Viable Architectural Options

### Option 1: Conservative Trimming (Minimalist Hexagonal)
- Retain all 6 outbound ports and adapters.
- Retain dual data models (`UserJpaEntity` vs `User`).
- Only remove anemic 1-field wrappers (`UserId`, `RoleId`, `DepartmentId`, `UserFlags`, `AuditMetadata`).
- **Pros:** Minimal change to existing classes.
- **Cons:** Leaves 32+ files for 1 endpoint; does not solve constructor bloat (6 ports) or 92-line repetitive JPA-to-domain mapping.

### Option 2 (RECOMMENDED): Pragmatic Clean Architecture & Spring Idiomatic SOLID
- **Direct Domain Typing:** Replace 1-field record wrappers (`UserId`, `RoleId`, `DepartmentId`) with native `UUID` and `String`. Inline `enabled` and `isInternal` booleans directly on `User`.
- **Adopt Framework-Standard Abstractions:** Inject Spring Security's `PasswordEncoder` and Spring's `ApplicationEventPublisher` directly into `LoginService`. Eliminate trivial 1-line wrapper ports (`PasswordEncoderPort`, `BCryptPasswordEncoderAdapter`, `EventPublisherPort`, `SpringEventPublisherAdapter`).
- **Preserve Clean Domain & Dependency Inversion:** Keep `User`, `Role`, `Permission`, and `RefreshToken` as pure domain models. Keep `UserRepositoryPort` and `RefreshTokenRepositoryPort` as clean domain abstractions (allowing isolated unit testing of `LoginService` without Spring Data or database).
- **Streamline Inbound Flow:** Keep `LoginUseCase` interface and `LoginCommand` for clean contract testing, but simplify DTOs and eliminate redundant nested mappings.
- **Retain Stateful Account Lockout Port:** Keep `AccountLockoutPort` + `InMemoryAccountLockoutAdapter` because it isolates the in-memory cache/policy from the service and allows future plug-and-play with Redis.
- **Pros:**
  - Reduces file count by ~35-40% (eliminates 12+ redundant adapter/port/wrapper classes).
  - Reduces `LoginService` constructor from 6 parameters down to 4.
  - 100% preserves Clean Architecture: domain models remain framework-independent; services remain testable with Mockito; Web layer remains decoupled from persistence.
  - Dramatically improves readability and developer velocity for upcoming `Document_Management` and `Audit_System` modules.
- **Cons:** Requires updating constructor signatures and test mocks.

### Option 3: Monolithic Active Record (Collapse Domain into JPA)
- Remove `User` domain model completely; use `UserJpaEntity` everywhere including services and controllers. Remove repository ports.
- **Pros:** Lowest file count (~15 files).
- **Cons:** Violates Clean Architecture; domain logic becomes tied to JPA annotations and Hibernate session lifecycle; harder to unit test without database mock or Testcontainers.

---

## 3. Trade-Off Matrix

| Dimension | Option 1 (Conservative) | Option 2 (Pragmatic Clean - Recommended) | Option 3 (Monolithic) |
| :--- | :---: | :---: | :---: |
| **Clean Architecture Integrity** | High | **High** | Poor (Coupled to JPA) |
| **SOLID Adherence (DIP, SRP)** | High (Overengineered) | **Optimal (Balanced)** | Low (Entities carry DB annotations) |
| **Readability & Simplicity** | Low (Too many hops) | **High (Direct & Idiomatic)** | Moderate |
| **Boilerplate Reduction** | ~10% | **~40%** | ~60% |
| **Velocity for DMS / S3 Slices** | Slow (High friction) | **Fast (Low friction)** | Fast |
| **Unit Test Independence** | Full | **Full** | Partial (JPA coupling) |

---

## 4. Architectural Decision & Gate 1 Sign-Off

<engineer_decision>
  <selected_option>Option 2: Pragmatic Clean Architecture & Spring Idiomatic SOLID</selected_option>
  <rationale>
    Option 2 delivers the exact balance requested by the user: it eliminates accidental overengineering (anemic ID wrappers, trivial port re-wrapping) while maintaining the pristine layer separation, unit testability, and SOLID principles of Clean Architecture.
  </rationale>
  <gate_1_signoff status="PASS">
    [AUTO: DELEGATED] Gate 1 certified under autonomous fast-track mode. Advancing to PLAN phase.
  </gate_1_signoff>
</engineer_decision>

</decision_context>
