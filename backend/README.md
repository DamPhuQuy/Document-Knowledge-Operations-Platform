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
├── Dockerfile                                 # Multi-stage Dockerfile (dev & prod targets)
├── Dockerfile.dev                             # Dedicated development Dockerfile (debug port 5005)
├── Dockerfile.prod                            # Dedicated production Dockerfile (JRE slim + layered JAR)
├── docker-compose.yaml                        # Local PostgreSQL container
├── docker-compose.dev.yaml                    # Full stack development compose (hot reload)
├── docker-compose.prod.yaml                   # Production compose stack
├── build.gradle                               # Gradle build file & dependencies
├── settings.gradle
└── src/
    ├── main/
    │   ├── java/com/platform/app/
    │   │   ├── AppApplication.java            # Main application entry point
    │   │   ├── package-info.java              # Root architecture description
    │   │   │
    │   │   ├── iam/                           # Identity & Access Management
    │   │   │   ├── package-info.java
    │   │   │   ├── domain/package-info.java
    │   │   │   ├── repository/package-info.java
    │   │   │   ├── service/package-info.java
    │   │   │   └── web/package-info.java
    │   │   │
    │   │   ├── system/                        # System & Health Capability
    │   │   │   ├── package-info.java
    │   │   │   └── web/package-info.java
    │   │   │
    │   │   └── shared/                        # Shared Kernel & Global Infrastructure
    │   │       ├── package-info.java
    │   │       ├── config/package-info.java
    │   │       ├── config/security/package-info.java
    │   │       ├── domain/package-info.java
    │   │       ├── dto/package-info.java
    │   │       └── exception/package-info.java
    │   │
    │   └── resources/
    │       ├── application.yaml               # Application configuration (${ENV_VAR:default})
    │       ├── application-dev.yaml           # Development profile overrides
    │       ├── application-prod.yaml          # Production profile overrides
    │       └── db/changelog/                  # Liquibase database migrations
    │           ├── db.changelog-master.yaml
    │           └── changes/
    │               ├── 001-initial-extensions.yaml
    │               ├── 002-create-iam-tables.yaml
    │               ├── 003-create-document-tables.yaml
    │               ├── 004-create-rag-tables.yaml
    │               ├── 005-create-conversation-tables.yaml
    │               ├── 006-create-workflow-tables.yaml
    │               ├── 007-create-operations-tables.yaml
    │               └── 008-create-audit-and-notification-tables.yaml
    └── test/                                  # Application tests
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
| `DB_NAME` | `doc_knowledge_db` | PostgreSQL database name |
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

### 1. Local Development (Standard)
```bash
# Start local PostgreSQL
docker compose up -d

# Run application
./gradlew bootRun
```

### 2. Development via Docker (with Hot-Reload & Remote Debugging)
```bash
# Start backend (dev target) + PostgreSQL
docker compose -f docker-compose.dev.yaml up --build
```
- App: `http://localhost:8080`
- JDWP Remote Debug: `localhost:5005`

### 3. Production via Docker (Multi-stage Slim Runner)
```bash
# Build & Run production container
docker compose -f docker-compose.prod.yaml up --build -d
```

---

## API Endpoints & Documentation
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
./gradlew bootJar
```

Compiled JAR location: `build/libs/app-0.0.1-SNAPSHOT.jar`

