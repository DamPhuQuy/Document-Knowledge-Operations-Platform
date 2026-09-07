# AI Document & Knowledge Operations Platform

> An AI-assisted platform for managing documents, retrieving organizational knowledge, and executing controlled knowledge workflows.

---

## 1. Quick Start

### A. Prerequisites
- **Java 25** (or Java 21+)
- **Python 3.11+** & **`uv`**
- **Node.js 20+** & **npm**
- **Docker & Docker Compose**

### B. Start Services

#### Option 1: Docker Compose Multi-Environment (Recommended)

```bash
# 1. Local Development (with Compose Watch & Live Hot Reload)
cp .env.example .env
docker compose up -d
docker compose watch
# Or using Makefile shorthand:
make dev-watch          # Chạy toàn bộ stack (Postgres, Backend, AI, Frontend) kèm Compose Watch
make backend-watch      # Chỉ chạy Backend (+ PostgreSQL) kèm Compose Watch
make frontend-watch     # Chỉ chạy Frontend độc lập kèm Compose Watch
make ai-watch           # Chỉ chạy AI Microservice (+ PostgreSQL) kèm Compose Watch
make db                 # Chỉ chạy PostgreSQL (pgvector)

# 2. Staging / Pre-Production (Parity testing with production-grade containers)
cp .env.staging.example .env.staging
docker compose -f docker-compose.staging.yaml --env-file .env.staging up -d --build
# Or using Makefile shorthand:
make staging

# 3. Production Deployment (Hardened, internal DB isolation, log rotation)
cp .env.prod.example .env.prod
docker compose -f docker-compose.prod.yaml --env-file .env.prod up -d --build
# Or using Makefile shorthand:
make prod
```

#### Option 2: Run Services Locally on Host (Bare-Metal)

1. **Start Infrastructure only (PostgreSQL 16 + pgvector)**:
   ```bash
   cp .env.example .env
   docker compose up -d postgres
   ```
2. **Start Backend API (Spring Boot)**:
   ```bash
   cd backend && ./gradlew bootRun
   # Running at: http://localhost:8080 | Swagger: http://localhost:8080/swagger-ui.html
   ```
3. **Start AI Microservice (FastAPI + RAG)**:
   ```bash
   cd ai && uv run python src/ai/main.py
   # Running at: http://localhost:8000 | Swagger: http://localhost:8000/docs
   ```
4. **Start Frontend Web Client (React + Vite)**:
   ```bash
   cd frontend && npm install && npm run dev
   # Running at: http://localhost:5173
   ```

---

## 2. Repository Structure

```text
Document-Knowledge-Operations-Platform/
├── backend/                    # Spring Boot service (Java 25, Security, Liquibase, JPA)
├── ai/                         # AI & Hybrid RAG microservice (Python 3.11+, FastAPI, pgvector)
├── frontend/                   # Web Client (React 19, TypeScript, Vite)
├── docs/
│   ├── specs/                  # [Source of Truth] System specifications, architecture, and schemas
│   │   ├── architecture.md
│   │   ├── security_and_rag_access_control.md
│   │   ├── configurations/note.md
│   │   └── database/
│   │       ├── schema.dbml
│   │       └── V1__init.md
│   ├── reports/                # Formal project and milestone reports
│   └── research/               # Engineering drafts and vertical slice roadmaps
├── docker-compose.yaml         # Local / Dev orchestration (Mounts, JDWP port 5005, Vite HMR, Compose Watch)
├── docker-compose.staging.yaml # Staging orchestration (Pre-prod verification, multi-stage prod target)
├── docker-compose.prod.yaml    # Production orchestration (Hardened, internal network DB, log rotation)
├── .env.example                # Local / Development environment template (cp .env.example .env)
├── .env.staging.example        # Staging environment template (cp .env.staging.example .env.staging)
├── .env.prod.example           # Production environment template (cp .env.prod.example .env.prod)
├── Makefile                    # Developer ergonomics for Docker workflows
└── README.md
```

---

## 3. Technology Stack

| Area | Technology | Role |
| :--- | :--- | :--- |
| **Frontend** | React 19, TypeScript, Vite | Document & Operations Web Interface |
| **Backend** | Java 25, Spring Boot 4, Liquibase | Core Business Logic, Identity, ACL & Orchestration |
| **AI Service** | Python 3.11+, FastAPI, `uv`, pgvector | Ingestion, Embeddings, Dense+FTS Hybrid RAG, Citations |
| **Database** | PostgreSQL 16+, pgvector | Relational Store & High-Dimensional Vector Search |
| **Container** | Docker, Multi-stage Builds | Multi-environment deployment |

---

## 4. Documentation Map (Source of Truth)

- **System Architecture**: [`docs/specs/architecture.md`](docs/specs/architecture.md)
- **Configuration & Environments**: [`docs/specs/configurations/note.md`](docs/specs/configurations/note.md)
- **Database Schema & DDL**: [`docs/specs/database/schema.dbml`](docs/specs/database/schema.dbml) & [`docs/specs/database/V1__init.md`](docs/specs/database/V1__init.md)
- **Security & RAG ACL Filtering**: [`docs/specs/security_and_rag_access_control.md`](docs/specs/security_and_rag_access_control.md)
