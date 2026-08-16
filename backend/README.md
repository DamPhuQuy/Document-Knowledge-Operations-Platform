# Customer Support Platform - Backend Architecture

Production-ready backend built with **Spring Boot 4 / Java 25**, structured around **"Package by business capability, then apply tactical DDD selectively according to domain complexity"**.

---

## 🏛️ Architecture Philosophy

```
                                    +-----------------------------------------+
                                    |         com.platform.app                |
                                    +-----------------------------------------+
                                                         |
         +------------------------+----------------------+----------------------+------------------------+
         |                        |                                             |                        |
         v                        v                                             v                        v
+------------------+    +--------------------+                       +--------------------+    +------------------+
|      shared      |    |       system       |                       |        iam         |    |      ticket      |
|  (Shared Kernel  |    | (System Diag /     |                       | (Identity & Access |    |   (Core Domain:  |
|  & Cross-Cutting)|    |  Lean Capability)  |                       |  Lean Capability)  |    |   Tactical DDD)  |
+------------------+    +--------------------+                       +--------------------+    +------------------+
                                                                                                         |
                                                                             +---------------------------+---------------------------+
                                                                             |                           |                           |
                                                                             v                           v                           v
                                                                    +-----------------+         +-----------------+         +-----------------+
                                                                    |     domain      |         |   application   |         | infrastructure  |
                                                                    | (Aggregate Root,|         | (Use Cases, DTOs|         | & web adapters) |
                                                                    |  VOs, Events)   |         |  App Service)   |         |                 |
                                                                    +-----------------+         +-----------------+         +-----------------+
```

### 1. Package by Business Capability (Top-Level)
Instead of horizontal technical layer packages (`controllers`, `services`, `repositories`), the top-level packages represent **Bounded Contexts / Business Capabilities**:
- **`ticket`**: Core Support Ticket Management & Conversation Lifecycle (Complex Domain).
- **`iam`**: Identity & Access Management, registration, authentication, JWT tokens.
- **`system`**: System diagnostics, uptime, and health monitoring.
- **`shared`**: Shared Kernel (base entities, standard response wrappers, exception handling, cross-cutting configurations).

### 2. Selective Tactical DDD (Domain Complexity Driven)
- **Lean / Pragmatic capabilities (`iam`, `system`)**: Simple CRUD and credential workflows without unnecessary DDD overhead (clean Service + Repository + DTOs + Controller).
- **Complex core domain (`ticket`)**: Full **Tactical DDD** implementation:
  - **Aggregate Root (`Ticket`)**: Protects business invariants, state machine transitions (`OPEN` -> `ASSIGNED` -> `IN_PROGRESS` -> `RESOLVED` / `CLOSED`), and encapsulates its child entities (`TicketMessage`).
  - **Value Objects (`TicketStatus`, `TicketPriority`, `TicketCategory`)**: Immutability and state transition validation rules (`canTransitionTo`).
  - **Domain Events (`TicketCreatedEvent`, `TicketStatusChangedEvent`, `TicketAssignedEvent`)**: Emitted on domain state changes via Spring Data's `@DomainEvents`.
  - **Domain Repository Interface (`TicketRepository`)**: Contract defined purely in the domain layer, decoupled from Spring Data JPA.
  - **Application Service (`TicketApplicationService`)**: Orchestrates use cases and transaction boundaries.
  - **Infrastructure Adapters (`SpringDataJpaTicketRepository`, `TicketRepositoryImpl`)**: Persistence implementation.
  - **Web Adapters (`TicketController`)**: Inbound REST endpoints.

---

## 📁 Project Structure

