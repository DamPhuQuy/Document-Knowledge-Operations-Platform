# Decision: DEC-IAM-02 Architecture & Implementation Decisions for UC-IAM-02

<technical_decision task_id="UC-IAM-02" dec_id="DEC-IAM-02" version="3.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. Present options. PAIR: engineer decides. DELEGATED: agent auto-selects optimal recommendation and advances. -->
<decision_status>
  <phase>INNOVATE</phase>
  <mode>PAIR</mode>
  <decision_owner>@engineer</decision_owner>
  <last_updated>2026-09-13</last_updated>
</decision_status>

---

## 1. Context & Business Constraints

<context>
  <task>[task.md](task.md)</task>
  <research_artifact>[research.md](research.md)</research_artifact>
  <constraints>
    - Architectural Alignment: Adhere strictly to Clean Hexagonal DDD guidelines (`architecture-template.md`).
    - Business Rule B1: Effective permissions equal the mathematical UNION of all permissions across all assigned roles.
    - Business Rule B2: Every user must maintain at least one active role at all times.
    - Alternative Path 4a: Self-lockout prevention: An administrator cannot revoke their own `ROLE_ADMIN` role (HTTP 400 Bad Request).
    - Alternative Path 6a: Only authorized users possessing `manage:users` permission / `ROLE_ADMIN` can invoke role assignment endpoints (HTTP 403 Forbidden).
    - Outbound Decoupling: In line with previous architectural streamline, standard Spring utilities (`ApplicationEventPublisher`) are injected directly, while genuine ports (`UserRepositoryPort`, `RoleRepositoryPort`) guard persistence boundaries.
  </constraints>
</context>

---

## 2. Key Architecture Decisions Required

1. **Decision 1: Domain Aggregate Invariants vs. Service-Level Validation**
2. **Decision 2: Security & Authorization Architecture (JWT Filter & RBAC)**
3. **Decision 3: Outbound Persistence Design & Role Resolution Strategy**

---

## 3. Options for Decision 1: Domain Aggregate Invariants vs. Service-Level Validation

<decision id="D1" title="Domain Aggregate Invariants vs. Service-Level Validation">

### Option 1.A: Rich Aggregate Root Invariants (Recommended)
- **Design**:
  - `User.java` (Aggregate Root) owns the business method:
    ```java
    public void assignRoles(Set<Role> newRoles, UUID operatorUserId)
    ```
  - The aggregate directly validates:
    1. `newRoles != null && !newRoles.isEmpty()` &rarr; throws `EmptyRolesException` (Rule B2).
    2. If `this.id.equals(operatorUserId)` &rarr; verifies that `newRoles` retains `ROLE_ADMIN` code; otherwise throws `SelfRoleRevocationException` (Alt Path 4a).
  - Domain exceptions extend a domain base exception or runtime exception and map cleanly to HTTP 400 Bad Request in `RestExceptionHandler`.
- **Trade-offs**:
  - **Pros**: Highest DDD integrity; business invariants cannot be bypassed; 100% testable in pure Java unit tests without Spring or database.
  - **Cons**: Requires passing `operatorUserId` to the domain method.

### Option 1.B: Anemic Domain Model with Procedural Service-Level Validation
- **Design**:
  - `User.java` remains a passive data bag with simple getter/setters.
  - `AssignRolesService` performs all checks (null/empty check, self-lockout check).
- **Trade-offs**:
  - **Pros**: Slightly fewer methods on the domain entity.
  - **Cons**: Violates DDD principles; allows invalid aggregate state to exist if created or modified elsewhere; mixes orchestration with core business logic.

</decision>

---

## 4. Options for Decision 2: Security & Authorization Architecture

<decision id="D2" title="Security & Authorization Architecture">

### Option 2.A: Standard Spring Security JWT Filter with `@PreAuthorize` (Recommended)
- **Design**:
  - Introduce `JwtAuthenticationFilter` (extends `OncePerRequestFilter`) placed before `UsernamePasswordAuthenticationFilter`.
  - Parses `Authorization: Bearer <token>`, validates token with `TokenProviderPort`, extracts `userId`, roles (`ROLE_<CODE>`), and permissions (`manage:users`).
  - Populates Spring `SecurityContextHolder` with `UsernamePasswordAuthenticationToken` containing granted authorities.
  - Enable `@EnableMethodSecurity` in `SecurityConfig`.
  - Secure endpoints with `@PreAuthorize("hasAuthority('manage:users') or hasRole('ADMIN')")`.
  - Controller resolves `operatorUserId` directly from the authenticated principal.
