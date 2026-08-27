# Multi-Environment Configuration

A concise architectural reference summarizing configuration design, rules, and differences across environments (**Local**, **Dev / Staging**, and **Prod**) for the **Document and Knowledge Operations Platform**.

---

## 1. 3-Tier Configuration Architecture

```text
┌────────────────────────────────────────────────────────────────────────┐
│ 1. Runtime Environment (.env / Secret Manager / K8s ConfigMap)         │
│    -> Dynamic injection: DB credentials, JWT secrets, service URLs     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ 2. Spring Boot Profiles (application.yaml -> application-{profile}.yaml│
│    -> Framework parameters: HikariCP, JPA, Logging, Swagger, CORS      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ 3. Containerization Strategy (Dockerfile & Docker Compose)             │
│    -> Dev: JDK + Hot-reload + JDWP Debug | Prod: JRE Slim + Layered JAR│
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Multi-Environment Comparison Matrix

### A. Environment Variables (`.env`)

| Variable                 | Local (`.env.example`)  | Dev (`.env.dev.example`)      | Prod (`.env.prod.example`)         | Description & Security Principle                           |
| :----------------------- | :---------------------- | :---------------------------- | :--------------------------------- | :--------------------------------------------------------- |
| `SPRING_PROFILES_ACTIVE` | `local`                 | `dev`                         | `prod`                             | Activates target Spring Boot profile                       |
| `DB_HOST`                | `localhost`             | `postgres-dev.internal`       | `postgres-primary.prod.internal`   | PostgreSQL host (Local / Staging / Cloud Managed RDS)      |
| `DB_NAME`                | `doc_knowledge_db`      | `doc_knowledge_dev`           | `doc_knowledge_prod`               | Environment database schema segregation                    |
| `JWT_SECRET`             | Dummy 256-bit           | Dev Secret 256-bit            | KMS / Vault 512-bit                | **Prod must inject via KMS / K8s Secret**, never plaintext |
| `JWT_EXPIRATION_MS`      | `86400000` (24h)        | `43200000` (12h)              | `3600000` (1h)                     | Prod uses short TTL + Refresh Token Rotation               |
| `CORS_ALLOWED_ORIGINS`   | `localhost:3000, 5173`  | `https://dev.docknowledge...` | `https://docknowledge.company.com` | Strict domain whitelisting on production                   |
| `AI_SERVICE_URL`         | `http://localhost:8000` | `http://ai-service-dev:8000`  | `http://ai-service.prod.internal`  | Internal VPC endpoint for AI microservice                  |

---

### B. Spring Boot Profiles (`application.yaml`)

| Setting                    | Local / Dev (`application-dev.yaml`)                   | Production (`application-prod.yaml`)                     | Rationale                                                                               |
| :------------------------- | :----------------------------------------------------- | :------------------------------------------------------- | :-------------------------------------------------------------------------------------- |
| **Hikari Connection Pool** | `min-idle: 5`<br>`max-size: 20`                        | `min-idle: 10`<br>`max-size: 50`+                        | Dev saves DB resources; Prod pre-warms pool for high concurrency and lower latency.     |
| **JPA / Hibernate SQL**    | `show-sql: true`<br>`format_sql: true`                 | `show-sql: false`<br>`format_sql: false`                 | Formatting/printing SQL in Prod causes severe I/O bottlenecks and data leakage.         |
| **DDL Validation**         | `validate`                                             | `validate` / `none`                                      | Never use `update` or `create`. Schema is exclusively managed via Liquibase.            |
| **Swagger / OpenAPI UI**   | `enabled: true`                                        | `enabled: false`                                         | Disabled in Prod to minimize public attack surface and avoid exposing internal schemas. |
| **Logging Level**          | `com.platform.app: DEBUG`<br>`org.hibernate.SQL: INFO` | `root: WARN`<br>`com.platform.app: INFO`<br>`SQL: ERROR` | Reduces log volume/costs (Datadog/CloudWatch) and boosts throughput.                    |

---

### C. Dockerfile & Container Strategy

| Feature                       | Development ([`Dockerfile.dev`](../../../backend/Dockerfile.dev)) | Production ([`Dockerfile.prod`](../../../backend/Dockerfile.prod))              |
| :---------------------------- | :------------------------------------------------------------------------------------------------------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------- |
| **Base Image**                | `eclipse-temurin:25-jdk-alpine` (Full JDK)                                                                                 | `eclipse-temurin:25-jre-alpine` (Minimal JRE Slim)                                                                                       |
| **Build Strategy**            | Executes source code directly via Gradle Wrapper                                                                           | **Multi-stage Build** + **Layered JAR** (`jarmode=tools extract`)                                                                        |
| **Docker Cache Optimization** | Mounts `~/.gradle` cache directory                                                                                         | 4 extracted layers: `dependencies`, `loader`, `snapshots`, `application`. Code changes only invalidate the application layer (~few MBs). |
| **User Privileges**           | `root` / default (simplifies volume mounting)                                                                              | Non-root `USER spring:spring` (prevents container escape vulnerabilities)                                                                |
| **Remote Debugging (JDWP)**   | Port `5005` enabled (`-agentlib:jdwp=...`)                                                                                 | **Disabled**, exposes only HTTP port `8080`                                                                                              |
| **JVM Container Flags**       | Standard                                                                                                                   | `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError` (Cgroups memory awareness)                              |
| **Health Check Probe**        | Optional                                                                                                                   | `HEALTHCHECK --interval=30s CMD curl -f http://localhost:8080/api/v1/health`                                                             |
