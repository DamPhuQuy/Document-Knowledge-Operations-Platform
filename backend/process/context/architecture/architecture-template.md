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
│   ├── use_cases/
│   │   ├── commands/
│   │   ├── queries/
│   │   └── handlers/
│   │
│   ├── ports/
│   │   ├── inbound/
│   │   └── outbound/
│   │
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
                 │      APPLICATION       │
                 │       USE CASES        │
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
  → contains use cases and application orchestration
  → owns inbound and outbound ports
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

### Boundary Rules

  → Outer layers must not bypass Application use cases
  → Domain must not access Infrastructure
  → Application must not access concrete Infrastructure implementations
  → Framework-specific types must not leak into Domain
  → Technology-specific types should not leak into Application
