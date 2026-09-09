# Decision: DEC-IAM-01 Architecture & Implementation Decisions for UC-IAM-01

<technical_decision task_id="UC-IAM-01" dec_id="DEC-IAM-01" version="2.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. Present options. PAIR: engineer decides. DELEGATED: agent auto-selects optimal recommendation and advances. -->
<decision_status>
  <phase>INNOVATE</phase>
  <mode>READ-ONLY</mode>
  <decision_owner>@engineer</decision_owner>
  <last_updated>2026-09-09</last_updated>
</decision_status>

---

## 1. Context

<context>
  <task>[task.md](task.md)</task>
  <research_artifact>[research.md](research.md)</research_artifact>
  <constraints>
    - Standard Architecture Template: Must strictly comply with `process/context/architecture/architecture-template.md` (Clean Hexagonal DDD).
    - B1: Password must be verified using BCrypt with work factor >= 12.
    - B2: Access Token expiration must be short-lived (configurable via `app.jwt.expiration-ms`).
    - B3: Refresh Token has a 30-day lifetime and supports immediate revocation (`revoked = TRUE`).
    - B4: Account is temporarily locked after 5 consecutive failed login attempts within 15 minutes.
    - Architectural Direction: UC-AUDIT-01 is deferred; IAM must decouple audit logging via domain events through an outbound port.
    - Hexagonal Rules:
      - Domain depends on nothing outside Domain.
      - Application depends on Domain, contains use cases, and owns Inbound and Outbound Ports.
      - Infrastructure implements Outbound Ports (Secondary Adapters) and calls Inbound Ports (Primary Adapters).
  </constraints>
</context>

---

## 2. Decision Required

<decision_question>
  How should UC-IAM-01 map to the Standard Hexagonal Architecture (`architecture-template.md`) across Inbound/Outbound Ports, Secondary Adapters (Security, Persistence, Messaging), and Account Lockout?
</decision_question>

---

## 3. Options for Decision 1: Hexagonal Port & Adapter Mapping

<options>

  <option id="A">
    <approach>
      Strict Clean Hexagonal Architecture (per architecture-template.md):
      - Domain: `domain/model/` (User, Role, Permission, RefreshToken, value objects) and `domain/exception/` (AccountDisabledException, AccountLockedException, InvalidCredentialsException).
      - Application:
        - `ports/inbound/LoginUseCase` (Inbound Port) & `ports/inbound/LoginCommand`.
        - `ports/outbound/UserRepositoryPort`, `RefreshTokenRepositoryPort`, `PasswordEncoderPort`, `TokenProviderPort`, `AccountLockoutPort`, `EventPublisherPort` (Outbound Ports).
        - `services/LoginService` (Application Service implementing `LoginUseCase`).
        - `dto/AuthTokensDto`, `UserProfileDto`, domain events.
      - Infrastructure:
        - Primary Adapter: `adapters/primary/rest/AuthController` (invokes Inbound Port), `LoginRequest`, `AuthResponse`, `RestExceptionHandler`.
        - Secondary Adapters:
          - `adapters/secondary/persistence/`: `UserJpaEntity`, `RefreshTokenJpaEntity`, Spring Data repos, `UserRepositoryAdapter`, `RefreshTokenRepositoryAdapter`.
          - `adapters/secondary/security/`: `BCryptPasswordEncoderAdapter` (strength 12), `JwtTokenProviderAdapter` (JJWT 0.12.6), `InMemoryAccountLockoutAdapter`, `SecurityConfig`.
          - `adapters/secondary/messaging/`: `SpringEventPublisherAdapter` (dispatches login success/failure domain events).
    </approach>
    <advantages>
      - 100% compliant with `process/context/architecture/architecture-template.md`.
      - Pure separation of concerns: Application layer has zero dependencies on JPA, Spring Web, JJWT, or Spring Security.
      - High testability: Use case can be tested in isolation using pure mock ports without any Spring/DB overhead.
      - Maximum maintainability and adherence to Dependency Inversion Principle.
    </advantages>
    <disadvantages>
      - Requires explicit port interfaces and adapters (slightly more boilerplate classes).
    </disadvantages>
    <complexity>MEDIUM</complexity>
    <compatibility>HIGH (Exact match with architectural standard)</compatibility>
    <concurrency_transaction_risk>LOW</concurrency_transaction_risk>
    <testability>HIGH (Trivial to unit test via plain mockito on ports)</testability>
    <maintainability>VERY HIGH</maintainability>
  </option>

  <option id="B">
    <approach>
      Hybrid Layered Architecture (Domain-owned repository interfaces, direct Spring infrastructure dependencies in Application):
      - Repository interfaces placed in `domain/repository/`.
      - Application services directly inject Spring Security `PasswordEncoder`, JJWT classes, and Spring's `ApplicationEventPublisher`.
      - No explicit outbound ports for security or messaging.
    </approach>
    <advantages>
      - Fewer interface declarations.
    </advantages>
    <disadvantages>
      - Violates `architecture-template.md` Hexagonal and Boundary rules: "Application owns inbound and outbound ports", "Technology-specific types should not leak into Application".
      - Couples application use case directly to third-party libraries (JJWT, Spring Security).
    </disadvantages>
    <complexity>LOW</complexity>
    <compatibility>LOW (Breaches standard architecture template)</compatibility>
    <concurrency_transaction_risk>LOW</concurrency_transaction_risk>
    <testability>MEDIUM</testability>
    <maintainability>MEDIUM</maintainability>
  </option>

