# Decision: DEC-IAM-01 Lombok Refactoring Strategy for IAM Subsystem

<technical_decision task_id="CHG-IAM-01" dec_id="DEC-IAM-01" version="1.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. Present options. PAIR: engineer decides. DELEGATED: agent auto-selects optimal recommendation and advances. -->
<decision_status>
  <phase>INNOVATE</phase>
  <mode>READ-ONLY</mode>
  <decision_owner>@engineer</decision_owner>
  <last_updated>2026-09-11</last_updated>
</decision_status>

---

## 1. Context

<context>
  <task>process/general-plans/active/CHG-IAM-01-lombok-refactoring/task.md</task>
  <research_artifact>process/general-plans/active/CHG-IAM-01-lombok-refactoring/research.md</research_artifact>
  <constraints>
    - Maintain Clean Architecture & Domain-Driven Design (DDD) domain purity (no JPA or framework leaks into domain).
    - Avoid dangerous Lombok patterns on JPA entities (`@Data`, unrestricted `@ToString`/`@EqualsAndHashCode` traversing lazy collections causing LazyInitializationException or StackOverflowError).
    - Retain domain invariants: explicit encapsulation, unmodifiable collections, validation/normalization rules.
    - Do not regress any existing unit, slice, or integration test.
    - Keep native Java 25 records as-is without redundant Lombok annotations.
  </constraints>
</context>

---

## 2. Decision Required

<decision_question>
  What scope and annotation strategy should be adopted when refactoring the IAM subsystem (`src/main/java/com/platform/app/iam`) with Project Lombok to maximize boilerplate reduction while maintaining DDD purity and JPA runtime safety?
</decision_question>

---

## 3. Options

