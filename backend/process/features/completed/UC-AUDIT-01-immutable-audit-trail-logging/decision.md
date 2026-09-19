# Decision: DEC-AUDIT-01 UC-AUDIT-01 Immutable Audit Trail Logging Architecture

<technical_decision task_id="UC-AUDIT-01" dec_id="DEC-AUDIT-01" version="1.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. Present options. PAIR: engineer decides. DELEGATED: agent auto-selects optimal recommendation and advances. -->
<decision_status>
  <phase>INNOVATE</phase>
  <mode>PAIR</mode>
  <decision_owner>@engineer</decision_owner>
  <last_updated>2026-09-19</last_updated>
</decision_status>

---

## 1. Context

<context>
  <task>`process/features/active/UC-AUDIT-01-immutable-audit-trail-logging/task.md`</task>
  <research_artifact>`process/features/active/UC-AUDIT-01-immutable-audit-trail-logging/research.md`</research_artifact>
  <constraints>
    - Invariant B1: The `audit_logs` table is strictly APPEND-ONLY. No UPDATE or DELETE operations or API endpoints are permitted.
    - Invariant B2: Database roles assigned to application backends must not execute DROP or TRUNCATE on `audit_logs`.
    - Invariant NF1: Audit logging must execute asynchronously with zero latency penalty on main user request threads.
    - Business Transaction Guarantee: Audit trail must only record successful state mutations if the underlying business transaction commits successfully.
    - Clean Architecture: Domain layer in `com.platform.app.audit` remains pure Java, decoupled from JPA, Liquibase, and Spring MVC.
    - RBAC: Querying audit logs (`GET /api/v1/audit-logs`) is restricted to `ROLE_ADMIN`, `ROLE_LEGAL_AUDITOR`, or users possessing `read:audit_logs`.
  </constraints>
</context>

---

## 2. Decision Questions

<decision_questions>
  1. **Event Listening & Transactional Decoupling:** How should domain events be listened to and processed so that audit records are written asynchronously without blocking HTTP callers, while guaranteeing business transactions commit before logging?
  2. **JSONB Details Mapping:** How should variable contextual event details (`details` JSONB column) be modeled and persisted in `AuditLogJpaEntity`?
  3. **Dynamic Log Query & Filtering:** How should `GET /api/v1/audit-logs` support multi-criteria filtering (date range, user ID, action, resource type, status) and pagination?
</decision_questions>

---

## 3. Options Analysis

### Decision 1: Event Listener Mechanism
<options id="D1">
  <option id="D1-A">
    <approach>
      **`@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` combined with `@Async`:**
      - The listener triggers only AFTER the triggering business transaction commits successfully.
      - Annotated with `@Async` to run on Spring's background thread pool, completely offloading I/O from the main web request thread.
      - For failed actions (e.g. `UserLoginFailedEvent`), the event is published and processed since no rollbacked transaction boundary blocks it.
    </approach>
    <advantages>
      - Guarantees audit log reflects actual committed state changes; aborted/rolled back transactions do not leave phantom audit trails.
      - True asynchronous zero-latency overhead on web requests.
    </advantages>
    <disadvantages>
      - Requires careful handling if an event is published outside an active transaction (can specify `fallbackExecution = true`).
    </disadvantages>
    <verdict>SELECTED (User approved: "Sử dụng TransactionalEventListener để đảm bảo business bắt buộc hoàn thành thành công")</verdict>
  </option>

  <option id="D1-B">
    <approach>
      **Standard `@EventListener` + `@Async`:**
      - Triggers immediately when `publishEvent()` is called, regardless of transaction status.
    </approach>
    <advantages>
      - Simpler configuration without Spring transaction synchronization dependency.
    </advantages>
    <disadvantages>
      - If transaction subsequently rolls back due to a constraint violation or exception, an invalid audit log entry would already have been persisted.
    </disadvantages>
    <verdict>REJECTED</verdict>
  </option>
</options>

