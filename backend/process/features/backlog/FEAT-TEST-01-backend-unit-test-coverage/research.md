# Research: FEAT-TEST-01 Backend Comprehensive Unit Test Suite

<research_context task_id="FEAT-TEST-01" version="2.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. No source modifications. No implementation decisions. -->
<research_status>
  <phase>RESEARCH</phase>
  <mode>READ-ONLY</mode>
  <research_owner>@engineer</research_owner>
  <last_updated>2026-09-14</last_updated>
</research_status>

---

## 1. Current Test Suite State & Coverage Baseline

<current_behavior>
  - **Total Passing Tests [Confirmed]**: 120 tests across 22 test files run in 14.2 seconds with 0 failures (`./gradlew test --rerun`).
  - **Test Breakdown by Bounded Context [Confirmed]**:
    - `document`:
      - `DocumentTest`: 6 tests (domain aggregate creation, file type validation, S3 sync state, version bump).
      - `DocumentVersionTest`: 4 tests (version creation, null checks, negative checks).
      - `DocumentUploadServiceTest`: 6 tests (upload orchestration, mime type checks, S3 error rollback, size checks).
      - `DocumentControllerTest`: 5 tests (MockMvc multipart upload, validation, HTTP 201, 400, 413, 415).
      - `DocumentRepositoryAdapterTest`: 5 tests (SpringDataDocumentRepository adapter).
      - `S3ObjectStorageAdapterTest`: 6 tests (S3 PutObject streaming, headObject, deleteObject, error handling).
    - `iam`:
      - Domain: `DepartmentTest` (10 tests), `UserTest` (12 tests), `RefreshTokenTest` (8 tests).
      - Application: `LoginServiceTest` (8 tests), `DepartmentServiceTest` (10 tests), `AssignRolesServiceTest` (7 tests).
      - Primary REST: `AuthControllerTest` (8 tests), `DepartmentControllerTest` (9 tests), `UserRoleControllerTest` (6 tests), `UserDepartmentControllerTest` (5 tests).
      - Secondary Persistence: `DepartmentRepositoryAdapterTest` (4 tests), `PersistenceAdaptersTest` (3 tests: UserRepository, RoleRepository, RefreshTokenRepository).
      - Secondary Security: `BCryptPasswordEncoderAdapterTest` (3 tests), `InMemoryAccountLockoutAdapterTest` (5 tests), `JwtTokenProviderAdapterTest` (6 tests).
    - `shared`:
      - `TraceIdFilterTest`: 3 tests.

  - **Identified Critical Test Gaps [Confirmed]**:
    1. `JwtAuthenticationFilter` (0% coverage):
       - Location: `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/filter/JwtAuthenticationFilter.java`
       - Risk: High. This OncePerRequestFilter validates incoming JWT tokens, parses roles and permissions, and populates `SecurityContextHolder`. Zero unit tests currently exist for token extraction, header parsing, valid/invalid/expired token branches, or authority normalization.
    2. `RestExceptionHandler` (0% coverage):
       - Location: `src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java`
       - Risk: High. Central error translator converting 12+ domain exceptions across IAM and Document contexts into standardized RFC-7807/ErrorResponse payloads. No direct unit tests verify status codes or response payloads.
    3. `DocumentVersionRepositoryAdapter` (0% coverage):
       - Location: `src/main/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentVersionRepositoryAdapter.java`
       - Risk: Medium. Implements `DocumentVersionRepositoryPort`. Maps between `DocumentVersion` domain model and `DocumentVersionJpaEntity`. While `DocumentRepositoryAdapter` is tested, `DocumentVersionRepositoryAdapter` was left uncovered.
    4. `Role` & `Permission` Domain Models (0% coverage):
       - Locations: `src/main/java/com/platform/app/iam/domain/model/Role.java`, `Permission.java`
       - Risk: Low-Medium. Fundamental domain entities enforcing invariants (non-null IDs, uppercase codes, immutable permission sets, `getPermissionCodes()` mapping). Currently untested.
    5. `AccessLevel` and `ProcessingStatus` (0% coverage):
       - Location: `src/main/java/com/platform/app/document/domain/model/AccessLevel.java`, `ProcessingStatus.java`
       - Risk: Low. Enumerations defining core business states and ACL visibility.
</current_behavior>

---

## 2. Testability & Dependency Analysis

<testability_analysis>

### 2.1. `JwtAuthenticationFilter`
- **Dependencies**: `JwtTokenProviderAdapter` (can be mocked with Mockito), `HttpServletRequest`, `HttpServletResponse`, `FilterChain`.
- **Target Test Class**: `src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/security/filter/JwtAuthenticationFilterTest.java`.
- **Key Scenarios to Verify**:
  1. `whenNoAuthorizationHeader_thenContinuesChainWithoutAuthentication()`
  2. `whenAuthorizationHeaderNotBearer_thenContinuesChainWithoutAuthentication()`
  3. `whenBearerTokenInvalid_thenContinuesChainWithoutAuthentication()`
  4. `whenBearerTokenValid_thenSetsAuthenticationInSecurityContextHolder()`
  5. `whenClaimsContainRolesWithoutRolePrefix_thenAddsBothPrefixedAndUnprefixedAuthorities()`
  6. `whenClaimsContainRolesWithRolePrefix_thenAddsBothPrefixedAndUnprefixedAuthorities()`
  7. `whenClaimsContainPermissions_thenAddsExactAndLowercaseAuthorities()`