</options>

---

## 4. Options for Decision 2: Account Lockout Implementation (Rule B4)

<options>

  <option id="A">
    <approach>
      In-Memory Sliding-Window Lockout Adapter (`InMemoryAccountLockoutAdapter` implementing `AccountLockoutPort`):
      - Uses `ConcurrentHashMap` with timestamped failed attempt sliding window (15-minute TTL).
      - Atomic counter increments and thread-safe eviction.
      - Configurable `Clock` instance for deterministic unit testing.
    </approach>
    <advantages>
      - Zero schema modifications to baseline `002-create-iam-tables.yaml`.
      - Sub-millisecond execution; eliminates database round-trip overhead on failed logins.
      - Prevents DB row-lock contention on `users` table during credential stuffing / brute-force attacks.
      - Fully isolated behind `AccountLockoutPort` (can be swapped for Redis in cluster environments without altering Application use case).
    </advantages>
    <disadvantages>
      - In single-instance in-memory setup, state resets on service restart.
    </disadvantages>
    <complexity>LOW</complexity>
    <compatibility>HIGH</compatibility>
    <concurrency_transaction_risk>LOW</concurrency_transaction_risk>
    <testability>HIGH</testability>
    <maintainability>HIGH</maintainability>
  </option>

  <option id="B">
    <approach>
      Database-Backed Lockout Adapter:
      - Adds a new Liquibase changeset `009` with `failed_attempts` and `locked_until` on `users` table.
      - Persistence adapter updates `users` table on each failed login.
    </approach>
    <advantages>
      - Distributed state across restarts without external cache.
    </advantages>
    <disadvantages>
      - Requires mutating database schema via additional changeset.
      - Creates write transactions on every failed password attempt.
    </disadvantages>
    <complexity>MEDIUM</complexity>
    <compatibility>MEDIUM</compatibility>
    <concurrency_transaction_risk>MEDIUM</concurrency_transaction_risk>
    <testability>MEDIUM</testability>
    <maintainability>MEDIUM</maintainability>
  </option>

</options>

---

## 5. Trade-off Matrix

<tradeoff_matrix>

### Decision 1: Architecture Structure Alignment

| Criterion | Option A (Strict Hexagonal per template) | Option B (Hybrid Layered) |
|---|:---:|:---:|
| Architecture Compliance | 5 | 2 |
| Port Decoupling | 5 | 2 |
| Testability | 5 | 3 |
| Maintainability | 5 | 3 |
| Simplicity | 4 | 5 |

### Decision 2: Account Lockout Strategy

| Criterion | Option A (In-Memory Sliding Window Adapter) | Option B (DB Schema Mutation) |
|---|:---:|:---:|
| Schema Stability | 5 | 2 |
| Performance / Latency | 5 | 3 |
| Brute-Force Resilience | 5 | 2 |
| Testability | 5 | 4 |
| Multi-Instance Persistence | 3 | 5 |

