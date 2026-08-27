# Backend Service (Spring Boot)

Backend service built with **Spring Boot 4**, **Java 25**, **Gradle**, **PostgreSQL**, **Liquibase**, and **Spring Security (JWT)** for identity, access control, and document operations.

---

## Project Structure

```text
backend/
├── .env.example                               # Local / default environment template
├── .env.dev.example                           # Development environment template
├── .env.prod.example                          # Production environment template
├── .env                                       # Active local environment configuration
├── docker-compose.yaml                        # PostgreSQL container
├── build.gradle                               # Gradle build file & dependencies
├── settings.gradle
└── src/
    ├── main/
    │   ├── java/com/platform/app/
    │   │   ├── AppApplication.java            # Main application entry point
    │   │   ├── package-info.java              # Root architecture description
    │   │   │
    │   │   ├── shared/                        # Shared Kernel & Global Infrastructure
    │   │   │   ├── domain/                    # BaseEntity, AggregateRoot, DomainEvent
    │   │   │   ├── dto/                       # ApiResponse, PageResponse
    │   │   │   ├── exception/                 # ErrorCode, AppException, GlobalExceptionHandler
    │   │   │   ├── config/                    # SecurityConfig, CorsConfig, OpenApiConfig, JacksonConfig
    │   │   │   └── security/                  # JwtTokenProvider, JwtAuthenticationFilter, UserPrincipal
    │   │   │
    │   │   ├── system/                        # System Capability
    │   │   │   └── web/                       # HealthController, HealthCheckResponse
    │   │   │
    │   │   ├── iam/                           # IAM Capability
    │   │   │   ├── domain/                    # User, Role
    │   │   │   ├── repository/                # UserRepository
    │   │   │   ├── service/                   # IamService, CustomUserDetailsService
    │   │   │   └── web/                       # AuthController, Login/Register DTOs
    │   │   │
    │   │   └── [capability]/                  # Business Capabilities (Tactical DDD)
    │   │       ├── domain/                    # Aggregate Roots, Entities, Value Objects, Repo Ports
    │   │       ├── application/               # Application Services, Commands, Queries, Response DTOs
    │   │       ├── infrastructure/            # Outbound Adapters (Spring Data JPA, External Clients)
    │   │       └── api/                       # Inbound REST Adapters (Controllers, Request Payloads)
    │   │
    │   └── resources/
    │       ├── application.yaml               # Application configuration (${ENV_VAR:default})
    │       └── db/changelog/                  # Liquibase database migrations
    │           ├── db.changelog-master.yaml
    │           └── changes/
    │               └── 001-create-users-table.yaml
    └── test/                                  # Unit, Controller, and Integration tests
```

---

## Requirements

- **Java 25** (Java 21+ supported)
- **Docker & Docker Compose**

---

## Environment Configuration

Copy the example environment file for local development:

```bash
cp .env.example .env
```

### Key Environment Variables

| Variable | Default Value | Description |
|---|---|---|
| `SERVER_PORT` | `8080` | Application HTTP server port |
| `SPRING_PROFILES_ACTIVE` | `local` | Active Spring profile (`local`, `dev`, `prod`) |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `support_platform_db` | PostgreSQL database name |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `JWT_SECRET` | *(secret)* | Secret key for signing JWT tokens (min 256 bits) |
| `JWT_EXPIRATION_MS` | `86400000` | Access token expiration in milliseconds (24h) |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` | Refresh token expiration in milliseconds (7d) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173,http://localhost:4200` | Allowed CORS origins (comma-separated) |
| `AI_SERVICE_URL` | `http://localhost:8000` | Base URL for AI microservice |
| `AI_GENERATE_PATH` | `/api/v1/ai/generate` | AI generation endpoint path |
| `LLM_TIMEOUT` | `10s` | AI service HTTP timeout |

---

## Getting Started

### 1. Start PostgreSQL Database
```bash
docker compose up -d
```

### 2. Run Application
```bash
# Linux / macOS
./gradlew bootRun

# Windows PowerShell / CMD
.\gradlew.bat bootRun
```

The application starts at `http://localhost:8080`.

### 3. API Endpoints & Documentation
- **Health Check**: [http://localhost:8080/api/v1/health](http://localhost:8080/api/v1/health)
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## Testing & Building

### Run Tests
```bash
./gradlew test
```

### Build Production Artifact (bootJar)
```bash
./gradlew build
```

Compiled JAR location: `build/libs/backend-0.0.1-SNAPSHOT.jar`

