# Architecture

iam/
shared/
system/
module_DDD/

## Module Architecture

<module_template version="1.0" architecture="clean_hexagonal">

```
module_DDD/
│
├── domain/
│   ├── model/
│   └── exception/
│
├── application/
│   ├── ports/
│   │   ├── inbound/
│   │   └── outbound/
│   │
│   ├── services/
│   └── dto/
│
└── infrastructure/
    │
    ├── adapters/
    │   ├── primary/
    │   │   ├── rest/
    │   │   ├── grpc/ <!-- optional -->
    │   │   ├── cli/ <!-- optional -->
    │   │   └── messaging/ <!-- optional -->
    │   │
    │   └── secondary/
    │       ├── persistence/
    │       │   ├── entity/           <!-- JPA/ORM mapping entities -->
    │       │   ├── repository/       <!-- Spring Data JPA repository interfaces -->
    │       │   └── adapter/          <!-- Secondary adapters implementing Outbound Ports -->
    │       ├── messaging/ <!-- optional -->
    │       └── external_services/ <!-- optional -->
```

</module_template>

## Property of architecture

```
                    ┌──────────────────┐
                    │  Primary Adapter │
                    │ REST / gRPC / MQ │
                    └────────┬─────────┘
                             │
                             ▼
                      ┌──────────────┐
                      │ Inbound Port │
                      └──────┬───────┘
                             │
                             ▼
                  ┌────────────────────────┐
                  │  APPLICATION SERVICES  │
                  │ (Implement Inbound Port│
                  │   & Coordinate Domain) │
                  └───────────┬────────────┘
                              │
                       Outbound Port
                              │
                              ▼
                     ┌──────────────────┐
                     │ Secondary Adapter│
                     │ DB / Kafka / HTTP│
                     └──────────────────┘
```

## Regulations of module architecture

### Dependency Rules

Domain
  → depends on nothing outside the Domain
  → contains pure business rules and domain models
  → must not depend on Infrastructure or frameworks

Application
  → depends on Domain
  → owns inbound ports (`ports/inbound/`: use case interfaces, commands, queries)
  → owns outbound ports (`ports/outbound/`: SPI repository & adapter interfaces)
  → contains application services in `services/` that implement inbound ports
  → contains DTOs in `dto/` for cross-boundary data transfer and events
  → must not depend on Infrastructure or frameworks

Infrastructure
  → depends on Application and Domain
  → implements outbound ports
  → contains framework and technology-specific concerns

### Hexagonal Rules

Primary Adapter
  → invokes an Inbound Port
  → translates external input into Application input

Secondary Adapter
  → implements an Outbound Port
  → translates application operations into external technology operations

### Persistence Organization Rules

Secondary Persistence Adapter (`infrastructure/adapters/secondary/persistence/`)
  → `entity/`: Houses database-specific schema representations (JPA entities with `@Entity`, `@Table`, `@Id`, relationships).
    - Entities are persistence implementation details and must never leak into Domain models or Application DTOs.
    - Equals/hashCode should strictly rely on ID to avoid lazy loading issues and cyclical recursion.
  → `repository/`: Houses Spring Data JPA interfaces extending `JpaRepository` or `CrudRepository`.
    - Declares custom JPQL queries, pagination, or finder methods.
    - Operates purely on JPA entities, kept isolated from domain models.
  → `adapter/`: Houses Spring `@Component` classes that implement application outbound ports (e.g. `UserRepositoryPort`).
    - Injects the relevant Spring Data repositories.
    - Translates between pure Domain models and internal JPA entities.
    - Encapsulates transactional boundaries for persistence operations (`@Transactional`).

### Boundary Rules

  → Outer layers must not bypass Application use cases
  → Domain must not access Infrastructure
  → Application must not access concrete Infrastructure implementations
  → Framework-specific types must not leak into Domain
  → Technology-specific types should not leak into Application
