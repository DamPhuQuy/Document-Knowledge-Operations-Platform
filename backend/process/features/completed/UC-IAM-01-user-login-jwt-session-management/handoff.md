# Handoff: UC-IAM-01 User Login & JWT Session Lifecycle Management

<handoff task_id="UC-IAM-01" version="2.0" framework="RIPER-5">

<!-- Final projection. Short. Do not duplicate research/plan/review artifacts. -->
<!-- Answer: What changed? Why? What proves it? What remains risky? -->

<handoff_status>
  <review_decision>PASS</review_decision>
  <review_artifact>[review.md](review.md)</review_artifact>
  <completed_date>2026-09-09</completed_date>
</handoff_status>

---

## 1. What Changed

<what_changed>
  Implemented complete end-to-end user authentication and JWT session management following strict Hexagonal Clean Architecture and Domain-Driven Design (DDD). Provides a public REST endpoint (`POST /api/v1/auth/login`) that validates credentials against BCrypt hashes (work factor 12), enforces account lockout (5 failed attempts within 15 minutes), generates short-lived stateless JWT access tokens embedding resolved user roles/permissions, generates and persists 30-day cryptographically secure opaque refresh tokens, and publishes decoupled domain events for login audit trail ingestion.
</what_changed>

<main_changes>
  - `src/main/java/com/platform/app/iam/domain/model/**` — Pure domain models (`User`, `Role`, `Permission`, `RefreshToken`, `UserId`, `RoleId`, `DepartmentId`) and domain exceptions (`AccountDisabledException`, `AccountLockedException`, `InvalidCredentialsException`).
  - `src/main/java/com/platform/app/iam/application/ports/inbound/**` — Inbound driving port [`LoginUseCase`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/application/ports/inbound/LoginUseCase.java) and input model [`LoginCommand`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/application/ports/inbound/LoginCommand.java).
  - `src/main/java/com/platform/app/iam/application/ports/outbound/**` — Outbound SPI ports (`UserRepositoryPort`, `RefreshTokenRepositoryPort`, `PasswordEncoderPort`, `TokenProviderPort`, `AccountLockoutPort`, `EventPublisherPort`).
  - `src/main/java/com/platform/app/iam/application/services/**` — Application service [`LoginService`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/application/services/LoginService.java) annotated with `@Service` for built-in Spring autowiring.
  - `src/main/java/com/platform/app/iam/application/dto/**` — DTOs and domain events (`AuthTokensDto`, `UserProfileDto`, `UserLoginSuccessEvent`, `UserLoginFailedEvent`).
  - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/**` — Security adapters: [`BCryptPasswordEncoderAdapter`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/BCryptPasswordEncoderAdapter.java) (strength 12), [`JwtTokenProviderAdapter`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/JwtTokenProviderAdapter.java) (JJWT 0.12.6 with HS256/512), [`InMemoryAccountLockoutAdapter`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/InMemoryAccountLockoutAdapter.java) (sliding window), and [`SecurityConfig`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/SecurityConfig.java) (public auth endpoint).
  - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/**` — JPA persistence adapters for User, Role, Permission, and RefreshToken mapping to existing Liquibase tables.
  - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/messaging/**` — [`SpringEventPublisherAdapter`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/messaging/SpringEventPublisherAdapter.java) publishing domain events to Spring's event bus.
  - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/**` — Primary REST controller [`AuthController`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AuthController.java) handling `POST /api/v1/auth/login` and [`RestExceptionHandler`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java) mapping error responses (HTTP 400, 401, 403, 423).
  - `process/context/architecture/architecture-template.md` — Updated application layer structure to standardized Hexagonal Ports & Services (`ports/inbound`, `ports/outbound`, `services`, `dto`).
</main_changes>

---

## 2. Why

<why>
  Implements `UC-IAM-01` to provide core authentication capabilities required across the entire platform. Without login and token generation, downstream subsystems (Document Management, RAG, Conversational AI, HITL) cannot authorize users or resolve tenancy and role-based permissions. Decoupling through Hexagonal ports ensures zero framework leakage into core business rules, while domain event publication enables seamless asynchronous integration with future `UC-AUDIT-01` audit trail consumers without modifying IAM code.
</why>

---

## 3. What Proves It

<evidence>

| AC | Verifier | Result |
|---|---|---|
| AC-1: Hexagonal Domain & Ports | Code inspection & unit tests | PASS |
| AC-2: BCrypt Password Verification | `BCryptPasswordEncoderAdapterTest` | PASS |
| AC-3: JJWT Token Provider with Claims | `JwtTokenProviderAdapterTest` | PASS |
| AC-4: Refresh Token Persistence (30-day) | `PersistenceAdaptersTest` | PASS |
| AC-5: Account Lockout (5 attempts / 15m) | `InMemoryAccountLockoutAdapterTest` & `AuthControllerTest` | PASS |
| AC-6: Domain Event Publishing | `LoginServiceTest` & `AuthControllerTest` | PASS |
| AC-7: REST Endpoint Responses (200, 400, 401, 403, 423) | `AuthControllerTest` | PASS |
| AC-8: Full Suite & Spotless Formatting | `./gradlew check` | PASS |

<!-- To reproduce: -->
```bash
./gradlew test --rerun
./gradlew check
```

</evidence>

---

## 4. What Remains Risky

<residual_risk>
  - Single-node lockout cache: `InMemoryAccountLockoutAdapter` operates in-memory. In a distributed multi-node production deployment, replace this adapter with a Redis-backed implementation of `AccountLockoutPort`.
</residual_risk>

---

## 5. Decisions & Assumptions

<decisions_and_assumptions>
  - Hexagonal Ports & Services: `application.ports.inbound` holds `LoginUseCase` interface and `LoginCommand` input model; `application.services` holds `LoginService` implementing `LoginUseCase`; `application.ports.outbound` holds SPI interfaces.
  - Spring Stereotype `@Service`: `LoginService` is directly annotated with `@Service` to leverage Spring's built-in dependency injection without requiring separate configuration classes.
  - Single Base Configuration: Consolidated into `src/main/resources/application.yaml` using 12-factor environment variable interpolations (`${ENV_VAR:default}`); redundant `application-dev.yaml` removed.
</decisions_and_assumptions>

---

## 6. Next Action

<next_action>
  Proceed to next prioritized use case in backlog (e.g. UC-AUDIT-01 to subscribe to authentication events, or UC-DOC document management).
</next_action>

</handoff>
