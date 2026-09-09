# Research: UC-IAM-01 User Login & JWT Session Lifecycle Management

<research_context task_id="UC-IAM-01" version="2.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. No source modifications. No implementation decisions. -->
<research_status>
  <phase>RESEARCH</phase>
  <mode>READ-ONLY</mode>
  <research_owner>@engineer</research_owner>
  <last_updated>2026-09-09</last_updated>
</research_status>

---

## 1. Current Behavior

<current_behavior>
  - Database migration changeset `002-create-iam-tables.yaml` provisions tables: `departments`, `users`, `roles`, `permissions`, `user_roles`, `role_permissions`, `refresh_tokens`.
  - Database migration changeset `008-create-audit-and-notification-tables.yaml` provisions `audit_logs` table.
  - Dependencies: `jjwt-api:0.12.6`, `jjwt-impl:0.12.6`, `jjwt-jackson:0.12.6`, `spring-boot-starter-security`, `spring-boot-starter-data-jpa`, `postgresql`, `h2` are available in `build.gradle`.
  - `application.yaml` defines default properties for JWT secret (`app.jwt.secret`), access token expiration (`app.jwt.expiration-ms`), and refresh token expiration (`app.jwt.refresh-expiration-ms`).
  - No domain entities, repository ports, Spring Data repositories, security filters, or REST controllers currently exist under `src/main/java/com/platform/app/`.
  - Spring Security defaults are active on the classpath; without custom `SecurityFilterChain` configuration, all endpoints require default HTTP Basic authentication.
</current_behavior>

---

## 2. Execution Flow

<execution_flow>

### Flow 1: Successful Login & Token Issuance
```text
User / Client
  → POST /api/v1/auth/login (email, password)
    → AuthController validates @Valid LoginRequest (email format, non-blank password)
      → LoginUseCase (Application Service)
        → AccountLockoutService checks if account is locked (5 failures within 15 min)
        → UserRepository.findByEmailIgnoreCase(email)
          - If user not found → publish UserLoginFailedEvent, throw BadCredentialsException
        → Check user.isEnabled()
          - If disabled → throw AccountDisabledException (HTTP 403)
        → PasswordEncoder.matches(rawPassword, user.getPasswordHash()) (BCrypt strength 12)
          - If mismatch → increment failed attempts, publish UserLoginFailedEvent, throw BadCredentialsException
        → AccountLockoutService.resetFailedAttempts(email)
        → Resolve user roles and permissions
        → JwtTokenProvider.generateAccessToken(user, roleIds, permissions)
            Claims: userId, departmentId, roleIds, isInternal, permissions
        → SecureTokenGenerator generates cryptographically random refresh token string
        → RefreshTokenRepository.save(new RefreshToken(userId, token, expiryDate=30 days, revoked=false))
        → Publish UserLoginSuccessEvent(userId, email, clientIp, userAgent, timestamp)
            (Note: Future UC-AUDIT-01 will listen to this event and write to audit_logs asynchronously)
      → AuthResponse (accessToken, refreshToken, tokenType="Bearer", expiresIn, userProfile)
```

### Flow 2: Failed Login & Account Lockout
```text
User / Client
  → POST /api/v1/auth/login (email, bad_password)
    → LoginUseCase verifies password -> mismatch!
    → AccountLockoutService.recordFailure(email)
      - If consecutive failures >= 5 within 15 minutes -> mark locked for 15 minutes
    → Publish UserLoginFailedEvent(email, clientIp, userAgent, reason, timestamp)
        (Note: Future UC-AUDIT-01 will listen to this event and write to audit_logs asynchronously)
    → Throw BadCredentialsException (or AccountLockedException if threshold reached)
    → ControllerAdvice maps to HTTP 401 Unauthorized (or HTTP 423 Locked)
```

</execution_flow>

---

## 3. Relevant Components

<components>

