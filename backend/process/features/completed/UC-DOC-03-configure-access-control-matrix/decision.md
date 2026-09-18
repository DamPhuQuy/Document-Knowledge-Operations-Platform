# Decision: DEC-DOC-03 Configure Document Access Control Matrix

<technical_decision task_id="UC-DOC-03" dec_id="DEC-DOC-03" version="1.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. Present options. DELEGATED mode: agent auto-selects optimal recommendation and advances. -->
<decision_status>
  <phase>INNOVATE</phase>
  <mode>READ-ONLY</mode>
  <decision_owner>@engineer</decision_owner>
  <last_updated>2026-09-17</last_updated>
</decision_status>

---

## 1. Context

<context>
  <task>UC-DOC-03-configure-access-control-matrix</task>
  <research_artifact>process/features/active/UC-DOC-03-configure-access-control-matrix/research.md</research_artifact>
  <constraints>
    - Must enforce 4-tier security classification: PUBLIC, INTERNAL, RESTRICTED, CONFIDENTIAL.
    - Must store explicit grants across 3 tables: document_user_access, document_department_access, document_role_access with permission levels: VIEW, EDIT, ADMIN.
    - Must execute atomically in a single ACID transaction.
    - Must publish DocumentAclUpdatedEvent upon successful commit for audit trail logging (UC-AUDIT-01).
    - Must maintain Clean Architecture boundaries and zero test regressions.
  </constraints>
</context>

---

## 2. Decision Required

<decision_question>
  How should the Document ACL domain entities, persistence ports, update synchronization strategy, and REST endpoints be architected for UC-DOC-03?
</decision_question>

---

## 3. Options

<options>

  <option id="A">
    <approach>
      Unified DocumentAclRepositoryPort with Full-Replacement Synchronization and Cohesive REST Endpoints in DocumentController.
      - Enum `AccessLevel` extended with `CONFIDENTIAL`.
      - Enum `PermissionLevel` created (`VIEW`, `EDIT`, `ADMIN`).
      - Value objects/domain models created for `DocumentUserAccess`, `DocumentDepartmentAccess`, `DocumentRoleAccess`.
      - Cohesive `DocumentAclRepositoryPort` providing `findPermissionsByDocumentId(UUID)` and `replacePermissions(UUID, List<DocumentUserAccess>, List<DocumentDepartmentAccess>, List<DocumentRoleAccess>)`.
      - Use case service `DocumentAclService` handles authorization checks (owner/admin/manage:permissions), updates `document.accessLevel`, atomically replaces ACL rows, and publishes `DocumentAclUpdatedEvent`.
      - Endpoints `PUT /api/v1/documents/{id}/permissions` and `GET /api/v1/documents/{id}/permissions` hosted in `DocumentController`.
    </approach>
    <advantages>
      - Complete transaction isolation: wiping previous grants and persisting new grants guarantees zero stale or orphaned ACL rows.
      - Clean Architecture: callers interact with one clear domain port rather than managing three separate repository adapters.
      - Deterministic testing: easy to mock in unit tests and verify in MockMvc tests.
    </advantages>
    <disadvantages>
      - Replaces all grants for the document in one go (which aligns with UI modal save behavior, but doesn't do sub-field PATCH).
    </disadvantages>
    <complexity>LOW</complexity>
    <compatibility>Full backward compatibility; non-breaking addition.</compatibility>
    <concurrency_transaction_risk>Low; transactions execute within Spring `@Transactional` on target document row.</concurrency_transaction_risk>
    <testability>HIGH</testability>
    <maintainability>HIGH</maintainability>
  </option>

  <option id="B">
    <approach>
      Three Separate Ports & Incremental Diff-based Update Strategy.
      - Separate ports: `UserAccessRepositoryPort`, `DepartmentAccessRepositoryPort`, `RoleAccessRepositoryPort`.
      - Incremental diffing: query existing DB records, compute set difference (to-delete, to-insert, to-update), and execute individual SQL statements.
    </approach>
    <advantages>
      - Minimal deletes if grants rarely change.
    </advantages>
    <disadvantages>
      - High complexity: requires 3x database queries and complex in-memory diffing logic.
      - Higher risk of concurrency race conditions and partial failures.
      - Increased boilerplate across ports and adapters.
    </disadvantages>
    <complexity>HIGH</complexity>
    <compatibility>Full backward compatibility.</compatibility>
    <concurrency_transaction_risk>Medium-High; multiple roundtrips and lock windows.</concurrency_transaction_risk>
    <testability>MEDIUM</testability>
    <maintainability>LOW</maintainability>
  </option>

  <option id="C">
    <approach>
      Separate Dedicated Micro-Controller `DocumentAclController` with Option A's Unified Port.
      - Same unified port and service as Option A, but placing endpoints in a separate `DocumentAclController`.
    </approach>
    <advantages>
      - Separates file size of `DocumentController`.
    </advantages>
    <disadvantages>
      - Fragments `/api/v1/documents/**` REST routes across multiple controllers without clear necessity.
    </disadvantages>
    <complexity>MEDIUM</complexity>
    <compatibility>Full backward compatibility.</compatibility>
    <concurrency_transaction_risk>Low</concurrency_transaction_risk>
    <testability>HIGH</testability>
    <maintainability>MEDIUM</maintainability>
  </option>

</options>

---

## 4. Trade-off Matrix

<tradeoff_matrix>

| Criterion | A (Unified + Full Replace) | B (Separate + Diffing) | C (Separate Controller) |
|---|:---:|:---:|:---:|
| Compatibility | 5 | 5 | 5 |
| Simplicity & Cleanliness | 5 | 2 | 4 |
| Transaction Safety & Determinism | 5 | 3 | 5 |
| Testability | 5 | 3 | 5 |
| Maintainability | 5 | 2 | 4 |

</tradeoff_matrix>

---

## 5. Recommendation

<recommendation>
  Option A is recommended. It provides deterministic, ACID-safe full-replacement synchronization for document ACLs matching the UI modal interaction pattern, cleanly encapsulates persistence behind `DocumentAclRepositoryPort`, and maintains a clean RESTful surface in `DocumentController`.
</recommendation>

---

## 6. Implementation Decision

<implementation_decision>
  <selected_option>A</selected_option>
  <decision_driver>Clean Architecture, deterministic atomic synchronization, and robust testability.</decision_driver>
</implementation_decision>

---

## 7. Gate 1 — Decision Approved

<gate id="G1" label="Gate 1 — Decision Approved">
  - [x] Options reviewed and trade-offs analyzed.
  - [x] Selected option recorded in `decision.md`.
  - [x] No blocking decision remains open.
  <approved_by>[AUTO: DELEGATED]</approved_by>
  <approved_date>2026-09-17</approved_date>
</gate>

</technical_decision>
