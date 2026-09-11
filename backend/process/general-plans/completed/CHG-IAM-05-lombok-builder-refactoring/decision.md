# Decision: DEC-CHG-IAM-05 Adopt Lombok @Builder Pattern for Object Creation

<technical_decision task_id="CHG-IAM-05" dec_id="DEC-CHG-IAM-05" version="1.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. Present options. PAIR: engineer decides. DELEGATED: agent auto-selects optimal recommendation and advances. -->
<decision_status>
  <phase>INNOVATE</phase>
  <mode>READ-ONLY</mode>
  <decision_owner>@engineer</decision_owner>
  <last_updated>2026-09-12</last_updated>
</decision_status>

---

## 1. Context

<context>
  <task>CHG-IAM-05 Adopt Lombok @Builder Pattern for Object Creation</task>
  <research_artifact>process/general-plans/active/CHG-IAM-05-lombok-builder-refactoring/research.md</research_artifact>
  <constraints>
    - JPA entities with collection fields (`roles`, `permissions`) must use `@Builder.Default` to prevent builders from clearing defaults to `null`.
    - Domain aggregate encapsulation must remain intact (no public setters on domain models, collections unmodifiable).
    - Modern Java records (`UserProfileDto`, `LoginCommand`, `ErrorResponse`, etc.) must maintain immutability and record semantics.
    - Zero compilation errors or test regressions across all unit/integration tests.
  </constraints>
</context>

---

## 2. Decision Required

<decision_question>
  How should the codebase transition object creation from positional `new` constructors to Lombok `@Builder` while preserving JPA safety, domain encapsulation, and backward-compatible test fixtures?
</decision_question>

---

## 3. Options

<options>

  <option id="A">
    <approach>Comprehensive @Builder adoption across domain models, JPA entities, and multi-parameter records/DTOs/events, with backwards-compatible delegate accessors on User and full test suite migration.</approach>
    <advantages>
      - Consistent, expressive, and fluent object creation across domain, persistence, application, and test layers.
      - Eliminates fragile multi-argument `new` calls sensitive to parameter order and evolution.
      - Preserves JPA entity safety by applying `@Builder.Default` to pre-initialized collection fields (`roles`, `permissions`).
      - Restores all broken tests by updating fixtures to use fluent builders.
      - Adds backward-compatible convenience methods on `User` (`isEnabled()`, `isInternal()`, `getCreatedAt()`, `getUpdatedAt()`) delegating to `flags` and `auditMetadata`.
    </advantages>
    <disadvantages>
      - Requires updating multiple test fixture instantiations and adapter mappings.
    </disadvantages>
    <complexity>LOW</complexity>
    <compatibility>HIGH (full backward compatibility for domain callers)</compatibility>
    <concurrency_transaction_risk>NONE (purely object instantiation syntax)</concurrency_transaction_risk>
    <testability>HIGH (all tests verified via `./gradlew test`)</testability>
    <maintainability>EXCELLENT</maintainability>
  </option>

  <option id="B">
    <approach>Partial @Builder adoption limited strictly to JPA entities and domain models; exclude Java records and events.</approach>
    <advantages>
      - Fewer files modified.
    </advantages>
    <disadvantages>
      - Inconsistent codebase style: 7-parameter records (`UserProfileDto`, `ErrorResponse`) still suffer from fragile positional `new` calls.
      - Does not fully fulfill user request to modernize object creation style across the codebase.
    </disadvantages>
    <complexity>LOW</complexity>
    <compatibility>MEDIUM</compatibility>
    <concurrency_transaction_risk>NONE</concurrency_transaction_risk>
    <testability>MEDIUM</testability>
    <maintainability>MEDIUM</maintainability>
  </option>

  <option id="C">
    <approach>Avoid Lombok @Builder and manually write custom fluent builder or factory classes for each model.</approach>
    <advantages>
      - Zero Lombok dependency for builder generation.
    </advantages>
    <disadvantages>
      - Adds hundreds of lines of boilerplate code that Lombok is designed to eliminate.
      - High maintenance burden when fields change.
    </disadvantages>
    <complexity>HIGH</complexity>
    <compatibility>HIGH</compatibility>
    <concurrency_transaction_risk>NONE</concurrency_transaction_risk>
    <testability>HIGH</testability>
    <maintainability>POOR</maintainability>
  </option>

</options>

---

## 4. Trade-off Matrix

<tradeoff_matrix>

| Criterion | Option A (Comprehensive @Builder) | Option B (Partial @Builder) | Option C (Manual Builders) |
|---|:---:|:---:|:---:|
| Expressiveness & Uniformity | 5 | 3 | 4 |
| Maintainability | 5 | 3 | 2 |
| Boilerplate Reduction | 5 | 4 | 1 |
| JPA & DDD Safety | 5 | 5 | 5 |
| Implementation Effort | 4 | 4 | 2 |

<!-- Score: 1 (poor) → 5 (excellent) -->
</tradeoff_matrix>

---

## 5. Recommendation

<recommendation>
  Adopt **Option A**. Applying `@Builder` across domain models, JPA entities (with `@Builder.Default`), and multi-parameter records/DTOs/events gives complete stylistic consistency, prevents argument mix-up bugs, provides `@Builder.Default` safety for JPA relationships, and repairs test fixture compilation cleanly.
</recommendation>

---

## 6. Implementation Decision

<engineer_decision>
  <selected_option>Option A</selected_option>
  <rationale>Option A delivers clean, idiomatic builder patterns throughout domain models, JPA entities, and records while safeguarding JPA default collections and aggregate encapsulation. Restores 100% test compilation and execution. [AUTO: DELEGATED]</rationale>
  <rejected_alternatives>
    - Option B: Rejected because leaving multi-field DTOs with long positional argument lists creates inconsistent code style and keeps error-prone instantiations.
    - Option C: Rejected due to significant boilerplate and redundancy given that Lombok is already an established project dependency.
  </rejected_alternatives>
</engineer_decision>

---

## 7. Constraints Created by This Decision

<constraints_created>
  - JPA entities with default initializers MUST use `@Builder.Default` to prevent null-collection bugs.
  - Domain models must retain unmodifiable collection getters and not expose direct public setters.
  - `User` aggregate root must provide helper methods `isEnabled()`, `isInternal()`, `getCreatedAt()`, `getUpdatedAt()` delegating to `UserFlags` and `AuditMetadata`.
</constraints_created>

---

## 8. Evidence Still Required

<evidence_required>
  - Verification that `./gradlew test` passes with 0 failures after all fixtures and adapters are updated.
  - Verification that `./gradlew spotlessCheck` passes.
</evidence_required>

---

## Gate 1 — Decision Approved

<gate id="G1">
  - [x] All options presented and trade-offs analyzed.
  - [x] Selected option recorded above.
  - [x] No blocking business / schema / security decision remains open.
  <approved_by>[AUTO: DELEGATED]</approved_by>
  <approved_date>2026-09-12</approved_date>
</gate>

</technical_decision>
