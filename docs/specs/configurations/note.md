# Multi-Environment Configuration

A concise architectural reference summarizing configuration design, rules, and differences across environments (**Local**, **Dev / Staging**, and **Prod**) for the **Document and Knowledge Operations Platform**.

---

## 1. 3-Tier Configuration Architecture

```text
┌────────────────────────────────────────────────────────────────────────┐
│ 1. Runtime & Build Environments (.env / Secret Manager / ConfigMap)    │
│    -> Dynamic injection: DB credentials, JWT secrets, service URLs     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ 2. Application Configuration (Backend Profiles & Frontend Config)      │
│    -> Backend: Spring profiles (HikariCP, JPA, Logging, CORS)          │
│    -> Frontend: Type-safe config (`src/config/env.ts`, Vite modes)     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ 3. Containerization Strategy (Dockerfile & Docker Compose)             │
│    -> Backend: Dev (JDK + JDWP) | Prod (JRE Slim + Layered JAR)        │
│    -> Frontend: Dev (Node 22 + Vite HMR) | Prod (Nginx Alpine + SPA)   │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Multi-Environment Comparison Matrix

### A. Environment Variables (`.env`)

| Variable | Local / Dev (`.env.example`) | Staging (`.env.staging.example`) | Prod (`.env.prod.example`) | Description & Security Principle |
| :--- | :--- | :--- | :--- | :--- |
| **Backend** | | | | |
| `SPRING_PROFILES_ACTIVE` | `dev` | `staging` | `prod` | Activates target Spring Boot profile |
| `DB_HOST` | `postgres` (or `localhost`) | `postgres` (or internal RDS) | `postgres-primary.prod.internal` | PostgreSQL host (Local / Staging / Cloud Managed RDS) |
| `DB_NAME` | `doc_knowledge_dev` | `doc_knowledge_staging` | `doc_knowledge_prod` | Environment database schema segregation |
| `JWT_SECRET` | Dev Secret 256-bit | Staging Secret 256-bit | KMS / Vault 512-bit | **Prod must inject via KMS / K8s Secret**, never plaintext |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | `14400000` (4h) | `3600000` (1h) | Prod uses short TTL + Refresh Token Rotation |
| `CORS_ALLOWED_ORIGINS` | `localhost:3000, 5173` | `https://staging.docknowledge...` | `https://docknowledge.company.com` | Strict domain whitelisting on production |
| `AI_SERVICE_URL` | `http://localhost:8000` | `http://ai-service-staging:8000` | `http://ai-service.prod.internal` | Internal VPC endpoint for AI microservice |
| **Frontend** | | | | |
| `VITE_API_BASE_URL` | `http://localhost:8080/api/v1` | `https://staging-api.docknowledge...` | `https://api.docknowledge.company.com/api/v1` | Backend API gateway endpoint |
| `VITE_APP_TITLE` | `Document & Knowledge... (Dev)` | `Document & Knowledge... (Staging)` | `Document & Knowledge Platform` | Application title display |
| `VITE_APP_ENV` | `development` | `staging` | `production` | Active runtime tier badge |
| `VITE_ENABLE_DEBUG` | `true` | `true` | `false` | Client-side console debug logging |

---

### B. Spring Boot Profiles (`application.yaml`)

| Setting | Local / Dev (`application-dev.yaml`) | Production (`application-prod.yaml`) | Rationale |
| :--- | :--- | :--- | :--- |
| **Hikari Connection Pool** | `min-idle: 5`<br>`max-size: 20` | `min-idle: 10`<br>`max-size: 50`+ | Dev saves DB resources; Prod pre-warms pool for high concurrency and lower latency. |
| **JPA / Hibernate SQL** | `show-sql: true`<br>`format_sql: true` | `show-sql: false`<br>`format_sql: false` | Formatting/printing SQL in Prod causes severe I/O bottlenecks and data leakage. |
| **DDL Validation** | `validate` | `validate` / `none` | Never use `update` or `create`. Schema is exclusively managed via Liquibase. |
| **Swagger / OpenAPI UI** | `enabled: true` | `enabled: false` | Disabled in Prod to minimize public attack surface and avoid exposing internal schemas. |
| **Logging Level** | `com.platform.app: DEBUG`<br>`org.hibernate.SQL: INFO` | `root: WARN`<br>`com.platform.app: INFO`<br>`SQL: ERROR` | Reduces log volume/costs (Datadog/CloudWatch) and boosts throughput. |

