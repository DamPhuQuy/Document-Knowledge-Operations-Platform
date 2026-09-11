# Handoff: CHG-IAM-01 Lombok Refactoring for IAM Subsystem

<handoff task_id="CHG-IAM-01" version="2.0" framework="RIPER-5">

<handoff_status>
  <review_decision>PASS</review_decision>
  <review_artifact>process/general-plans/completed/CHG-IAM-01-lombok-refactoring/review.md</review_artifact>
  <completed_date>2026-09-11</completed_date>
</handoff_status>

---

## 1. What Changed

<what_changed>
  Refactored the IAM module (`src/main/java/com/platform/app/iam`) using Project Lombok to eliminate 562 lines of boilerplate (net -451 lines across 14 files) while preserving domain purity, encapsulation, JPA runtime safety, and 100% test compatibility.
</what_changed>

<main_changes>
  - `infrastructure/adapters/secondary/persistence/UserJpaEntity.java` — Replaced manual getters, setters, constructors, equals/hashCode with `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, and `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` on ID.
  - `infrastructure/adapters/secondary/persistence/RoleJpaEntity.java` — Replaced manual getters, setters, constructors, equals/hashCode with safe Lombok annotations on ID.
  - `infrastructure/adapters/secondary/persistence/PermissionJpaEntity.java` — Replaced manual getters, setters, constructors, equals/hashCode with safe Lombok annotations on ID.
  - `infrastructure/adapters/secondary/persistence/RefreshTokenJpaEntity.java` — Replaced manual getters, setters, constructors, equals/hashCode with safe Lombok annotations on ID.
  - `application/services/LoginService.java` — Replaced 20-line manual constructor with `@RequiredArgsConstructor`.
  - `infrastructure/adapters/primary/rest/AuthController.java` — Replaced constructor injection with `@RequiredArgsConstructor`.
  - `infrastructure/adapters/secondary/persistence/UserRepositoryAdapter.java` — Replaced constructor injection with `@RequiredArgsConstructor`.
  - `infrastructure/adapters/secondary/persistence/RefreshTokenRepositoryAdapter.java` — Replaced constructor injection with `@RequiredArgsConstructor`.
  - `infrastructure/adapters/secondary/messaging/SpringEventPublisherAdapter.java` — Replaced constructor injection with `@RequiredArgsConstructor`.
  - `infrastructure/adapters/primary/rest/RestExceptionHandler.java` — Replaced manual `LoggerFactory.getLogger(...)` with `@Slf4j`.
  - `domain/model/User.java` — Refactored with selective `@Getter`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`, `@ToString(onlyExplicitlyIncluded = true)` while maintaining custom validation constructor and `Collections.unmodifiableSet(roles)`.
  - `domain/model/Role.java` — Refactored with selective `@Getter`, `@EqualsAndHashCode`, `@ToString` while preserving custom constructor and unmodifiable permissions.
  - `domain/model/RefreshToken.java` — Refactored with `@Getter` and `@EqualsAndHashCode` on ID while preserving business methods (`revoke()`, `isExpired()`, `isValid()`).
  - `domain/model/Permission.java` — Refactored with `@Getter`, `@EqualsAndHashCode`, `@ToString` on code while preserving custom constructor.
</main_changes>

---

## 2. Why

<why>
  The IAM subsystem previously contained repetitive manual boilerplate that added maintenance friction and visual noise. Refactoring with Lombok reduces cognitive load, adheres to standard Spring/JPA idioms, avoids JPA lazy-loading errors (by strictly avoiding `@Data` on entity collections), and preserves domain invariants. Full context documented in [task.md](task.md) and [decision.md](decision.md).
</why>

---

## 3. What Proves It

<evidence>

| AC | Verifier | Result |
|---|---|---|
| AC-1 | `./gradlew test --tests "*PersistenceAdaptersTest*"` | PASS |
| AC-2 | `./gradlew test --tests "*LoginServiceTest*"` | PASS |
| AC-3 | `./gradlew test --tests "*AuthControllerTest*"` | PASS |
| AC-4 | `./gradlew test --tests "*UserTest*" && ./gradlew test --tests "*RefreshTokenTest*"` | PASS |
| AC-5 | `./gradlew check` | PASS |

<!-- To reproduce: -->
```bash
./gradlew spotlessCheck
./gradlew test
./gradlew check
```

</evidence>

---

## 4. What Remains Risky

<residual_risk>
  - None. All refactored code has identical runtime behavior and is verified by 100% automated test coverage and static analysis checks.
</residual_risk>

---

## 5. Decisions & Assumptions

<decisions_and_assumptions>
  - DEC-IAM-01 (Option A): Adopted layer-calibrated safe Lombok annotations. Never use `@Data` on JPA entities with lazy relationships. Retain Java 25 records without Lombok.
  - Rely on Spring IoC fail-fast container resolution for non-null dependency injection without adding redundant `@NonNull` annotations.
</decisions_and_assumptions>

---

## 6. Next Action

<next_action>
  NONE. Task complete.
</next_action>

</handoff>
