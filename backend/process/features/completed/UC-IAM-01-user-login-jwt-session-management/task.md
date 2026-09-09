# Task: UC-IAM-01 User Login & JWT Session Lifecycle Management

<task_spec version="3.0" framework="RIPER-5">

<!-- ════════════════════════════════════════════
     SECTION 0 — TASK CONTROL (master state record)
     ════════════════════════════════════════════ -->
<task_control>
  <status>COMPLETED</status>
  <spec_level>S1</spec_level>
  <priority>P1</priority>
  <risk>MEDIUM</risk>
  <estimated_story_points>5</estimated_story_points>
  <working_mode>PAIR</working_mode>
  <current_phase>REVIEW</current_phase>
  <owner>@engineer</owner>
  <decision_owner>@engineer</decision_owner>
  <created>2026-09-09</created>
  <last_updated>2026-09-09</last_updated>
</task_control>

---

## 1. Specification (Pillar 1: Task / Spec)

<specification>
  <goal>
    Implement user authentication via email and password, issuing a stateless short-lived JWT Access Token (containing userId, departmentId, roleIds, isInternal, and permissions) and a secure long-lived Refresh Token recorded in `refresh_tokens`, with domain event publishing for audit trail (`LOGIN`, `LOGIN_FAILED`), account lockout protection, and credential verification.
  </goal>

  <current_behavior>
    The database schema contains IAM tables (`users`, `roles`, `permissions`, `user_roles`, `role_permissions`, `refresh_tokens`) via Liquibase changelogs, but there is no application code, domain entities, authentication service, JWT infrastructure, or REST API endpoints implemented.
  </current_behavior>

  <expected_behavior>
    - Users authenticate via `POST /api/v1/auth/login` supplying email and password.
    - Payload format is validated (valid email, non-empty password).
    - Email lookup is case-insensitive; user status (`enabled = TRUE`) is verified.
    - Password verified against BCrypt hash (work factor >= 12).
    - Account lockout enforced (temporarily locked after 5 consecutive failed login attempts within 15 minutes).
    - Roles and permissions resolved for the authenticated user.
    - JWT Access Token generated containing `userId`, `departmentId`, `roleIds`, `isInternal`, and `permissions` claims with configured expiration.
    - Opaque cryptographically secure Refresh Token generated and persisted to `refresh_tokens` table with 30-day lifetime and revocation support.
    - Domain events published on success (`LOGIN`) and failure (`LOGIN_FAILED`) via Spring ApplicationEventPublisher to decouple from future UC-AUDIT-01 implementation.
    - Response returns HTTP 200 OK with tokens and user profile summary.
  </expected_behavior>

  <actor_authorization>
    - System User (primary actor): Submits credentials to public authentication endpoint.
    - Authentication Subsystem (`SYS-01`): Handles verification, token issuance, and session creation.
    - Audit Subsystem (`SYS-04`): Downstream subscriber to domain login events (decoupled).
  </actor_authorization>

  <invariants>
    - B1: Password must be verified using BCrypt with work factor >= 12.
    - B2: Access Token expiration must be short-lived (configurable, default 1 hour in prod, 24 hours in dev).
    - B3: Refresh Token has a 30-day lifetime and supports immediate revocation (`revoked = TRUE`).
    - B4: Account is temporarily locked after 5 consecutive failed login attempts within 15 minutes.
    - Clean Architecture / DDD: Domain models (`User`, `Role`, `Permission`, `RefreshToken`) decoupled from JPA persistence entities.
  </invariants>

  <out_of_scope>
    - Full implementation of UC-AUDIT-01 subsystem (audit_logs persistence, audit query REST API); IAM only publishes domain events for future audit consumption.
    - Multi-factor authentication (MFA / 2FA).
    - OAuth2 / Social / SSO identity providers (Google, GitHub, SAML).
    - Password reset and registration workflows (separate use cases).
  </out_of_scope>

  <acceptance_criteria>
    - [x] AC-1: Domain models and repository ports created for User, Role, Permission, RefreshToken, and LoginSession following Clean Architecture / DDD.
    - [x] AC-2: Password hashing and verification implemented with BCryptPasswordEncoder (strength = 12).
    - [x] AC-3: JWT token provider implemented to sign and parse tokens using JJWT (HMAC-SHA256/512), embedding `userId`, `departmentId`, `roleIds`, `isInternal`, and `permissions` claims.
    - [x] AC-4: Secure opaque Refresh Token generation and persistence in `refresh_tokens` table with 30-day expiry and revocation flag.
    - [x] AC-5: Account lockout service implemented to track failed attempts and lock account for 15 minutes upon 5 consecutive failures.
    - [x] AC-6: Domain events published on login success (`UserLoginSuccessEvent` / `LOGIN`) and failure (`UserLoginFailedEvent` / `LOGIN_FAILED`) capturing user ID (if identified), client IP, user agent, and timestamp.
    - [x] AC-7: REST endpoint `POST /api/v1/auth/login` implemented returning tokens and user profile on success (200), or appropriate errors (400 Bad Request, 401 Unauthorized, 403 Forbidden for deactivated accounts, 423 Locked for locked accounts).
    - [x] AC-8: Unit and integration tests covering successful login, invalid email, wrong password, disabled account, account lockout, and login domain event publishing.
  </acceptance_criteria>
</specification>

---

## 2. Context Boundaries (Pillar 2: Context)

