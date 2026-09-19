# Task Lite: AUTH-003 Add User Register Endpoint

<task_lite version="3.0" framework="RIPER-5-Lite">

<!-- ════════════════════════════════════════════
     SECTION 0 — TASK CONTROL
     ════════════════════════════════════════════ -->
<task_control>
  <track>LITE</track>
  <status>COMPLETED</status>
  <priority>P2</priority>
  <working_mode>DELEGATED</working_mode>
  <current_phase>REVIEW</current_phase>
  <owner>@engineer</owner>
</task_control>

---

## 1. Intent & Specification

<specification>
  <goal>
    Add a POST /api/v1/auth/register endpoint to AuthController with complete Clean Architecture flow:
    RegisterRequest, RegisterResponse, RegisterCommand, RegisterUseCase, RegisterService, EmailAlreadyExistsException,
    SecurityConfig authorization update, and comprehensive integration tests.
  </goal>

  <invariants>
    - Passwords must be hashed using BCrypt (PasswordEncoder).
    - Duplicate email registrations must be rejected with HTTP 409 Conflict.
    - SecurityConfig must permit anonymous access to /api/v1/auth/register.
    - Zero regression on existing auth endpoints (login, account lockout, roles).
  </invariants>

  <acceptance_criteria>
    - [x] AC-1: Fix syntax error in RegisterRequest and add validation annotations.
    - [x] AC-2: Implement RegisterResponse, RegisterCommand, RegisterUseCase, and RegisterService.
    - [x] AC-3: Add EmailAlreadyExistsException mapped to HTTP 409 in RestExceptionHandler.
    - [x] AC-4: Update SecurityConfig to permit /api/v1/auth/** or /api/v1/auth/register.
    - [x] AC-5: Expose POST /api/v1/auth/register in AuthController returning 201 Created.
    - [x] AC-6: Add integration tests in AuthControllerTest for registration happy path, 409 duplicate email, and 400 validation failures.
    - [x] AC-7: All tests pass with `./gradlew test` and `./gradlew check`.
  </acceptance_criteria>

  <definition_of_ready>
    - [x] Intent and acceptance criteria are clear without assumptions.
    - [x] Allowed files in Section 2 are identified.
  </definition_of_ready>
</specification>

---

## 2. Scope Contract & File Whitelist

<scope_contract>
  <allowed_files>
    <file>backend/src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AuthController.java</file>
    <file>backend/src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/dto/request/RegisterRequest.java</file>
    <file>backend/src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/dto/response/RegisterResponse.java</file>
    <file>backend/src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java</file>
    <file>backend/src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/config/SecurityConfig.java</file>
    <file>backend/src/main/java/com/platform/app/iam/domain/exception/EmailAlreadyExistsException.java</file>
    <file>backend/src/main/java/com/platform/app/iam/application/ports/inbound/RegisterCommand.java</file>
    <file>backend/src/main/java/com/platform/app/iam/application/ports/inbound/RegisterUseCase.java</file>
    <file>backend/src/main/java/com/platform/app/iam/application/services/RegisterService.java</file>
    <file>backend/src/test/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AuthControllerTest.java</file>
  </allowed_files>

  <forbidden_files>
    <file>backend/build.gradle</file>
    <file>backend/src/main/resources/application.yaml</file>
  </forbidden_files>
</scope_contract>

---

## 3. Execution Plan (Compact Slices)

<execution_plan>
  ### Slice 1: Domain & Application Layer (Exception, Command, UseCase, Service)
  - **Action:** Create EmailAlreadyExistsException, RegisterCommand, RegisterUseCase, and RegisterService.
  - **Verifier:** ./gradlew compileJava
  - **Status:** [x] DONE

  ### Slice 2: Primary Adapters & Security Config (DTOs, AuthController, RestExceptionHandler, SecurityConfig)
  - **Action:** Fix RegisterRequest, hydrate RegisterResponse, wire RegisterUseCase in AuthController, handle 409 in RestExceptionHandler, permit endpoint in SecurityConfig.
  - **Verifier:** ./gradlew compileJava
  - **Status:** [x] DONE

  ### Slice 3: Verification & Integration Tests
  - **Action:** Add registration tests in AuthControllerTest and verify entire test suite passes.
  - **Verifier:** ./gradlew test && ./gradlew check
  - **Status:** [x] DONE
</execution_plan>

---

## 4. Consolidated Verification & Gates

<verification_gates>
  - [x] **Gate G1/G2 (Plan Approved):** Allowed files confirmed, test verifiers defined. [AUTO: DELEGATED]

  - [x] **Gate G3 (Ready for Handoff):**
    - [x] Verifier commands executed cleanly (Zero errors: ./gradlew test, ./gradlew check).
    - [x] Git diff inspected — NO files touched outside `<allowed_files>`.
    - [x] No temporary debug logs or unintended changes.
</verification_gates>

</task_lite>
