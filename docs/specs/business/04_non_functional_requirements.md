# 4. Non-Functional Requirements (NFRs)
## Enterprise Document Knowledge & Operations Platform

> **Source of Truth:** Non-Functional Requirements (NFRs) for Performance, Security, Reliability, and Scalability.
> **Orchestrated by:** [`MVP.md`](MVP.md)

---

## 1. Performance & Latency

- **NFR-PERF-01 (Vector Search Latency):** Query latency for Pre-filtered Vector Search combined with ACL checks on PostgreSQL `pgvector` across a $100,000$ chunk dataset must be **$< 25\text{ ms}$** via HNSW vector indexing and composite B-tree indexes.
- **NFR-PERF-02 (AI Time to First Token - TTFT):** AI Assistant streaming response latency to the first token via Server-Sent Events (SSE) must be **$< 800\text{ ms}$**.
- **NFR-PERF-03 (Ingestion Pipeline Throughput):** Document parsing, recursive chunking, and embedding generation for a standard 20-page PDF document must complete within **$< 5\text{ seconds}$**.

---

## 2. Security & Compliance

- **NFR-SEC-01 (Zero Data Leakage Guarantee):** 100% of RAG context retrieval must enforce Pre-filtered SQL access validation. Under no circumstances may confidential chunks from unauthorized departments be retrieved into LLM context.
- **NFR-SEC-02 (Password Hashing & Encryption):** Passwords must be hashed using **BCrypt** with work factor $\ge 12$. All client-to-backend and backend-to-AI communications must use HTTPS/TLS.
- **NFR-SEC-03 (Stateless JWT & Instant Revocation):** Access Tokens must be short-lived (1-24h). Active sessions must be revocable instantly via the `refresh_tokens` revocation flag.

---

## 3. Reliability & Idempotency

- **NFR-REL-01 (Audit Trail Immutability):** The `audit_logs` table operates under an Append-only invariant. Application database users must not possess `UPDATE` or `DELETE` grants on audit tables.
- **NFR-REL-02 (HITL Idempotency):** All 2-phase approval commits in `action_approvals` must enforce uniqueness on `idempotency_key` to prevent accidental duplicate state mutations from network retries.
- **NFR-REL-03 (Binary Storage Integrity):** All uploaded files must have their SHA-256 hash verified before and after storage in Object Storage.

---

## 4. Scalability & Operational Portability

- **NFR-OPS-01 (Containerization):** 100% of application components (`backend`, `ai`, `database`) must build via multi-stage Dockerfiles and execute in unison via `docker-compose.yaml`.
- **NFR-OPS-02 (S3 API Compatibility):** Object storage integrations must adhere strictly to the AWS S3 API standard, allowing seamless transition between local dev (Floci/MinIO) and production AWS S3 buckets without code modifications.
