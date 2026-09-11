# Handoff: CHG-IAM-05 Adopt Lombok @Builder Pattern for Object Creation

<handoff task_id="CHG-IAM-05" version="2.0" framework="RIPER-5">

<!-- Final projection. Short. Do not duplicate research/plan/review artifacts. -->
<!-- Answer: What changed? Why? What proves it? What remains risky? -->

<handoff_status>
  <review_decision>PASS</review_decision>
  <review_artifact>process/general-plans/completed/CHG-IAM-05-lombok-builder-refactoring/review.md</review_artifact>
  <completed_date>2026-09-12</completed_date>
</handoff_status>

---

## 1. What Changed

<what_changed>
  Refactored object creation across the IAM subsystem and shared domain components to utilize Lombok `@Builder`, eliminating brittle positional multi-argument constructors in favor of expressive and type-safe fluent builders.
</what_changed>

<main_changes>
  - `domain/model/User.java`: Added convenience accessors (`isEnabled()`, `isInternal()`, `getCreatedAt()`, `getUpdatedAt()`) delegating to `UserFlags` and `AuditMetadata`. Safe defaults applied for flags and audit metadata.
  - `domain/model/RefreshToken.java`: Added `@Builder` annotation.
  - `persistence/entity/*JpaEntity.java`: Added `@Builder` to `UserJpaEntity`, `RoleJpaEntity`, `PermissionJpaEntity`, and `RefreshTokenJpaEntity`, applying `@Builder.Default` to collection fields (`roles`, `permissions`) to prevent null-collection bugs.
  - `persistence/adapter/RefreshTokenRepositoryAdapter.java`: Updated mapping logic to use `RefreshTokenJpaEntity.builder()` and `RefreshToken.builder()`.
  - `application/dto/*` & `ports/inbound/LoginCommand.java`: Added `@Builder` to `UserProfileDto`, `UserLoginSuccessEvent`, `UserLoginFailedEvent`, `LoginCommand`, `LoginRequest`, `ErrorResponse`, and `JwtProperties`.
  - `application/services/LoginService.java`, `AuthController.java`, `RestExceptionHandler.java`: Refactored to instantiate events, commands, and responses via `.builder().build()`.
  - Test suites: Migrated `UserTest`, `LoginServiceTest`, `AuthControllerTest`, `JwtTokenProviderAdapterTest`, `PersistenceAdaptersTest`, and `RefreshTokenTest` to use fluent builders.
</main_changes>

---

## 2. Why

<why>
  Positional `new ClassName(...)` constructors are error-prone, hard to read with many parameters, and brittle against schema or constructor changes. Adopting Lombok's `@Builder` provides consistent, readable, and maintainable object creation while safeguarding JPA relations and domain invariants.
</why>

---

## 3. What Proves It

<evidence>

| AC | Verifier | Result |
|---|---|---|
| AC-1 | `./gradlew compileJava` | PASS |
| AC-2 | `./gradlew compileJava` | PASS |
| AC-3 | `./gradlew compileJava` | PASS |
| AC-4 | `./gradlew compileTestJava` | PASS |
| AC-5 | `./gradlew test && ./gradlew spotlessCheck` | PASS |

<!-- To reproduce: -->
```bash
./gradlew test
./gradlew spotlessCheck
```

</evidence>

---

## 4. What Remains Risky

<residual_risk>
  None. All tests compile and execute cleanly with 100% pass rate.
</residual_risk>

---

## 5. Decisions & Assumptions

<decisions_and_assumptions>
  - Adopted Option A (DEC-CHG-IAM-05): Comprehensive builder adoption across domain models, JPA entities (with `@Builder.Default`), and multi-property records/DTOs/events.
</decisions_and_assumptions>

---

## 6. Next Action

<next_action>
  NONE. Task complete and archived to `process/general-plans/completed/CHG-IAM-05-lombok-builder-refactoring/`.
</next_action>

</handoff>