- **Trade-offs**:
  - **Pros**: Standard Spring Security idiom; granular declarative authorization; works seamlessly with mock security contexts (`@WithMockUser`) in `@WebMvcTest`.
  - **Cons**: Requires adding filter and authentication token adapter.

### Option 2.B: Manual Header Parsing & Service-Level Verification
- **Design**:
  - Controller manually parses `Authorization` header, calls `TokenProviderPort`, and verifies `manage:users`.
- **Trade-offs**:
  - **Pros**: Avoids registering a filter bean.
  - **Cons**: Brittle, repetitive across future endpoints, bypasses standard Spring Security filters and annotations.

</decision>

---

## 5. Options for Decision 3: Persistence Design & Role Resolution

<decision id="D3" title="Persistence Design & Role Resolution">

### Option 3.A: Dedicated `RoleRepositoryPort` & Transactional User Merge (Recommended)
- **Design**:
  - Create `RoleRepositoryPort` in `application/ports/outbound/` with:
    - `Set<Role> findByIds(Set<UUID> ids);`
    - `Optional<Role> findByCode(String code);`
    - `List<Role> findAll();`
  - Implement `SpringDataRoleRepository` and `RoleRepositoryAdapter`.
  - Update `UserRepositoryPort`:
    - `Optional<User> findById(UUID id);`
    - `User save(User user);`
  - In `UserRepositoryAdapter.save(User user)`:
    - Loads existing `UserJpaEntity`, replaces the `roles` collection with corresponding `RoleJpaEntity` references, and saves.
- **Trade-offs**:
  - **Pros**: Clean segregation of responsibilities (Interface Segregation Principle); strict separation of JPA entities from domain models; robust transactional integrity.
  - **Cons**: Requires creating repository adapter and Spring Data interface for roles.

### Option 3.B: Ad-hoc Direct Queries in User Repository
- **Design**:
  - Consolidate all role fetching into `UserRepositoryAdapter` without a distinct `RoleRepositoryPort`.
- **Trade-offs**:
  - **Pros**: One fewer port interface.
  - **Cons**: Violates Single Responsibility Principle and Interface Segregation; couples role queries to user repository.

---

## 6. Recommendation Matrix

| Decision | Recommended Option | Rationale |
|---|---|---|
| **D1: Domain Design** | **Option 1.A (Rich Aggregate Root)** | Enforces invariants (B2 & 4a) inside `User` aggregate root; 100% unit-testable in isolation. |
| **D2: Security & Authorization** | **Option 2.A (Standard JWT Filter + `@PreAuthorize`)** | Production-ready, declarative RBAC using Spring Security standards; prepares security context for all subsequent use cases. |
| **D3: Persistence Design** | **Option 3.A (Dedicated RolePort & Adapter)** | Clean separation of concerns complying with Clean Hexagonal DDD architecture and SOLID principles. |

---

## 7. Engineer Decision Record

<!-- In DELEGATED mode, auto-certified per user approval -->
<engineer_decision gate="G1">
  <status>SIGNED</status>
  <selected_options>
    <decision_1_selection>Option 1.A (Rich Aggregate Root Invariants)</decision_1_selection>
    <decision_2_selection>Option 2.A (Standard Spring Security JwtAuthenticationFilter + @PreAuthorize)</decision_2_selection>
    <decision_3_selection>Option 3.A (Dedicated RoleRepositoryPort & RoleRepositoryAdapter)</decision_3_selection>
  </selected_options>
  <rationale>
    Approved by human engineer with fast-track delegation. Rich aggregate invariants enforce DDD principles and testability; standard Spring Security JWT filter ensures robust declarative RBAC; dedicated role port maintains clean hexagonal boundaries.
  </rationale>
  <sign_off engineer="@engineer" date="2026-09-13">APPROVED [FAST-TRACK]</sign_off>
</engineer_decision>

</technical_decision>