```
backend/
├── .env.example                         # Environment template
├── .env                                 # Local development environment
├── docker-compose.yaml                  # PostgreSQL 16 service
├── build.gradle                         # Gradle build file & dependencies
└── src/
    ├── main/
    │   ├── java/com/platform/app/
    │   │   ├── AppApplication.java
    │   │   │
    │   │   ├── shared/                  # Shared Kernel & Global Infrastructure
    │   │   │   ├── domain/              # BaseEntity, AggregateRoot, DomainEvent
    │   │   │   ├── dto/                 # ApiResponse<T>, PageResponse<T>
    │   │   │   ├── exception/           # ErrorCode, AppException, GlobalExceptionHandler
    │   │   │   ├── config/              # SecurityConfig, CorsConfig, OpenApiConfig, JacksonConfig
    │   │   │   └── security/            # JwtTokenProvider, JwtAuthenticationFilter, UserPrincipal
    │   │   │
    │   │   ├── system/                  # Business Capability: System Diagnostics (Lean)
    │   │   │   └── web/                 # HealthController, HealthCheckResponse
    │   │   │
    │   │   ├── iam/                     # Business Capability: Identity & Access Management (Lean)
    │   │   │   ├── domain/              # User, Role (ADMIN, AGENT, CUSTOMER)
    │   │   │   ├── repository/          # UserRepository
    │   │   │   ├── service/             # IamService, CustomUserDetailsService
    │   │   │   └── web/                 # AuthController, Login/Register DTOs
    │   │   │
    │   │   └── ticket/                  # Business Capability: Support Ticket Management (Tactical DDD)
    │   │       ├── domain/              # Pure Domain Layer
    │   │       │   ├── model/           # Ticket (Aggregate Root), TicketMessage, Status/Priority VOs
    │   │       │   ├── event/           # TicketCreatedEvent, TicketStatusChangedEvent, TicketAssignedEvent
    │   │       │   └── repository/      # TicketRepository (Domain Interface)
    │   │       ├── application/         # Application / Use Cases Layer
    │   │       │   ├── dto/             # CreateTicketCommand, TicketResponse, TicketMessageResponse
    │   │       │   └── service/         # TicketApplicationService
    │   │       ├── infrastructure/      # Outbound Adapters Layer
    │   │       │   └── persistence/     # SpringDataJpaTicketRepository, TicketRepositoryImpl
    │   │       └── web/                 # Inbound Web Adapter Layer
    │   │           └── TicketController.java
    │   │
    │   └── resources/
    │       ├── application.yaml         # Configuration with ${ENV_VAR:default}
    │       └── db/changelog/            # Liquibase migrations for users, tickets, ticket_messages
    └── test/                            # Unit tests, Domain tests & Integration tests
```

---

## 🚀 Quick Start

### 1. Requirements
- **Java 21+** (Java 25 recommended)
- **Docker & Docker Compose**

### 2. Setup Environment
```bash
cp .env.example .env
```

### 3. Start PostgreSQL Database
```bash
docker compose up -d
```

### 4. Run Application
```powershell
.\gradlew.bat bootRun
```

Application URL: **http://localhost:8080**

---

## 📚 API Documentation & Endpoints

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### API Endpoints Summary

| Capability | Method | Endpoint | Description | Auth Required |
|---|---|---|---|---|
| **System** | `GET` | `/api/v1/health` | Service uptime & status | No |
| **IAM** | `POST` | `/api/v1/auth/register` | Register new user account | No |
| **IAM** | `POST` | `/api/v1/auth/login` | Login and obtain JWT tokens | No |
| **IAM** | `GET` | `/api/v1/auth/me` | Current user profile | Bearer Token |
| **IAM** | `POST` | `/api/v1/auth/refresh` | Refresh JWT access token | `X-Refresh-Token` |
| **Ticket (DDD)** | `POST` | `/api/v1/tickets` | Create support ticket | Bearer Token |
| **Ticket (DDD)** | `GET` | `/api/v1/tickets` | List tickets (paginated & filtered) | Bearer Token |
| **Ticket (DDD)** | `GET` | `/api/v1/tickets/{id}` | Get ticket detail & message thread | Bearer Token |
| **Ticket (DDD)** | `PATCH` | `/api/v1/tickets/{id}/assign` | Assign agent to ticket | Agent / Admin |
| **Ticket (DDD)** | `PATCH` | `/api/v1/tickets/{id}/status` | Transition ticket state | Bearer Token |
| **Ticket (DDD)** | `POST` | `/api/v1/tickets/{id}/messages`| Add message or internal note | Bearer Token |

---

## 🧪 Run Tests

Execute unit, domain, and integration tests:
```powershell
.\gradlew.bat test
```
