# Decision: DEC-IAM-02 Persistence Secondary Adapter Structure Standardization

<technical_decision task_id="CHG-IAM-02" dec_id="DEC-IAM-02" version="1.0" framework="RIPER-5">

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
  <task>process/general-plans/active/CHG-IAM-02-persistence-organization/task.md</task>
  <research_artifact>process/general-plans/active/CHG-IAM-02-persistence-organization/research.md</research_artifact>
  <constraints>
    - Preserve Clean Architecture / Hexagonal isolation (Domain models and Application use cases must never know about persistence details).
    - Eliminate clutter in `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/persistence`.
    - Provide a repeatable pattern that can be adopted into `process/context/architecture/architecture-template.md` for all DDD modules.
    - Zero breaking changes to application tests and runtime behavior.
  </constraints>
</context>

---

## 2. Decision Required

<decision_question>
  How should the secondary persistence adapter package be structured to clearly separate JPA entities, Spring Data repositories, and outbound port repository adapters while keeping the layout intuitive and maintainable?
</decision_question>

---

## 3. Options

<options>

  <option id="A">
    <approach>
      **Categorical Subpackages (Recommended)**:
      Segregate persistence components by their explicit architectural responsibility:
      ```
      persistence/
      ├── entity/
      │   ├── UserJpaEntity.java
      │   ├── RoleJpaEntity.java
      │   ├── PermissionJpaEntity.java
      │   └── RefreshTokenJpaEntity.java
      ├── repository/
      │   ├── SpringDataUserRepository.java
      │   └── SpringDataRefreshTokenRepository.java
      └── adapter/
          ├── UserRepositoryAdapter.java
          └── RefreshTokenRepositoryAdapter.java
      ```
    </approach>
    <advantages>
      - Immediate clarity: each directory contains exactly one architectural artifact type.
      - Highly scalable: adding new JPA entities or repositories does not clutter adapters.
      - Clean package-level encapsulation: package-private visibility can be used where appropriate.
      - Easy to standardize in `architecture-template.md`.
    </advantages>
    <disadvantages>
      - Slices files for a single aggregate across three subdirectories.
    </disadvantages>
  </option>

  <option id="B">
    <approach>
      **Aggregate-Based Subpackages**:
      Group persistence files by domain aggregate:
      ```
      persistence/
      ├── user/
      │   ├── UserJpaEntity.java
      │   ├── RoleJpaEntity.java
      │   ├── PermissionJpaEntity.java
      │   ├── SpringDataUserRepository.java
      │   └── UserRepositoryAdapter.java
      └── token/
          ├── RefreshTokenJpaEntity.java
          ├── SpringDataRefreshTokenRepository.java
          └── RefreshTokenRepositoryAdapter.java
      ```
    </approach>
    <advantages>
      - High cohesion for everything related to a single aggregate.
    </advantages>
    <disadvantages>
      - Ambiguity for shared lookup entities (e.g. `roles`, `permissions`, `audit_logs`).
      - Still mixes entities, Spring Data interfaces, and adapters within each aggregate subpackage.
    </disadvantages>
  </option>

  <option id="C">
    <approach>
      **Adapter at Root with Internal Subpackages**:
      Keep Outbound Port Adapter beans at `persistence/` root, and isolate internal database mechanics:
      ```
      persistence/
      ├── entity/
      │   ├── UserJpaEntity.java ...
      ├── repository/
      │   ├── SpringDataUserRepository.java ...
      ├── UserRepositoryAdapter.java
      └── RefreshTokenRepositoryAdapter.java
      ```
    </approach>
    <advantages>
      - Emphasizes that `persistence/` is itself the secondary adapter entry point.
    </advantages>
    <disadvantages>
      - Asymmetrical: mixes folders and `.java` files at the `persistence/` root.
    </disadvantages>
  </option>

</options>

---

## 4. Trade-Off Matrix

<trade_off_matrix>

| Criterion | Option A: Categorical Subpackages (`entity/`, `repository/`, `adapter/`) | Option B: Aggregate Subpackages (`user/`, `token/`) | Option C: Adapter at Root (`entity/`, `repository/`) |
|---|---|---|---|
| **Separation of Concerns** | ⭐⭐⭐⭐⭐ High (strict separation of entity vs repo vs adapter) | ⭐⭐⭐ Medium (still mixes all 3 within aggregate) | ⭐⭐⭐⭐ Good (separates internal details from adapter) |
| **Consistency with Architecture Template** | ⭐⭐⭐⭐⭐ Very High (standard repeatable convention) | ⭐⭐⭐ Medium (varies per module's aggregates) | ⭐⭐⭐⭐ Good |
| **Handling Shared Entities** | ⭐⭐⭐⭐⭐ Seamless (all entities in `entity/`) | ⭐⭐ Prone to friction / cross-package coupling | ⭐⭐⭐⭐⭐ Seamless |
| **Cognitive Simplicity** | ⭐⭐⭐⭐⭐ High (developer knows exactly where each type lives) | ⭐⭐⭐ Medium | ⭐⭐⭐⭐ Good |

</trade_off_matrix>

---

## 5. Recommendation

<recommendation>
  Adopt **Option A: Categorical Subpackages** (`persistence/entity/`, `persistence/repository/`, `persistence/adapter/`).
  
  Rationale:
  1. It cleanly addresses the problem statement ("messy because there are many kinds of files: entity, interface repository, adapter").
  2. It establishes a uniform, clean pattern that can be directly recorded in `process/context/architecture/architecture-template.md` as the official guideline for all modules in the platform.
  3. Shared entities (like `RoleJpaEntity` and `PermissionJpaEntity`) naturally reside alongside `UserJpaEntity` in `entity/` without cross-aggregate confusion.
</recommendation>

---

## 6. Engineer Decision & Gate 1 Sign-Off

<engineer_decision>
  <selected_option>Option A</selected_option>
  <rationale>Categorical subpackaging provides the cleanest separation of concerns (entity, repository, adapter), scales cleanly across all modules, and provides an unambiguous blueprint for the architecture context.</rationale>
</engineer_decision>

<gate id="G1" label="Gate 1 — Decision Approved">
  <status>APPROVED</status>  <!-- PENDING | APPROVED -->
  <approved_by>@engineer</approved_by>
  <approved_date>2026-09-11</approved_date>
</gate>

</technical_decision>
