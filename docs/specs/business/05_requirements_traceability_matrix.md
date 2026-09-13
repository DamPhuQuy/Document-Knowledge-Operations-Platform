# 5. Requirements Traceability Matrix (RTM)
## Enterprise Document Knowledge & Operations Platform
### Ultra-Lean Infrastructure-Focused MVP Specification

> **Source of Truth:** Master Requirements Traceability Matrix (RTM) mapping Business Problems, Bounded Contexts, Active Use Cases, Database Tables, API Endpoints, and Owning Modules.
> **Orchestrated by:** [`MVP.md`](MVP.md)

---

## 1. Active Ultra-Lean MVP Traceability Matrix

The following matrix maps the active core business and infrastructure requirements for the MVP:

| Business & Cloud Goal | Bounded Context | Use Case / Requirement ID | Stereotype / Relations | Related Database Tables (`schema.dbml`) / Cloud Target | Target API Endpoint / Technical Port | Owning Module | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :---: |
| **Automated CI/CD & Deploy** | `Cloud_DevOps` | `REQ-CLOUD-01` | Automated Pipeline | GitHub Actions, Docker Engine, AWS EC2 | `SSH Deploy / docker-compose` | `devops` | **P0 (Must Have)** |
| **Durable Blob Storage & SSE** | `Cloud_DevOps` | `REQ-CLOUD-02` | Cloud Storage Provider | AWS S3 Bucket (`SSE-AES256`) | `S3 Client / Presigned URLs` | `backend` & `cloud` | **P0 (Must Have)** |
| **Network Security & Ingress** | `Cloud_DevOps` | `REQ-CLOUD-03` | Edge Network | AWS VPC, Security Groups, Cloudflare | `HTTPS / 443 (Universal SSL)` | `devops` | **P0 (Must Have)** |
| **Health Diagnostics** | `System_Diagnostics` | `REQ-CLOUD-04` | Diagnostic Check | In-memory / DB Ping / S3 Check | `GET /api/v1/health` | `backend` (`system`) | **P0 (Must Have)** |
| **Authentication & Sessions** | `IAM_Organization` | `UC-IAM-01` | Base (includes `UC-AUDIT-01`) | `users`, `roles`, `permissions`, `refresh_tokens` | `POST /api/v1/auth/login` | `backend` (IAM) | **P0 (Must Have)** |
| **Multi-Role RBAC** | `IAM_Organization` | `UC-IAM-02` | Base (includes `UC-AUDIT-01`) | `user_roles`, `role_permissions` | `PUT /api/v1/users/{id}/roles` | `backend` (IAM) | **P0 (Must Have)** |
| **Department Scoping** | `IAM_Organization` | `UC-IAM-03` | Base (includes `UC-AUDIT-01`) | `departments`, `users` | `POST /api/v1/departments` | `backend` (IAM) | **P0 (Must Have)** |
| **Document Upload & S3** | `Document_Management` | `UC-DOC-01` | Base (extended by `UC-DOC-02`, includes `UC-AUDIT-01`) | `documents`, `document_versions` | `POST /api/v1/documents` | `backend` (DMS) | **P0 (Must Have)** |
| **Document Versioning** | `Document_Management` | `UC-DOC-02` | Extension of `UC-DOC-01` | `document_versions` | `POST /api/v1/documents/{id}/versions` | `backend` (DMS) | **P0 (Must Have)** |
| **4-Tier ACL Matrix** | `Document_Management` | `UC-DOC-03` | Base (includes `UC-AUDIT-01`) | `document_user_access`, `document_dept_access`, `document_role_access` | `PUT /api/v1/documents/{id}/permissions` | `backend` (DMS) | **P0 (Must Have)** |
| **Document Soft Deletion** | `Document_Management` | `UC-DOC-04` | Base (includes `UC-AUDIT-01`) | `documents` (`deleted_at`) | `DELETE /api/v1/documents/{id}` | `backend` (DMS) | **P0 (Must Have)** |
| **Immutable Audit Logging** | `Audit_System` | `UC-AUDIT-01` | Included / Base (Append-only) | `audit_logs` (Append-only) | `GET /api/v1/audit-logs` | `backend` (Audit) | **P0 (Must Have)** |

---

## 2. Deferred Post-MVP Requirements Reference

The following requirements have been relocated to [`deferred/`](deferred/) for post-MVP implementation:

| Bounded Context | Use Case ID | Name / Capability | Source Specification File | Target Milestone |
| :--- | :--- | :--- | :--- | :---: |
| `Workflow_Automation` | `UC-WF-01` | Trigger-Based Pipeline Execution | [`deferred/use_cases/05_workflow_automation.md`](deferred/use_cases/05_workflow_automation.md) | Phase 2 |
| `Workflow_Automation` | `UC-WF-02` | Workflow Execution Monitoring | [`deferred/use_cases/05_workflow_automation.md`](deferred/use_cases/05_workflow_automation.md) | Phase 2 |
| `Operations_HITL` | `UC-HITL-01` | 2-Phase Action Request & Preview | [`deferred/use_cases/06_operations_hitl.md`](deferred/use_cases/06_operations_hitl.md) | Phase 2 |
| `Operations_HITL` | `UC-HITL-02` | Manager Action Review, Approval & Commit | [`deferred/use_cases/06_operations_hitl.md`](deferred/use_cases/06_operations_hitl.md) | Phase 2 |
| `Operations_HITL` | `UC-HITL-03` | Operational Exception Task Handling | [`deferred/use_cases/06_operations_hitl.md`](deferred/use_cases/06_operations_hitl.md) | Phase 2 |
| `Audit_System` | `UC-AUDIT-02` | Real-time In-App Notifications | [`deferred/use_cases/07_notifications.md`](deferred/use_cases/07_notifications.md) | Phase 2 |
| `AI_Knowledge_RAG` | `UC-RAG-01` | Document Ingestion & 1536d Vector Indexing | [`deferred/use_cases/03_ai_knowledge_rag.md`](deferred/use_cases/03_ai_knowledge_rag.md) | Phase 2 |
| `AI_Knowledge_RAG` | `UC-RAG-02` | Pre-filtered Hybrid RRF Search | [`deferred/use_cases/03_ai_knowledge_rag.md`](deferred/use_cases/03_ai_knowledge_rag.md) | Phase 2 |
| `AI_Knowledge_RAG` | `UC-RAG-03` | Anti-Hallucination Safe Abstention | [`deferred/use_cases/03_ai_knowledge_rag.md`](deferred/use_cases/03_ai_knowledge_rag.md) | Phase 2 |
| `Conversational_AI` | `UC-CHAT-01` | Multi-Turn Conversational RAG with Citations | [`deferred/use_cases/04_conversational_ai.md`](deferred/use_cases/04_conversational_ai.md) | Phase 2 |
| `Conversational_AI` | `UC-CHAT-02` | Evidence Citation Drill-Down & Verification | [`deferred/use_cases/04_conversational_ai.md`](deferred/use_cases/04_conversational_ai.md) | Phase 2 |
| `Conversational_AI` | `UC-CHAT-03` | Token Usage & Confidence Tracking | [`deferred/use_cases/04_conversational_ai.md`](deferred/use_cases/04_conversational_ai.md) | Phase 2 |
