# System Architecture & Package Blueprint

> **Source of Truth:** Architectural design, package layout, and design principles for the **Document and Knowledge Operations Platform**.

---

## 1. System High-Level Architecture

```text
                                  +-----------------------------------------+
                                  |         com.platform.app                |
                                  +-----------------------------------------+
                                                       |
         +------------------------+--------------------+--------------------+------------------------+
         |                        |                                         |                        |
         v                        v                                         v                        v
+------------------+    +--------------------+                   +--------------------+    +--------------------+
|      shared      |    |       system       |                   |        iam         |    |      document      |
|  (Shared Kernel  |    | (Health Check &    |                   | (Identity & Access |    |   (Core Domain:    |
|  & Cross-Cutting)|    |  Lean Diagnostics) |                   |  Management)       |    | Tactical DDD Arch) |
+------------------+    +--------------------+                   +--------------------+    +--------------------+
                                                                                                     |
                                                +----------------------------+-----------------------+-----------------------+
                                                |                            |                       |                       |
                                                v                            v                       v                       v
                                       +-----------------+          +-----------------+     +-----------------+     +-----------------+
                                       |     domain      |          |   application   |     | infrastructure  |     |       api       |
                                       | (Aggregate Root,|          | (Use Cases,     |     | (Persistence &  |     | (REST Adapter,  |
                                       |  VOs, Events)   |          |  App Services)  |     |  Outbound Ports)|     |  Web DTOs)      |
                                       +-----------------+          +-----------------+     +-----------------+     +-----------------+
```

---

## 2. Package by Business Capability

Instead of technical horizontal layers across the entire root, top-level packages represent autonomous **Business Capabilities / Bounded Contexts**:

- **`shared`**: Shared Kernel housing cross-cutting foundations (`BaseEntity`, `AggregateRoot`, `DomainEvent`, `ApiResponse`, `PageResponse`, `GlobalExceptionHandler`, `SecurityConfig`, `JwtTokenProvider`).
- **`system`**: Operational health and diagnostic capabilities (`/api/v1/health`).
- **`iam`**: Identity & Access Management (User registration, login, JWT token lifecycle, role-based authorization).
- **`document`**: Core Document Lifecycle management (Upload, S3 object storage pointer, versioning, ACL permissions).
- **`workflow`**: Automated document processing pipelines and Human-In-The-Loop approval state machines.

---

## 3. Selective Tactical DDD

Tactical Domain-Driven Design patterns are applied selectively where domain complexity warrants them:

| Business Capability | Complexity | Architectural Style | Rationale |
| :--- | :--- | :--- | :--- |
| **`system`** | Minimal | Thin REST Controller | Read-only runtime diagnostic checks with zero business rules. |
| **`iam`** | Medium-Low | Pragmatic Service + Repository | Standard authentication and user account management. |
| **`document` / `workflow`** | High (Core Domain) | Full Tactical DDD & Hexagonal Ports/Adapters | Enforces strict invariants, versioning, S3 sync, ACL matrix, and immutable domain events. |

---

## 4. Standard Capability Package Layout (For Complex DDD Domains)

```text
com.platform.app.[capability]/
├── domain/                      # Pure Domain Core (Zero Framework Dependencies)
│   ├── model/                   # Aggregate Roots, Entities, Value Objects
│   ├── event/                   # Immutable Domain Events
│   └── repository/              # Outbound Repository Port Interfaces
├── application/                 # Use Cases & Orchestration Layer
│   ├── service/                 # Application Services (@Transactional)
│   └── dto/                     # Command, Query, and Result DTOs
├── infrastructure/              # Outbound Adapters
│   └── persistence/             # Spring Data JPA Entities & Repository Adapters
└── api/                         # Inbound Driving Adapters
    ├── web/                     # REST Controllers & OpenAPI metadata
    └── dto/                     # Web Request Payloads & Validation (@Valid)
```

### Dependency Inversion Flow:
```text
  [ api ] ───────────────┐
                         ▼
  [ infrastructure ] ──► [ application ] ──► [ domain ] (Pure Core - Zero outer dependencies)
```

---

## 5. Testing Pyramid

- **Domain Unit Tests**: Tests domain invariants, state machines, and calculations in isolation without Spring context.
- **Application Service Tests**: Mocks domain repository ports to verify use case orchestration and transaction boundaries.
- **API / Web Integration Tests**: Uses `MockMvc` to validate HTTP routing, security filters, payload validation, and HTTP status codes.
- **Persistence Integration Tests**: Uses `@DataJpaTest` or PostgreSQL Testcontainers to verify queries, Liquibase migrations, and JPA mappings.