<options>

  <option id="A">
    <approach>
      Layer-Calibrated Safe Adoption (Recommended):
      - JPA Entities (`UserJpaEntity`, `RoleJpaEntity`, `PermissionJpaEntity`, `RefreshTokenJpaEntity`): Use `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, and `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` on ID field. Exclude lazy relationship collections (`roles`, `permissions`) from equals/hashCode/toString.
      - Spring DI (`LoginService`, `AuthController`, `UserRepositoryAdapter`, `RefreshTokenRepositoryAdapter`, `SpringEventPublisherAdapter`): Use `@RequiredArgsConstructor` on classes with `private final` dependencies.
      - Exception Logging (`RestExceptionHandler`): Use `@Slf4j` to replace static `LoggerFactory.getLogger(...)`.
      - Domain Models (`User`, `Role`, `RefreshToken`, `Permission`): Use selective `@Getter` and `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`. Retain custom domain constructors with normalization/invariants and retain custom unmodifiable collection accessors (`getRoles()`, `getPermissions()`). Do not add `@Setter` to domain models.
      - Records & Value Objects: Keep Java 25 records untouched as they are natively concise and immutable.
    </approach>
    <advantages>
      - Maximum boilerplate reduction (~500 lines removed) without introducing JPA runtime bugs.
      - Preserves DDD domain invariants and encapsulation.
      - 100% backward compatible with existing tests and calling code.
      - Avoids unnecessary churn on modern Java records.
    </advantages>
    <disadvantages>
      - Requires care when applying to domain entities to ensure custom getters (e.g. unmodifiable collections) are not overridden by Lombok.
    </disadvantages>
    <complexity>LOW</complexity>
    <compatibility>100% backward-compatible (all public method signatures preserved)</compatibility>
    <concurrency_transaction_risk>LOW (no mutable state introduced, JPA identity rules preserved)</concurrency_transaction_risk>
    <testability>HIGH (verified via existing test suites with zero changes to test files)</testability>
    <maintainability>EXCELLENT (drastically reduced boilerplate, standard Spring/JPA idiomatic patterns)</maintainability>
  </option>

  <option id="B">
    <approach>
      Aggressive Full-Subsystem Lombok Adoption:
      - Apply `@Data` or `@Builder` broadly across all classes, including JPA entities and domain models.
      - Replace Java records with Lombok `@Value` or `@Builder` classes.
      - Add `@AllArgsConstructor` and `@Builder` to domain aggregates.
    </approach>
    <advantages>
      - Unifies all classes under Lombok annotations.
    </advantages>
    <disadvantages>
      - `@Data` on JPA entities generates `equals` and `hashCode` across all fields including lazy `@ManyToMany` sets, triggering `LazyInitializationException` and `StackOverflowError`.
      - `@Data` introduces setters on domain models, destroying encapsulation and invariant enforcement.
      - Downgrading native Java 25 records to Lombok `@Value` creates unnecessary boilerplate and loses pattern-matching advantages.
    </disadvantages>
    <complexity>HIGH</complexity>
    <compatibility>LOW (risk of breaking equals/hashCode behavior and lazy loading)</compatibility>
    <concurrency_transaction_risk>HIGH (uncontrolled mutation in domain)</concurrency_transaction_risk>
    <testability>MEDIUM</testability>
    <maintainability>POOR (fragile JPA semantics, broken DDD)</maintainability>
  </option>

  <option id="C">
    <approach>
      Infrastructure-Only Conservative Adoption:
      - Refactor ONLY JPA entities (`UserJpaEntity`, `RoleJpaEntity`, `PermissionJpaEntity`, `RefreshTokenJpaEntity`) and `@RequiredArgsConstructor` on Spring beans.
      - Leave all domain models (`domain/model/`) 100% manual without any Lombok annotations.
    </approach>
    <advantages>
      - Zero risk to domain layer purity.
      - Eliminates the bulk of boilerplate (~350 lines in JPA entities).
    </advantages>
    <disadvantages>
      - Misses opportunities to safely eliminate repetitive getter/equals boilerplate in domain entities.
      - Inconsistent developer experience within the module.
    </disadvantages>
    <complexity>LOW</complexity>
    <compatibility>100% backward-compatible</compatibility>
    <concurrency_transaction_risk>LOW</concurrency_transaction_risk>
    <testability>HIGH</testability>
    <maintainability>GOOD</maintainability>
  </option>

</options>

---

## 4. Trade-off Matrix

<tradeoff_matrix>

| Criterion | Option A (Layer-Calibrated Safe) | Option B (Aggressive Full) | Option C (Infrastructure-Only) |
|---|:---:|:---:|:---:|
| Compatibility | 5 | 2 | 5 |
| Boilerplate Reduction | 5 | 5 | 3 |
| JPA Runtime Safety | 5 | 1 | 5 |
| DDD Encapsulation & Purity | 5 | 1 | 5 |
| Complexity | 5 | 2 | 5 |
| Maintainability | 5 | 2 | 4 |

<!-- Score: 1 (poor) → 5 (excellent) -->
</tradeoff_matrix>

---

## 5. Recommendation

<recommendation>
  Adopt **Option A (Layer-Calibrated Safe Adoption)**.
  - It safely eliminates ~500 lines of boilerplate across JPA entities, Spring service constructors, and domain models.
  - It avoids known JPA pitfalls by explicitly scoping equals/hashCode to entity ID and omitting lazy-loaded collections.
  - It preserves encapsulation and domain invariants in domain models (retaining unmodifiable collections and custom constructors).
  - It respects modern Java records without unnecessary modifications.
</recommendation>

---

## 6. Implementation Decision

<!-- PAIR mode: Completed by engineer before Gate 1 passes.
     DELEGATED / Fast-Track mode: Agent automatically populates Recommendation
     into <selected_option>, documents rationale, signs Gate 1 with [AUTO: DELEGATED], and proceeds. -->
<engineer_decision>
  <selected_option>A</selected_option>
  <rationale>Approved by engineer. Option A (Layer-Calibrated Safe Adoption) provides optimal boilerplate reduction across JPA entities, Spring components, logging, and domain entities while safeguarding JPA lazy-loading relationships, DDD purity, and existing tests.</rationale>
  <rejected_alternatives>
    - Option B: High risk of JPA lazy-loading exceptions, StackOverflowError, and loss of domain encapsulation.
    - Option C: Leaves unnecessary boilerplate in domain entities when safe @Getter is completely non-invasive.
  </rejected_alternatives>
</engineer_decision>

---

## 7. Constraints Created by This Decision

<constraints_created>
  - JPA entities MUST use `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` with `@EqualsAndHashCode.Include` on `@Id` field; do not annotate collections with equals/hashCode or toString.
  - Domain models (`User`, `Role`) MUST keep their custom constructors and custom accessors that return `Collections.unmodifiableSet(...)`.
  - Spring components must have `private final` fields so `@RequiredArgsConstructor` generates exact constructor signatures.
  - Existing Java records must not be changed.
</constraints_created>

---

## 8. Evidence Still Required

<evidence_required>
  - Verify that `./gradlew test` passes all tests with identical assertion behavior after refactoring.
  - Verify that `./gradlew spotlessCheck` passes without formatting violations.
</evidence_required>

---

## Gate 1 — Decision Approved

<gate id="G1">
  - [x] All options presented and trade-offs analyzed.
  - [x] Selected option recorded above.
  - [x] No blocking business / schema / security decision remains open.
  <approved_by>@engineer</approved_by>
  <approved_date>2026-09-11</approved_date>
</gate>

</technical_decision>