<!-- Score: 1 (poor) → 5 (excellent) -->
</tradeoff_matrix>

---

## 6. Recommendation

<recommendation>
  1. **Architecture & Port Alignment:** Adopt **Option A (Strict Clean Hexagonal Architecture)**.
     - Fully adhere to the newly provided `process/context/architecture/architecture-template.md`.
     - `application/ports/inbound/LoginUseCase` defines the primary contract invoked by `infrastructure/adapters/primary/rest/AuthController`.
     - `application/ports/outbound/` defines the secondary contracts (`UserRepositoryPort`, `RefreshTokenRepositoryPort`, `PasswordEncoderPort`, `TokenProviderPort`, `AccountLockoutPort`, `EventPublisherPort`) implemented by `infrastructure/adapters/secondary/`.
     - Application use cases remain 100% pure Java, decoupled from Spring MVC, JPA, JJWT, and Spring Security.
  2. **Account Lockout:** Adopt **Option A (In-Memory Sliding Window Adapter)**.
     - Implements `AccountLockoutPort` in `infrastructure/adapters/secondary/security/InMemoryAccountLockoutAdapter`.
     - Preserves the baseline Liquibase changelog without schema pollution; provides lightning-fast checks resilient to brute-force DDoS.
  3. **Audit Decoupling:**
     - Define `EventPublisherPort` in `application/ports/outbound/`.
     - Implement `SpringEventPublisherAdapter` in `infrastructure/adapters/secondary/messaging/` publishing `UserLoginSuccessEvent` and `UserLoginFailedEvent`.
     - Provides clean boundary for future `UC-AUDIT-01` subscription.
</recommendation>

---

## 7. Implementation Decision

<!-- PAIR mode: Completed by engineer before Gate 1 passes. -->
<engineer_decision>
  <selected_option>Option A (Strict Clean Hexagonal Architecture per architecture-template.md, In-Memory Sliding-Window Lockout Adapter, EventPublisherPort for Audit Decoupling)</selected_option>
  <rationale>Approved by engineer. Enforces strict hexagonal layering, keeps domain and application layers completely free from infrastructure frameworks, and preserves database schema stability.</rationale>
  <rejected_alternatives>
    - Option B for Architecture: Rejected because it violates the standard module architecture template and leaks frameworks into Application layer.
    - Option B for Lockout: Rejected because modifying database schema is unnecessary and introduces write lock contention on brute-force attempts.
  </rejected_alternatives>
</engineer_decision>

---

## 8. Constraints Created by This Decision

<constraints_created>
  - Package structure must strictly mirror `process/context/architecture/architecture-template.md`:
    - `com.platform.app.iam.domain.model`
    - `com.platform.app.iam.domain.exception`
    - `com.platform.app.iam.application.ports.inbound`
    - `com.platform.app.iam.application.ports.outbound`
    - `com.platform.app.iam.application.services`
    - `com.platform.app.iam.application.dto`
    - `com.platform.app.iam.infrastructure.adapters.primary.rest`
    - `com.platform.app.iam.infrastructure.adapters.secondary.persistence`
    - `com.platform.app.iam.infrastructure.adapters.secondary.security`
    - `com.platform.app.iam.infrastructure.adapters.secondary.messaging`
  - No infrastructure or framework imports (`jakarta.persistence.*`, `org.springframework.security.*`, `io.jsonwebtoken.*`) permitted in `domain` or `application`.
</constraints_created>

---

## 9. Evidence Still Required

<evidence_required>
  - Verify spotless formatting compliance with deep hexagonal package hierarchy.
  - Verify Spring component scanning picks up all adapters and configuration classes under `com.platform.app.iam.infrastructure`.
</evidence_required>

---

## Gate 1 — Decision Approved

<gate id="G1">
  - [x] All options presented and trade-offs analyzed.
  - [x] Selected option recorded above.
  - [x] No blocking business / schema / security decision remains open.
  <approved_by>@engineer</approved_by>
  <approved_date>2026-09-09</approved_date>
</gate>

</technical_decision>