---

### C. Dockerfile & Container Strategy

#### 1. Backend Service

| Feature | Development ([`backend/Dockerfile.dev`](../../../backend/Dockerfile.dev)) | Production ([`backend/Dockerfile.prod`](../../../backend/Dockerfile.prod)) |
| :--- | :--- | :--- |
| **Base Image** | `eclipse-temurin:25-jdk-alpine` (Full JDK) | `eclipse-temurin:25-jre-alpine` (Minimal JRE Slim) |
| **Build Strategy** | Executes source code directly via Gradle Wrapper | **Multi-stage Build** + **Layered JAR** (`jarmode=tools extract`) |
| **Docker Cache Optimization** | Mounts `~/.gradle` cache directory | 4 extracted layers: `dependencies`, `loader`, `snapshots`, `application`. |
| **User Privileges** | `root` / default (simplifies volume mounting) | Non-root `USER spring:spring` |
| **Remote Debugging (JDWP)** | Port `5005` enabled (`-agentlib:jdwp=...`) | **Disabled**, exposes only HTTP port `8080` |
| **JVM Container Flags** | Standard | `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError` |
| **Health Check Probe** | Optional | `HEALTHCHECK --interval=30s CMD curl -f http://localhost:8080/api/v1/health` |

#### 2. Frontend Web Client

| Feature | Development ([`frontend/Dockerfile.dev`](../../../frontend/Dockerfile.dev)) | Production ([`frontend/Dockerfile.prod`](../../../frontend/Dockerfile.prod)) |
| :--- | :--- | :--- |
| **Base Image** | `node:22-alpine` | `nginx:alpine` |
| **Runtime Strategy** | Vite Dev Server with HMR (`npm run dev`) | Hardened Nginx static asset server with SPA routing |
| **Volume Mounting** | `./:/app` for real-time live reload | Compiled static distribution (`dist/` copied from builder stage) |
| **Performance** | Unminified + Inline/External Source Maps | Gzip compression + Cache-Control (`max-age=1y` for hashed assets, `no-cache` for HTML) |
| **Health Check Probe** | Optional | `HEALTHCHECK --interval=30s CMD curl -f http://localhost:80/health` |
| **Port Exposure** | `5173` | `80` (mapped to `3000` via Compose) |

---

## 3. Root Docker Compose Orchestration & Override Architecture

To eliminate configuration sprawl across subfolders and enforce **Twelve-Factor App (Factors III, IV, X)**, all Docker Compose definitions are unified at the repository root:

```text
Document-Knowledge-Operations-Platform/
├── docker-compose.yaml         # Local / Dev: target dev, host mounts, debug port 5005, Compose Watch
├── docker-compose.staging.yaml # Staging: target prod, pre-prod configs, ports
└── docker-compose.prod.yaml    # Production: target prod, hardened, internal DB network, log rotation
```

### A. Execution Commands per Environment

```bash
# 1. Local / Dev inner loop with Compose Watch (HMR + auto sync & rebuild)
cp .env.example .env
docker compose up -d
docker compose watch

# 2. Staging integration verification
cp .env.staging.example .env.staging
docker compose -f docker-compose.staging.yaml --env-file .env.staging up -d --build

# 3. Production deployment
cp .env.prod.example .env.prod
docker compose -f docker-compose.prod.yaml --env-file .env.prod up -d --build
```

### B. Docker Compose Watch Mechanism

In `docker-compose.yaml`, each service defines a `develop.watch` block:
- **`sync`**: Real-time mirror of source directories (`./backend/src`, `./frontend/src`) into containers without container restarts, leveraging native Vite HMR and build tools.
- **`rebuild`**: Automatically triggers image recompilation when dependencies change (`package.json`, `build.gradle`).
