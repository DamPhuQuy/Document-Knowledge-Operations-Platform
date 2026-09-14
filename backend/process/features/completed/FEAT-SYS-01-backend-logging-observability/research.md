# Research: FEAT-SYS-01 Backend Structured Logging & Observability Infrastructure

<research_context task_id="FEAT-SYS-01" version="2.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. No source modifications. No implementation decisions. -->
<research_status>
  <phase>RESEARCH</phase>
  <mode>READ-ONLY</mode>
  <research_owner>@engineer</research_owner>
  <last_updated>2026-09-14</last_updated>
</research_status>

---

## 1. Current Behavior

<current_behavior>
  - An exhaustive scan across all 58 Java source files under `src/main/java/` revealed that runtime logging is almost completely absent from the backend codebase.
  - Exactly **1 single log statement** exists across the entire project:
    - [`RestExceptionHandler.java:105`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java#L105): `log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);` for catching generic unhandled `Exception.class`.
  - All other layers operate completely silently:
    - **Security Filters:** [`JwtAuthenticationFilter`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/filter/JwtAuthenticationFilter.java) catches token parsing and signature exceptions silently without logging reasons for token rejection or anonymous fallback.
    - **Primary REST Controllers:** [`AuthController`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AuthController.java), [`DepartmentController`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/DepartmentController.java), [`UserRoleController`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/UserRoleController.java), and [`UserDepartmentController`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/UserDepartmentController.java) do not log incoming request paths, HTTP methods, caller identifiers, or completion latencies.
    - **Application Services:** [`LoginService`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/application/services/LoginService.java), [`DepartmentService`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/application/services/DepartmentService.java), and [`AssignRolesService`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/application/services/AssignRolesService.java) do not log execution milestones, domain event dispatches, or business failure reasons.
    - **Secondary Adapters:** [`JwtTokenProviderAdapter`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/adapter/JwtTokenProviderAdapter.java), [`InMemoryAccountLockoutAdapter`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/adapter/InMemoryAccountLockoutAdapter.java), and persistence repository adapters contain no operational logs.
  - **Tracing Context (MDC):** No Mapped Diagnostic Context (MDC) filter exists. Request logs lack a unified `traceId` / `correlationId`, making it impossible to correlate log lines generated across concurrent threads or distributed log aggregators.
  - **Configuration:** `application.yaml` specifies a standard Spring Boot console log pattern and sets `com.platform.app` to `DEBUG`, but without active log statements this configuration produces no useful application telemetry. No structured JSON layout is configured for production.
</current_behavior>

---

## 2. Execution Flow

<execution_flow>

### Flow 1: Current Inbound Request Execution (Unobserved)
```text
HTTP Request (e.g. POST /api/v1/auth/login)
  → Servlet Container (Tomcat)
    → Spring Security FilterChain (JwtAuthenticationFilter)
       [SILENT - No request identification, no traceId, no token inspection log]
    → DispatcherServlet
      → AuthController.login()
         [SILENT - No entry log, no parameter metadata logged]
        → LoginService.authenticate()
           [SILENT - Password check, lockout check, token generation]
           → InMemoryAccountLockoutAdapter.recordFailure() / reset()
              [SILENT - Lockout threshold state unlogged]
           → JwtTokenProviderAdapter.generateAccessToken()
              [SILENT - Token lifecycle unlogged]
        → ResponseEntity.ok(AuthResponse)
           [SILENT - No HTTP response status or duration recorded]
```

### Flow 2: Target Traced & Structured Execution Flow
```text
HTTP Request (Incoming with optional X-Request-ID / X-Trace-ID header)
  → TraceIdFilter (Ordered HIGHEST_PRECEDENCE in FilterChain)
     ├── Generates or extracts correlationId (UUID / hex)
     ├── Puts traceId, clientIp, httpMethod, uri into org.slf4j.MDC
     └── Attaches X-Trace-Id to HttpServletResponse header
  → Security FilterChain
     ├── JwtAuthenticationFilter:
     │     - Valid token → log.debug("Authenticated user {} via JWT [traceId={}]", userId, traceId)
     │     - Expired/Malformed → log.warn("JWT authentication failed: {} [traceId={}]", reason, traceId)
  → DispatcherServlet
     → Rest Controllers:
     │     - Incoming request summary at DEBUG/INFO
  → Application Services:
     ├── LoginService:
     │     - Login success → log.info("Authentication successful for user [{}]", email)
     │     - Bad credentials → log.warn("Authentication failed for user [{}]", email)
     │     - Account locked → log.warn("Authentication blocked for user [{}]: account locked", email)
     ├── DepartmentService:
     │     - Department created/updated → log.info("Department [{}] created with id [{}]", code, id)
     ├── AssignRolesService:
     │     - Roles updated → log.info("Assigned roles {} to user [{}]", roleIds, targetUserId)
  → RestExceptionHandler:
     ├── 4xx Client Exceptions → log.warn("Client error [{}] on {}: {}", status, uri, message)
     └── 5xx Server Exceptions → log.error("Server error on {}: {}", uri, ex.getMessage(), ex)
  → TraceIdFilter (finally block):
     └── MDC.clear() (Guaranteed thread-local memory cleanup)
```

</execution_flow>

---

## 3. Relevant Components

<components>

| Component / File | Layer | Current Logging State | Required Telemetry Role | Confidence |
|---|---|---|---|---|
| `JwtAuthenticationFilter` | Security / Secondary Adapter | Completely silent; swallows exceptions without trace | Log token validation success/failure at DEBUG/WARN | HIGH |
| `AuthController` | Primary REST Adapter | No logger defined | Log auth endpoint invocation, client IP, outcome | HIGH |
| `DepartmentController` | Primary REST Adapter | No logger defined | Log department REST requests & responses | HIGH |
| `UserRoleController` | Primary REST Adapter | No logger defined | Log role assignment requests | HIGH |
| `UserDepartmentController` | Primary REST Adapter | No logger defined | Log user-department assignment requests | HIGH |
| `RestExceptionHandler` | Primary REST Adapter | Single `log.error` for `Exception.class` | Calibrate WARN for 4xx (validation, conflict, not found, unauthorized) and ERROR for 5xx | HIGH |
| `LoginService` | Application Service | No logger defined | Log auth outcomes, lockout enforcement, event publications | HIGH |
| `DepartmentService` | Application Service | No logger defined | Log department creation, modification, code conflict | HIGH |
| `AssignRolesService` | Application Service | No logger defined | Log role mutations, self-revocation rejections | HIGH |
| `JwtTokenProviderAdapter` | Secondary Adapter | No logger defined | Log token issuance & signature validation anomalies | HIGH |
| `InMemoryAccountLockoutAdapter` | Secondary Adapter | No logger defined | Log failed attempt counters & lock trigger events | HIGH |
| `TraceIdFilter` / MDC Provider | Shared / System Infrastructure | **Does not exist** | Intercept requests, initialize MDC, attach response header, guarantee cleanup | HIGH |
| `application.yaml` | Configuration | Basic console pattern, `[com.platform.app]: DEBUG` | Configure uniform pattern including `%X{traceId}` | HIGH |
| `application-prod.yaml` | Configuration | Levels defined (`WARN`/`INFO`), console pattern default | Calibrate production profile logging | HIGH |

</components>

---

## 4. Dependencies & Boundaries

<boundaries>
  <callers>
    - External HTTP clients (Web browsers, mobile clients, external services) calling REST APIs.
    - Internal Spring Application Event listeners (`ApplicationEventPublisher`).
  </callers>

  <callees>
    - SLF4J 2.x abstraction backed by Logback (Spring Boot 4.0.7 native starter).
    - MDC (Mapped Diagnostic Context) thread-local storage.
    - Standard Java logging output (`System.out` / console or file appender).
  </callees>

  <persistence>
    - Logging itself does not directly write to the database. (Database audit logging is handled separately via `audit_logs` / `UC-AUDIT-01`).
    - Secondary persistence repository adapters may log query latency or constraint violations at DEBUG/ERROR.
  </persistence>

  <security_boundary>
    - **CRITICAL OWASP / Regulatory Constraint:** Under NO circumstances should passwords, password hashes, JWT tokens, refresh tokens, or personal identifiers (PII beyond necessary business identifiers like userId/email in operational logs) be logged.
    - Filter order: MDC tracing filter must run before `JwtAuthenticationFilter` and `SecurityFilterChain` to ensure security events are properly tagged with `traceId`.
  </security_boundary>

  <architectural_boundary>
    - Clean Architecture Rule: Pure domain models in `src/main/java/com/platform/app/iam/domain/model/` (`Department`, `User`, `Role`, `Permission`, `RefreshToken`) must remain pure Java POJOs without logging dependencies.
    - Shared logging infrastructure should reside in a neutral, reusable location (e.g. `com.platform.app.shared.infrastructure.logging` or `com.platform.app.system.logging`), accessible to all modules (`iam`, `document`, `knowledge`).
  </architectural_boundary>
</boundaries>

---

## 5. Existing Tests

<existing_tests>

| Test Suite | Current Scope | Logging Impact / Gap |
|---|---|---|
| `AuthControllerTest.java` | WebMvcTest for login endpoint | No assertions for logging; tests pass without log verification |
| `DepartmentControllerTest.java` | WebMvcTest for department CRUD | No assertions for logging |
| `UserRoleControllerTest.java` | WebMvcTest for role assignments | No assertions for logging |
| `UserDepartmentControllerTest.java` | WebMvcTest for user department assignment | No assertions for logging |
| `LoginServiceTest.java` | Unit tests for LoginUseCase | Verifies business logic; no log assertions |
| `DepartmentServiceTest.java` | Unit tests for DepartmentService | Verifies business logic; no log assertions |
| `DepartmentRepositoryAdapterTest.java` | Adapter integration tests | Verifies JPA persistence; no log assertions |
| `AppApplicationTests.java` | Context load test | Context loads successfully |

</existing_tests>

---

## 6. Runtime & Configuration

<runtime_config>
  - **JDK:** Java 25 (`JavaLanguageVersion.of(25)` in `build.gradle`).
  - **Framework:** Spring Boot `4.0.7` (`org.springframework.boot:4.0.7`).
  - **Logging Engine:** SLF4J 2.0.x with Logback (`spring-boot-starter-webmvc` transitively provides `spring-boot-starter-logging`).
  - **Boilerplate Helper:** Project Lombok is active on compiler classpath (`@Slf4j` is readily supported and already utilized in `RestExceptionHandler.java`).
  - **Configuration Profiles:**
    - `application.yaml` (default/dev): Log level `com.platform.app: DEBUG`.
    - `application-prod.yaml`: Log level `com.platform.app: INFO`.
    - `application-staging.yaml`: Log level `com.platform.app: DEBUG`.
</runtime_config>

---

## 7. Source-of-Truth Analysis

<source_of_truth_analysis>

| Source | Specification / Rule | Authority | Alignment & Impact |
|---|---|---|---|
| `process/development-protocols/implementation-standards.md` Section 5 | Rule `structured_logging`: Key-value context (`task_id`, `user_id`, `component`) instead of unstructured concatenation. Rule `no_credential_leakage`: Never log passwords, tokens, API keys, cookies, or PII. | Platform Standard | Enforces MDC usage and strict log content sanitation. |
| `process/context/architecture/architecture-template.md` | Modules organized as `iam/`, `shared/`, `system/`, `module_DDD/`. Dependency rules: Domain depends on nothing; Application depends on Domain; Infrastructure depends on Application & Domain. | Architectural Framework | Tracing filter and shared logging beans belong in `shared/` or `system/`. Domain models must not log directly. |
| `AGENTS.md` Pillar 3 | Action Governance: Anti-escape, no destructive commands, preserve clean diff. | Operational Guardrail | Logging additions must not break existing test contracts or alter business semantics. |

</source_of_truth_analysis>

---

## 8. Evidence Classification

<evidence>
  <confirmed>
    - Slf4j and Logback are present on runtime classpath via Spring Boot Starter.
    - Lombok `@Slf4j` is functional and successfully processed by Gradle annotation processors.
    - Exactly 1 `log.error` call currently exists across `src/main/java/` (in `RestExceptionHandler.java`).
    - `application.yaml` defines console pattern without `%X{traceId}` or MDC token placeholders.
    - No correlation ID filter or request interceptor exists anywhere in the repository.
  </confirmed>

  <observed>
    - Error handling in `RestExceptionHandler` returns HTTP response structures, but 4xx exceptions (validation errors, invalid credentials, locked accounts, conflict exceptions) are not logged at all, leaving operators blind to authentication brute-force attacks or invalid client calls.
    - `JwtAuthenticationFilter` silently ignores invalid tokens, preventing debugging of client authentication issues.
    - Existing test suites execute and pass completely without checking or failing on logging.
  </observed>

  <hypothesized>
    - Introducing an `OncePerRequestFilter` at `Ordered.HIGHEST_PRECEDENCE` will reliably inject `traceId` into MDC for both public and secured endpoints, including Spring Security filter chains.
    - Logging HTTP request boundaries and use case execution at appropriate levels (`DEBUG` for details, `INFO` for state changes, `WARN` for client rejections) will drastically increase observability without degrading throughput.
  </hypothesized>
</evidence>

---

## 9. Assumptions & Uncertainty

<assumptions>
  - **Assumption 1 (Trace ID Header):** Standard header name `X-Trace-Id` (or fallback to incoming `X-Request-Id` if provided by upstream API gateway/reverse proxy) is acceptable for client and log correlation.
  - **Assumption 2 (Package Organization):** Reusable logging components (MDC filter, log constants) should be placed in `com.platform.app.shared.infrastructure.logging` in accordance with the `shared/` architectural archetype.
  - **Assumption 3 (No External Log Aggregator Dependency):** In this initial phase, standard SLF4J / Logback with enhanced console pattern and MDC support meets the immediate requirement without forcing external dependencies (e.g. Logstash logback encoder, Zipkin, Datadog).
</assumptions>

---

## 10. Impacted Files

<impacted_files>
  - **New Infrastructure Files:**
    - `src/main/java/com/platform/app/shared/infrastructure/logging/TraceIdFilter.java` (or MDC correlation filter)
    - `src/main/java/com/platform/app/shared/infrastructure/logging/LoggingConstants.java` (MDC keys: `traceId`, `userId`, `clientIp`)
  - **Primary REST Adapters:**
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AuthController.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/DepartmentController.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/UserRoleController.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/UserDepartmentController.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java`
  - **Security Layer & Secondary Adapters:**
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/filter/JwtAuthenticationFilter.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/adapter/JwtTokenProviderAdapter.java`
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/adapter/InMemoryAccountLockoutAdapter.java`
  - **Application Services:**
    - `src/main/java/com/platform/app/iam/application/services/LoginService.java`
    - `src/main/java/com/platform/app/iam/application/services/DepartmentService.java`
    - `src/main/java/com/platform/app/iam/application/services/AssignRolesService.java`
  - **Configuration Files:**
    - `src/main/resources/application.yaml`
    - `src/main/resources/application-prod.yaml`
    - `src/main/resources/application-staging.yaml`
  - **Test Files:**
    - `src/test/java/com/platform/app/shared/infrastructure/logging/TraceIdFilterTest.java` (new)
    - Existing controller and service test suites updated to assert log behavior where relevant
</impacted_files>

---

## 11. Open Decisions (For INNOVATE Phase)

<open_decisions>
  - **DEC-LOG-01 (MDC Filter vs Interceptor):** Should correlation tracing be implemented via a Servlet `Filter` (runs before Spring Security) or Spring `HandlerInterceptor` (runs only for mapped controllers)?
    - *Preview:* Filter is strictly preferred because it covers Spring Security, authentication failures, and error dispatches.
  - **DEC-LOG-02 (Shared Module Location):** `com.platform.app.shared.infrastructure.logging` vs `com.platform.app.system.logging`.
  - **DEC-LOG-03 (HTTP Request Logging Level & Payload Boundaries):** Should we log HTTP request URI + method + status only, or also request/response bodies? (Request body logging carries severe risk of credential leakage for `/login` and performance overhead for file uploads).
</open_decisions>

---

## 12. Research Exit Criteria (Gate G0)

<research_exit_criteria>
  - [x] Current logging state thoroughly audited and documented across all 58 codebase files.
  - [x] Request execution flows traced from HTTP entry to service dispatch and exception handling.
  - [x] Relevant components and required telemetry roles identified.
  - [x] Architectural and security boundaries (Clean Architecture, zero credential leakage) mapped out.
  - [x] Evidence classified as Confirmed, Observed, or Hypothesized.
  - [x] Open decisions documented for immediate evaluation in INNOVATE phase (`decision.md`).
  - [x] No unresolved research blocker.
</research_exit_criteria>

</research_context>
