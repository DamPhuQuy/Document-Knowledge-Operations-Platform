# Master Repository Agent Guidelines (RIPER-5 Framework)

<agent_guidelines version="4.0">

<overview>
  Master entry-point configuration for AI agents operating at the root of the
  **AI Document & Knowledge Operations Platform** repository.
  
  This repository is a polyglot multi-service architecture comprising:
  - **`backend/`** — Java 21+ Spring Boot core business logic, document operations & persistence
  - **`ai/`** — Python 3.11+ FastAPI microservice for RAG, vector embeddings & LLM workflows
  - **`frontend/`** — React 19 + TypeScript + Vite modern web user interface
  - **`docker-compose*.yaml` & `Makefile`** — Centralized infrastructure and environment orchestration
</overview>

---

## 1. Universal Agent Control Layer Reference

<agent_control_ref>
  The universal, project-agnostic agent control rules live in [`.agents/`](.agents/README.md):

  | File | What it governs |
  |------|----------------|
  | [`.agents/behavior.md`](.agents/behavior.md) | Mode declaration, session startup protocol, context priority |
  | [`.agents/guardrails.md`](.agents/guardrails.md) | Retry budget, escalation triggers, safety conditions, completion gate |
  | [`.agents/conventions/naming.md`](.agents/conventions/naming.md) | Universal naming and structural hygiene |

  Read these files at session start. They require no project-specific edits.
</agent_control_ref>

---

## 2. Subsystem Routing

<subsystem_routing>
  When working within a specific subsystem, follow its specialized guideline:

  | Subsystem | Stack | Guidelines & Task Workflow |
  |-----------|-------|----------------------------|
  | **Backend** | Java 21+, Spring Boot, Gradle, PostgreSQL | [`backend/AGENTS.md`](backend/AGENTS.md) & [`backend/process/`](backend/process/) |
  | **Frontend** | React 19, TypeScript, Vite, Tailwind CSS | [`frontend/AGENTS.md`](frontend/AGENTS.md) & [`frontend/process/`](frontend/process/) |
  | **AI Microservice** | Python 3.11+, FastAPI, uv, pgvector, Chroma | [`ai/AGENTS.md`](ai/AGENTS.md) & [`ai/process/`](ai/process/) |
</subsystem_routing>

---

## 3. Core Architectural Foundations & Harness

<foundations>
  <!-- Pillar 1: Environment & Configuration Single Source of Truth -->
  <pillar id="env_config" title="Configuration & Environment Management">
    <rule>Single Source of Truth: All environment variables are centralized at repository root (`.env.example`, `.env.staging.example`, `.env.prod.example`).</rule>
    <rule>NEVER create nested `.env` or `.env.*` files inside `backend/`, `frontend/`, or `ai/`.</rule>
    <rule>Local development uses `docker-compose.yaml` (default) with Docker Compose Watch for instant live code synchronization.</rule>
  </pillar>

  <!-- Pillar 2: Master Harness & Toolchain Commands -->
  <pillar id="harness" title="Engineering Harness & Orchestration">
    <validation_commands>
      # Run full stack in development mode:
      make dev-watch

      # Service-specific verification commands:
      backend:  cd backend && ./gradlew test && ./gradlew check
      ai:       cd ai && uv run pytest && uv run mypy --strict . && uv run ruff check .
      frontend: cd frontend && npm run type-check && npm run lint && npm run build
    </validation_commands>
    <orchestration>
      Use the root [`Makefile`](Makefile) for standard lifecycle operations:
      - `make dev` / `make dev-watch`: Start all services (PostgreSQL, Backend, AI, Frontend)
      - `make backend-watch`: Isolate and run Backend + PostgreSQL
      - `make ai-watch`: Isolate and run AI Service + PostgreSQL
      - `make frontend-watch`: Run Frontend independently
      - `make down`: Stop all running containers
    </orchestration>
  </pillar>

  <!-- Pillar 3: Task & Specification -->
  <pillar id="task_spec" title="Task & Specification">
    <rule>Cross-cutting tasks touching multiple subsystems must maintain an explicit contract in the active task file.</rule>
    <rule>Acceptance Criteria (AC) must be unambiguous, verifiable markdown checkboxes (`- [ ]`).</rule>
  </pillar>
</foundations>

---

## 4. RIPER-5 Operating Protocol

<riper5_protocol>

  <!-- Working Modes -->
  <working_modes>
    <mode id="PAIR" default="true">Step-by-step collaboration. Halts after each phase for human engineer review and gate sign-off.</mode>
    <mode id="DELEGATED" alias="fast-track,autonomous,skip-permissions">Autonomous delegation. The agent auto-certifies qualifying gates ([AUTO: DELEGATED]), advances task phase, and executes continuously without pausing.</mode>
  </working_modes>

  <!-- Phases: RESEARCH -> INNOVATE -> PLAN -> EXECUTE -> REVIEW -->
  <phases>
    <phase name="RESEARCH" order="1">Read-only exploration, no code modifications.</phase>
    <phase name="INNOVATE" order="2">Generate architectural options and trade-off matrix.</phase>
    <phase name="PLAN" order="3">Draft slice-by-slice implementation plan with verification matrix.</phase>
    <phase name="EXECUTE" order="4">Scoped atomic modifications, verifier execution per slice.</phase>
    <phase name="REVIEW" order="5">Audit diff, zero noise, verify regression safety before completion.</phase>
  </phases>

</riper5_protocol>

---

## 5. Workspace Safety & Hygiene

<workspace_rules>
  <rule id="boundary">Do not modify files across subsystems simultaneously unless executing an explicitly planned cross-system contract.</rule>
  <rule id="destructive_commands">Never run destructive commands (`git push --force`, `git reset --hard`, `rm -rf /`, dropping database tables without approval).</rule>
  <rule id="clean_diff">Before declaring task completion, verify `git status` and `git diff` to ensure zero unintended artifacts or debug code remain.</rule>
</workspace_rules>

</agent_guidelines>