<context_boundaries>
  <target_files>
    - `src/main/java/com/platform/app/iam/domain/model/**` — [Pure DDD models: User, Role, Permission, RefreshToken, value objects]
    - `src/main/java/com/platform/app/iam/domain/exception/**` — [Domain exceptions: AccountDisabledException, etc.]
    - `src/main/java/com/platform/app/iam/application/ports/inbound/**` — [Inbound use case ports & commands: LoginUseCase, LoginCommand]
    - `src/main/java/com/platform/app/iam/application/ports/outbound/**` — [Outbound SPI ports: UserRepositoryPort, RefreshTokenRepositoryPort, PasswordEncoderPort, TokenProviderPort, EventPublisherPort, AccountLockoutPort]
    - `src/main/java/com/platform/app/iam/application/services/**` — [Application services: LoginService]
    - `src/main/java/com/platform/app/iam/application/dto/**` — [Application response DTOs: AuthTokensDto, UserProfileDto]
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/**` — [REST controller, web request/response DTOs, Exception handler]
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence/**` — [JPA entities, Spring Data interfaces, repository adapters]
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/**` — [BCrypt encoder adapter, JJWT token provider adapter, SecurityConfig]
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/messaging/**` — [Spring ApplicationEventPublisher adapter]
    - `src/test/java/com/platform/app/iam/**` — [Unit and integration test suites structured by layer]
  </target_files>

  <context_groups>
    - planning
    - tests
    - protocols
    - architecture
  </context_groups>

  <source_of_truth>
    <requirement>UC-IAM-01 Specification</requirement>
    <architecture>Standard Architecture Template (process/context/architecture/architecture-template.md)</architecture>
    <existing_behavior>src/main/resources/db/changelog/changes/002-create-iam-tables.yaml, 008-create-audit-and-notification-tables.yaml</existing_behavior>
    <tests>src/test/java/com/platform/app/iam/</tests>
  </source_of_truth>
</context_boundaries>

---

## 3. Verification Strategy

<verification_strategy>

| AC / Risk | Evidence required | Verifier |
|---|---|---|
| AC-1 | Clean domain models without JPA annotations; application ports define inbound/outbound boundaries | Code inspection |
| AC-2 | BCryptPasswordEncoder instantiated with strength 12; hashes verified | Unit test |
| AC-3 | JWT contains expected claims (userId, departmentId, roleIds, isInternal, permissions) and expires appropriately | Unit test with token parsing |
| AC-4 | Refresh token persisted with expiry_date = now + 30 days, revoked = false | JPA integration test |
| AC-5 | 5 consecutive failed attempts lock user for 15 minutes; subsequent attempt returns locked status | Unit/Integration test |
| AC-6 | Login domain events verified published via outbound event port | Integration test |
| AC-7 | REST endpoint handles all paths (200, 400, 401, 403, 423) | MockMvc / WebMvc integration test |
| AC-8 | `./gradlew test` and `./gradlew check` pass with spotless compliance | `./gradlew check` |

</verification_strategy>

---

## 4. Decisions

<decisions>
  <approved_decisions>
    <!-- Decisions recorded in decision.md during INNOVATE phase -->
  </approved_decisions>

  <open_decisions>
    <!-- Decisions that still need human input -->
  </open_decisions>
</decisions>

---

## 5. RIPER-5 Execution Plan (Pillar 4: Loop)

<execution_plan>
  <phase name="Research" order="1">
    - [x] Ingest task spec, domain invariants, and out-of-scope boundaries.
    - [x] Read corresponding schema, configurations, and dependencies.
    - [x] Establish execution flow, boundaries, and source-of-truth conflicts.
    - [x] Produce/update `research.md` artifact.
    <gate id="G0" label="Research Complete">
      - [x] Current behavior understood and documented.
      - [x] Execution flow traced.
      - [x] No unresolved research blocker.
    </gate>
  </phase>

  <phase name="Innovate" order="2">
    - [x] Re-evaluate architectural alignment with process/context/architecture/architecture-template.md.
    - [x] Generate 2–3 alternative approaches with trade-off matrix in decision.md.
    - [x] Produce updated `decision.md` artifact.
    <gate id="G1" label="Gate 1 — Decision Approved">
      - [x] Options reviewed and trade-offs analyzed.
      - [x] Selected option recorded in `decision.md`.
      - [x] No blocking business/schema/security decision remains open.
      <approved_by>@engineer</approved_by>
      <approved_date>2026-09-09</approved_date>
    </gate>
  </phase>

  <phase name="Plan" order="3">
    - [x] Decompose into vertical slices with verifiers and rollback points.
    - [x] Populate `plan.md` with scope contract and verification matrix.
    <gate id="G2" label="Gate 2 — Plan Approved">
      - [x] Every slice has a verifier.
      - [x] Allowed/forbidden file scope is defined.
      - [x] Rollback point defined per slice.
      - [x] Stop conditions defined.
      - [x] Plan approved.
      <approved_by>@engineer</approved_by>
      <approved_date>2026-09-09</approved_date>
    </gate>
  </phase>

  <phase name="Execute" order="4">
    - [x] Implement each slice atomically.
    - [x] Run verifier after each slice.
    - [x] Inspect diff after each slice.
    - [x] Update `state.md` after each slice.
  </phase>

  <phase name="Review" order="5">
    - [x] Review full diff, behavior, architecture, data, security, regression.
    - [x] Produce `review.md` with findings and verification matrix.
    <gate id="G3" label="Gate 3 — Review Passed">
      - [x] All AC verified with evidence.
      - [x] Residual risk accepted.
      - [x] Review decision: PASS.
      - [x] Ready for handoff.
      <approved_by>@engineer</approved_by>
      <approved_date>2026-09-09</approved_date>
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
    Never suppress errors with flags.
    If exhausted, log into `<open_decisions>` and halt.
  </retry_budget>
</guardrails>

</task_spec>
