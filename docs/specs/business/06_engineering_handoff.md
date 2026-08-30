# 6. Engineering Handoff & Implementation Guidelines
## Enterprise Document Knowledge & Operations Platform

> **Source of Truth:** Cross-Functional Implementation Guidelines for Backend, AI & Data, Frontend, and QA & Testing Teams.
> **Orchestrated by:** [`MVP.md`](MVP.md)

---

## 1. Overview & Handoff Principles

This specification suite serves as the formal **Business Analysis & Requirements Source of Truth** for cross-functional engineering execution across Backend, AI, Frontend, and Quality Assurance teams.

---

## 2. Engineering Role Guidelines

### 2.1. Backend Engineering (Java 21 / Spring Boot 3)
- **Architecture & Tactical DDD:** Implement Domain Aggregates, Entities, Value Objects, Repository Outbound Ports, Application Services, and REST Inbound Controllers corresponding to `UC-IAM-*`, `UC-DOC-*`, `UC-WF-*`, `UC-HITL-*`, and `UC-AUDIT-*` following Tactical Domain-Driven Design (DDD).
- **Security & Authorization:** Enforce functional RBAC via Spring Security annotations (`@PreAuthorize("hasAuthority('...')")`) at the API Gateway / Web Controller layer, and pass authenticated `UserSecurityContext` (`userId`, `departmentId`, `roleIds`, `isInternal`) down to domain use cases and outbound ports.
- **Transactional & Idempotency Boundaries:** Ensure all state-mutating operations execute within `@Transactional` boundaries, verifying the uniqueness of `idempotency_key` in `action_approvals` prior to commit.
- **Audit Subsystem Integration:** Asynchronously dispatch immutable audit events via `@EventListener` interceptors on every state change and sensitive resource query.

---

### 2.2. AI & Data Engineering (Python 3.11 / FastAPI / pgvector)
- **Architecture & Clean Ports:** Implement the AI microservice using Clean Architecture / Hexagonal Ports & Adapters (`EmbeddingPort`, `VectorStorePort`, `LlmClientPort`, `RerankerPort`).
- **Ingestion Pipeline (`UC-RAG-01`):** Construct recursive chunking (`chunk_size = 500-1000 tokens`, `overlap = 50-100 tokens`) with source page metadata tracking, batch embedding calculation (1536-dimensional vectors), and dual-indexing (HNSW cosine index + GIN tsvector full-text index).
- **Pre-filtered Hybrid RRF Search (`UC-RAG-02`):** Implement single-SQL hybrid search combining pgvector cosine distance (`<=>`) and full-text search (`tsv @@ plainto_tsquery`) with strict SQL-level ACL `WHERE` clauses, ranked via Reciprocal Rank Fusion (RRF).
- **Prompt Contract & Anti-Hallucination Guard (`UC-RAG-03`, `UC-CHAT-01`):** Construct structured prompt contracts with XML/Markdown delimiters. Enforce automated Safe Abstention returning `NO_ACCESSIBLE_KNOWLEDGE` when retrieved chunks are empty or cosine similarity is below $0.50$.

---

### 2.3. Frontend Engineering (React 18 / TypeScript / Vite)
- **User Experience & Navigation:** Build role-aware dashboards and navigation based on permissions received in JWT session payloads.
- **Citation Badges & Drill-Down (`UC-CHAT-02`):** Render inline citation badges `[1]`, `[2]` next to factual claims. On click, open a citation drawer displaying verbatim text, page number, relevance score, and direct PDF page preview via presigned S3 URLs.
- **2-Phase HITL Approval UI (`UC-HITL-01`, `UC-HITL-02`):** Implement visual Diff Previews rendering staged before/after JSON payloads, requiring manager review notes before submitting approval or rejection.
- **Real-Time Notifications (`UC-AUDIT-02`):** Display real-time in-app notification toasts and unread counters for pending approvals and background indexing tasks.

---

### 2.4. QA & Testing Engineers
- **Traceability Verification:** Map test cases directly to the Requirements Traceability Matrix (RTM).
- **Automated Test Pyramid:**
  - **Unit Tests:** Validate domain invariants, permission evaluations, RRF math calculations, and token limit verifications in isolation.
  - **Integration Tests:** Use Testcontainers (PostgreSQL + pgvector) to verify Pre-filtered SQL queries, transaction rollbacks, and S3 binary streaming.
  - **Security & ACL Tests:** Execute automated cross-department penetration test suites to verify that `RESTRICTED` and `CONFIDENTIAL` documents achieve a $0.0\%$ retrieval leakage rate.
  - **E2E & UAT Tests:** Translate `Basic Path`, `Alternative Paths`, and `Business Rules` from use case specifications into automated Playwright / Cypress user flow tests.