### 2.2. `RestExceptionHandler`
- **Dependencies**: `HttpServletRequest` (can use `MockHttpServletRequest` from `spring-test`).
- **Target Test Class**: `src/test/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandlerTest.java`.
- **Key Scenarios to Verify**:
  1. `InvalidCredentialsException` -> 401 UNAUTHORIZED
  2. `AccountDisabledException` -> 403 FORBIDDEN
  3. `AccountLockedException` -> 423 LOCKED
  4. `DepartmentNotFoundException` -> 404 NOT_FOUND
  5. `DepartmentCodeConflictException` -> 409 CONFLICT
  6. `UnsupportedMediaTypeException` -> 415 UNSUPPORTED_MEDIA_TYPE
  7. `PayloadTooLargeException` -> 413 PAYLOAD_TOO_LARGE
  8. `StorageException` -> 502 BAD_GATEWAY
  9. `DocumentValidationException`, `EmptyRolesException`, `SelfRoleRevocationException`, `InvalidDepartmentCodeException`, `IllegalArgumentException` -> 400 BAD_REQUEST
  10. `AccessDeniedException` -> 403 FORBIDDEN
  11. `Exception` (unhandled generic) -> 500 INTERNAL_SERVER_ERROR

### 2.3. `DocumentVersionRepositoryAdapter`
- **Dependencies**: `SpringDataDocumentVersionRepository` (mocked with Mockito).
- **Target Test Class**: `src/test/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentVersionRepositoryAdapterTest.java`.
- **Key Scenarios to Verify**:
  1. `save()` maps domain object to JPA entity, persists via repo, and maps returned entity back to domain.
  2. `findByDocumentId()` queries repository ordered by version descending and maps list to domain.
  3. `findByDocumentIdAndVersionNumber()` queries repository and returns populated/empty Optional.
  4. Null checks in `toEntity(null)` and `toDomain(null)` return null safely.

### 2.4. `Role` & `Permission` Domain Models
- **Dependencies**: None (pure Java domain).
- **Target Test Classes**:
  - `src/test/java/com/platform/app/iam/domain/model/RoleTest.java`
  - `src/test/java/com/platform/app/iam/domain/model/PermissionTest.java`
- **Key Scenarios to Verify**:
  1. Successful instantiation with builder and constructor.
  2. Invariant verification: throws `NullPointerException` on null id, null code, or null name.
  3. Code normalization: verifies lowercase input code is converted to uppercase.
  4. Immutability: verifies `getPermissions()` returns unmodifiable set and prevents external mutation.
  5. `getPermissionCodes()` extracts set of string codes accurately.
  6. Equality and hash code: verifies equality is based strictly on id / code.

### 2.5. `AccessLevel` Domain Model
- **Target Test Class**: `src/test/java/com/platform/app/document/domain/model/AccessLevelTest.java`.
- **Key Scenarios to Verify**:
  1. Verify enum values `INTERNAL`, `PUBLIC`, `RESTRICTED`.
  2. Verify `valueOf()` behavior.
</testability_analysis>

---

## 3. Evidence Classification

<evidence_classification>
| Finding | Evidence Level | Rationale |
| :--- | :--- | :--- |
| Current test count is 120 tests across 22 test files | **Confirmed** | Verified via `./gradlew test --rerun` and HTML test report inspection |
| `JwtAuthenticationFilter` is untested | **Confirmed** | Grep of `src/test` found no test class matching or referencing `JwtAuthenticationFilter` |
| `RestExceptionHandler` is untested directly | **Confirmed** | Only indirectly hit during controller slice tests, leaving branch paths untested |
| `DocumentVersionRepositoryAdapter` is untested | **Confirmed** | Grep of `src/test` confirmed only `DocumentRepositoryAdapterTest` exists |
| `Role` and `Permission` are untested | **Confirmed** | Grep of `src/test` confirmed only `DepartmentTest`, `UserTest`, and `RefreshTokenTest` exist |
| All target tests can be executed hermetically with zero external dependencies | **Confirmed** | All targets use plain JUnit 5 + Mockito without Spring context loading |
</evidence_classification>

---

## 4. Research Exit Criteria (Gate G0)

<research_exit_criteria>
- [x] All 5 missing test targets identified and verified in `src/main/java`.
- [x] Test execution baseline confirmed (120 tests passing).
- [x] Mocking strategy and test dependencies analyzed (pure JUnit 5 + Mockito).
- [x] Scope whitelist established in `task.md`.
</research_exit_criteria>

</research_context>
