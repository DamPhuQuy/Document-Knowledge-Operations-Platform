# 4. Non-Functional Requirements (NFRs)
## Enterprise Document Knowledge & Operations Platform
### Ultra-Lean Infrastructure-Focused MVP Specification

> **Source of Truth:** Non-Functional Requirements (NFRs) for Performance, Security, Reliability, Scalability, and AWS Cloud Infrastructure.
> **Orchestrated by:** [`MVP.md`](MVP.md)

---

## 1. Performance & Latency

- **NFR-PERF-01 (Document Upload & S3 Ingestion):** Uploading and securing standard documents (PDF/DOCX up to 20MB) to AWS S3 with SHA-256 integrity verification must complete within **$< 3\text{ seconds}$** over standard broadband.
- **NFR-PERF-02 (API Gateway & Database Query Latency):** Core backend operations (Authentication, Document Metadata queries, ACL evaluation, and Audit logging) must execute with P95 response latency **$< 100\text{ ms}$**.
- **NFR-PERF-03 [Deferred Post-MVP] (Vector Search Latency):** Query latency for Pre-filtered Vector Search combined with ACL checks on PostgreSQL `pgvector` across a $100,000$ chunk dataset must be **$< 25\text{ ms}$** via HNSW vector indexing.
- **NFR-PERF-04 [Deferred Post-MVP] (AI Time to First Token - TTFT):** AI Assistant streaming response latency to the first token via Server-Sent Events (SSE) must be **$< 800\text{ ms}$**.

---

## 2. Security & Compliance

- **NFR-SEC-01 (Zero Data Leakage Guarantee):** 100% of document retrieval must enforce Pre-filtered SQL access validation (`WHERE` clause). Under no circumstances may confidential metadata from unauthorized departments be retrieved.
- **NFR-SEC-02 (Password Hashing & End-to-End TLS):** Passwords must be hashed using **BCrypt** with work factor $\ge 12$. All client-to-Cloudflare and Cloudflare-to-AWS communications must enforce HTTPS/TLS 1.3.
- **NFR-SEC-03 (Stateless JWT & Instant Revocation):** Access Tokens must be short-lived (1-24h). Active sessions must be revocable instantly via the `refresh_tokens` revocation flag.
- **NFR-SEC-04 (Cloud Secret Zero-Leakage):** Zero AWS secrets, IAM root keys, or database credentials may be committed in version control. All credentials must be injected via runtime environment variables or AWS IAM Instance Roles.

---

## 3. Reliability & Data Integrity

- **NFR-REL-01 (Audit Trail Immutability):** The `audit_logs` table operates under an Append-only invariant. Application database users must not possess `UPDATE`, `DELETE`, `DROP`, or `TRUNCATE` grants on audit tables.
- **NFR-REL-02 (Binary Storage Integrity):** All uploaded files must have their SHA-256 hash verified before and after storage in AWS S3 Object Storage.
- **NFR-REL-03 [Deferred Post-MVP] (HITL Idempotency):** 2-phase approval commits in `action_approvals` enforce uniqueness on `idempotency_key` to prevent accidental duplicate state mutations.

---

## 4. AWS Cloud Infrastructure, Deployment & DevOps (Priority: P0 Must Have)

- **NFR-CLOUD-01 (Automated CI/CD Pipeline):** Every push/merge to `develop` or `main` must trigger GitHub Actions to run test suites, package multi-stage Docker containers, and deploy to AWS EC2 via automated SSH/Docker Compose without manual sysadmin intervention.
- **NFR-CLOUD-02 (AWS S3 Durability & SSE-AES256):** Document binary assets must reside in private AWS S3 buckets with default Server-Side Encryption (AES256 enabled), bucket policies blocking public access, and presigned temporary URL access (expiry $\le 15\text{ minutes}$).
- **NFR-CLOUD-03 (Edge Network & SSL Ingress):** Production traffic must ingress through Cloudflare Edge with Universal SSL/TLS certificates (Full Strict Mode), DDoS L3/L4/L7 mitigation, and automated certificate management.
- **NFR-CLOUD-04 (Cost-Aware Resource Allocation):** System must run stably on AWS Free Tier / low-cost compute (e.g., `t2.micro` or `t3.micro` with 30GB gp3 EBS and 3GB automated swap space) using container restart policies (`restart: unless-stopped`) to ensure maximum cost awareness.
- **NFR-CLOUD-05 (Containerization & Portability):** 100% of active application services (`backend`, `database`, `frontend`) must build via multi-stage Dockerfiles and execute in unison via `docker-compose.yaml`.
- **NFR-CLOUD-06 (Operational Health Diagnostics):** The backend must expose a lightweight `/api/v1/health` endpoint reporting database connectivity, S3 storage reachability, and memory utilization.