| File / Symbol | Role | Evidence | Confidence |
|---|---|---|---|
| `002-create-iam-tables.yaml` | Liquibase DDL for `users`, `roles`, `permissions`, `user_roles`, `role_permissions`, `refresh_tokens` | Confirmed in `src/main/resources/db/changelog/changes/` | HIGH |
| `008-create-audit-and-notification-tables.yaml` | Liquibase DDL for `audit_logs` | Confirmed in `src/main/resources/db/changelog/changes/` | HIGH |
| `build.gradle` | Dependencies: JJWT 0.12.6, Spring Security 4.0.7, Spring Data JPA, H2, PostgreSQL | Confirmed in `backend/build.gradle` | HIGH |
| `application.yaml` | Properties `app.jwt.secret`, `app.jwt.expiration-ms`, `app.jwt.refresh-expiration-ms` | Confirmed in `src/main/resources/application.yaml` | HIGH |
| `User` (Domain Entity) | Core aggregate representing system user, decoupled from JPA | Architectural requirement (Pillar 3) | HIGH |
| `RefreshToken` (Domain Entity) | Aggregate root for refresh token lifecycle (revocation, expiry) | Architectural requirement (Pillar 3) | HIGH |
| `JwtTokenProvider` | Utility to sign/parse JWT tokens using JJWT | Technical requirement | HIGH |
| `AccountLockoutService` | Implements rule B4 (5 failed attempts within 15 min lock) | Business Rule B4 requirement | HIGH |
| `AuditEventListener` | Asynchronously writes audit trail records to `audit_logs` | UC-AUDIT-01 inclusion requirement | HIGH |

</components>

---

## 4. Dependencies & Boundaries

<boundaries>
  <callers>
    - Frontend client / API consumers invoking `POST /api/v1/auth/login`.
  </callers>
  <callees>
    - PostgreSQL database (`users`, `roles`, `permissions`, `user_roles`, `role_permissions`, `refresh_tokens`, `audit_logs`).
  </callees>
  <persistence>
    - Reads: `users`, `roles`, `permissions`, `user_roles`, `role_permissions`.
    - Writes: `refresh_tokens` (INSERT session), `audit_logs` (INSERT audit record).
  </persistence>
  <external_systems>
    - None (stateless JWT issuance).
  </external_systems>
  <transaction_boundary>
    - `LoginUseCase.authenticate()` executes in a read-only transaction for credential lookup, then creates the refresh token session.
    - Audit event insertion runs asynchronously in an independent transaction boundary (`REQUIRES_NEW`), so failure of audit log insertion never rolls back a successful authentication, and a failed authentication still successfully records a `LOGIN_FAILED` audit log.
  </transaction_boundary>
  <security_boundary>
    - `POST /api/v1/auth/login` is public (`permitAll()`).
    - CSRF disabled for stateless REST API.
    - All other application endpoints require valid JWT authentication.
  </security_boundary>
</boundaries>

---

## 5. Existing Tests

<existing_tests>

| Test | Behavior covered | Gap |
|---|---|---|
| `AppApplicationTests.java` | Spring ApplicationContext loading | No IAM unit or integration tests exist yet. |

</existing_tests>

---

## 6. Runtime / Configuration

<runtime_config>
  - Java 25 toolchain, Spring Boot 4.0.7, Spring Security 7.x, Hibernate 7.2.
  - JWT configuration in `application.yaml`:
    - `app.jwt.secret`: 256+ bit secret (configured default provided in `application.yaml`).
    - `app.jwt.expiration-ms`: 86,400,000 ms (dev 24h); prod should support 1h.
    - `app.jwt.refresh-expiration-ms`: 2,592,000,000 ms (30 days per rule B3).
  - Test profile uses H2 in-memory DB with Liquibase disabled and JPA `ddl-auto: create-drop`.
</runtime_config>

---

## 7. Source-of-Truth Analysis

<source_of_truth_analysis>

| Source | Says | Authority | Conflict |
|---|---|---|---|
| `UC-IAM-01` Spec | BCrypt strength $\ge 12$, short-lived JWT with specific claims, 30-day refresh token, 5 failures in 15 min locks account, audit logging via `UC-AUDIT-01` | User Requirement | None |
| `002-create-iam-tables.yaml` | `users` has `id`, `email`, `password_hash`, `full_name`, `department_id`, `enabled`, `is_internal`. `refresh_tokens` has `id`, `user_id`, `token`, `expiry_date`, `revoked`. | Database Schema Contract | No lockout columns exist on `users` table. Lockout state must be tracked via sliding cache or `audit_logs` queries. |
| `AGENTS.md` Pillar 3 | Hexagonal / Clean Architecture & DDD: persistence entities and DB tables must not leak into core domain models. | Architectural Guardrail | Pure domain models in `iam/domain/` decoupled from JPA entities in `iam/infrastructure/persistence/`. |

</source_of_truth_analysis>

---

## 8. Evidence Classification