### Decision 2: JSONB Details Mapping
<options id="D2">
  <option id="D2-A">
    <approach>
      **`@JdbcTypeCode(SqlTypes.JSON)` with `Map<String, Object>` in `AuditLogJpaEntity`:**
      - Uses Hibernate 6/7 native JSON type mapping.
      - Serializes/deserializes Jackson `Map<String, Object>` or POJO cleanly to PostgreSQL `jsonb`.
      - Domain model `AuditLog` stores `Map<String, Object> details`.
    </approach>
    <advantages>
      - Standard in Spring Boot 4 and modern Hibernate.
      - Type-safe, flexible, and preserves schema neutrality.
      - Direct JSON serialization in REST responses.
    </advantages>
    <disadvantages>
      - None for this use case.
    </disadvantages>
    <verdict>SELECTED (User approved)</verdict>
  </option>

  <option id="D2-B">
    <approach>
      **Raw String Column with manual `ObjectMapper.writeValueAsString()`:**
      - Maps entity column as `String details` with columnDefinition `jsonb`.
    </approach>
    <advantages>
      - Avoids Hibernate type dependency.
    </advantages>
    <disadvantages>
      - Requires manual serialization/deserialization in every service/adapter, increasing boilerplate.
    </disadvantages>
    <verdict>REJECTED</verdict>
  </option>
</options>

### Decision 3: Dynamic Log Query & Filtering
<options id="D3">
  <option id="D3-A">
    <approach>
      **Spring Data JPA `Specification<AuditLogJpaEntity>` (Criteria API):**
      - `SpringDataAuditLogRepository` extends `JpaRepository<AuditLogJpaEntity, UUID>` and `JpaSpecificationExecutor<AuditLogJpaEntity>`.
      - An `AuditLogSpecificationBuilder` composes predicates dynamically based on non-null filter fields (`userId`, `action`, `resourceType`, `resourceId`, `status`, `from`, `to`).
      - Returns `Page<AuditLogJpaEntity>` using Spring Data `Pageable`.
    </approach>
    <advantages>
      - Native Spring Data feature with no additional third-party dependencies (QueryDSL/jOOQ).
      - Type-safe, composable, and leverages database indexes (`idx_audit_logs_user_id`, `idx_audit_logs_resource`, `idx_audit_logs_created_at`).
    </advantages>
    <disadvantages>
      - Criteria API syntax can be slightly verbose.
    </disadvantages>
    <verdict>SELECTED (User approved)</verdict>
  </option>

  <option id="D3-B">
    <approach>
      **Custom Dynamic Native SQL / JPQL concatenation:**
      - Builds SQL string with `StringBuilder` based on present query parameters.
    </approach>
    <advantages>
      - Direct SQL control.
    </advantages>
    <disadvantages>
      - Prone to syntax bugs, string interpolation errors, and pagination count query complexities.
    </disadvantages>
    <verdict>REJECTED</verdict>
  </option>
</options>

---

## 4. Implementation Decisions Summary

<implementation_decisions>
  1. **Event Listener:** Implement `AuditEventListener` with `@Async` and `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)` to ensure committed mutations are recorded without latency penalties. For security failure events (e.g. `UserLoginFailedEvent`), `fallbackExecution = true` allows immediate logging even when no active transaction exists.
  2. **Domain Model & Immutability:** `AuditLog.java` is designed with an immutable structure (all fields `final`, no setters, no update or delete domain methods).
  3. **JSONB Mapping:** Use `@JdbcTypeCode(SqlTypes.JSON)` on `Map<String, Object> details` in `AuditLogJpaEntity`.
  4. **Persistence & Queries:** Implement `AuditLogRepositoryPort` and `AuditLogRepositoryAdapter` using `JpaSpecificationExecutor` with dynamic `Specification` predicates for all query parameters.
  5. **REST API & RBAC:** `AuditLogController` exposes `GET /api/v1/audit-logs` secured with `@PreAuthorize("hasAuthority('read:audit_logs') or hasAuthority('READ:AUDIT_LOGS') or hasRole('ADMIN') or hasRole('LEGAL_AUDITOR')")`.
</implementation_decisions>

---

## 5. Gate 1 Checklist

<gate_1_checklist>
  - [x] Context and constraints verified.
  - [x] Options presented with advantages, disadvantages, and trade-offs.
  - [x] Implementation decision explicitly selected and confirmed with engineer.
  - [x] Zero architectural ambiguity remaining for the Plan phase.
  <approved_by>@engineer</approved_by>
  <approved_date>2026-09-19</approved_date>
</gate_1_checklist>

</technical_decision>