<evidence>
  <confirmed>
    - Tables `users`, `departments`, `roles`, `permissions`, `user_roles`, `role_permissions`, `refresh_tokens` defined in `002-create-iam-tables.yaml`.
    - Table `audit_logs` defined in `008-create-audit-and-notification-tables.yaml`.
    - JJWT 0.12.6 is present on compile and runtime classpath.
    - Password encryption requires `BCryptPasswordEncoder(12)`.
    - Spring Boot 4.0.7 and Java 25 are active.
  </confirmed>

  <observed>
    - The `users` table schema does not include `failed_attempts` or `locked_until` columns.
    - No existing `SecurityFilterChain` bean is defined in the project, causing Spring Security to apply default secure-all behavior.
  </observed>

  <hypothesized>
    - Account lockout (B4) can be cleanly managed by an in-memory/concurrent sliding-window service (e.g. Caffeine or `ConcurrentHashMap` with time-based eviction) or by querying recent `LOGIN_FAILED` entries from `audit_logs`.
    - Decoupling audit logging via Spring Application Events (`AuditEventPublisher`) allows `UC-IAM-01` to seamlessly trigger `UC-AUDIT-01` without tight coupling between IAM and Audit persistence code.
  </hypothesized>
</evidence>

---

## 9. Assumptions & Uncertainty

<assumptions>
  - Assumption 1: Token claims include: `sub` (user email or userId), `userId`, `departmentId`, `roleIds` (List<UUID>), `isInternal` (boolean), `permissions` (List<String> of permission codes).
  - Assumption 2: Refresh token is an opaque cryptographically secure random string (e.g., Base64-encoded 64-byte secure random).
  - Assumption 3: For account lockout tracking without altering the frozen Liquibase schema, an in-memory sliding-window cache is standard, robust, and avoids DDL schema changes.
</assumptions>

---

## 10. Impacted Files

<impacted_files>
  - `src/main/java/com/platform/app/iam/domain/model/*` — Domain entities (`User`, `Role`, `Permission`, `RefreshToken`, `DepartmentId`, `UserId`, `RoleId`)
  - `src/main/java/com/platform/app/iam/domain/repository/*` — Domain repository ports (`UserRepository`, `RefreshTokenRepository`)
  - `src/main/java/com/platform/app/iam/application/service/*` — `LoginUseCase`, `AccountLockoutService`
  - `src/main/java/com/platform/app/iam/application/dto/*` — `LoginCommand`, `AuthTokensDto`, `UserProfileDto`
  - `src/main/java/com/platform/app/iam/infrastructure/persistence/entity/*` — JPA entities (`UserJpaEntity`, `RoleJpaEntity`, `PermissionJpaEntity`, `RefreshTokenJpaEntity`)
  - `src/main/java/com/platform/app/iam/infrastructure/persistence/repository/*` — Spring Data JPA repository interfaces and adapters
  - `src/main/java/com/platform/app/iam/infrastructure/security/*` — `JwtTokenProvider`, `SecurityConfig`, `PasswordEncoderConfig`
  - `src/main/java/com/platform/app/iam/infrastructure/web/*` — `AuthController`, `LoginRequest`, `AuthResponse`, `GlobalExceptionHandler`
  - `src/main/java/com/platform/app/audit/*` — `AuditEvent`, `AuditLogRepository`, asynchronous `AuditEventListener` for UC-AUDIT-01
  - `src/test/java/com/platform/app/iam/*` — Unit and integration tests for authentication, tokens, lockout, and error scenarios
</impacted_files>

---

## 11. Open Decisions

<open_decisions>
  - Decision 1: Account Lockout Strategy (In-memory time-window cache vs `audit_logs` query vs schema addition).
  - Decision 2: Audit Trail Event Dispatching (Spring Domain Event vs direct Audit Service call).
  - Decision 3: JWT Subject (`sub` claim: user ID vs user email).
</open_decisions>

---

## 12. Research Exit Criteria

<research_exit_criteria>
  - [x] Current behavior understood and documented.
  - [x] Execution flow traced end-to-end.
  - [x] Relevant boundaries identified (callers, callees, persistence, security).
  - [x] Source-of-truth conflicts identified or confirmed absent.
  - [x] Impacted files identified.
  - [x] Evidence classified (Confirmed / Observed / Hypothesized).
  - [x] No unresolved research blocker.
</research_exit_criteria>

</research_context>
