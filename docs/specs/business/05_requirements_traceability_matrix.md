# 5. Requirements Traceability Matrix (RTM)
## Enterprise Document Knowledge & Operations Platform

> **Source of Truth:** Master Requirements Traceability Matrix (RTM) mapping Business Problems, Bounded Contexts, Use Cases, Database Tables, API Endpoints, and Owning Modules.
> **Orchestrated by:** [`MVP.md`](MVP.md)

---

## 1. Master Requirements Traceability Matrix

The following matrix maps business problems and operational requirements to their corresponding Bounded Contexts, Use Case specifications, database schemas, and architectural components:

| Business Problem | Bounded Context | Use Case ID | Stereotype / Relations | Related Database Tables (`schema.dbml`) | Target API Endpoint / Technical Port | Owning Module |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Identity Management & Data Security** | `IAM_Organization` | `UC-IAM-01` | Base (includes `UC-AUDIT-01`) | `users`, `roles`, `permissions`, `refresh_tokens` | `POST /api/v1/auth/login` | `backend` (IAM) |
| | | `UC-IAM-02` | Base (includes `UC-AUDIT-01`) | `user_roles`, `role_permissions` | `PUT /api/v1/users/{id}/roles` | `backend` (IAM) |
| | | `UC-IAM-03` | Base (includes `UC-AUDIT-01`) | `departments`, `users` | `POST /api/v1/departments` | `backend` (IAM) |
| **Document Fragmentation & Versioning** | `Document_Management` | `UC-DOC-01` | Base (extended by `UC-DOC-02`, includes `UC-AUDIT-01`) | `documents`, `document_versions` | `POST /api/v1/documents` | `backend` (Document DDD) |
| | | `UC-DOC-02` | Extension of `UC-DOC-01` | `document_versions` | `POST /api/v1/documents/{id}/versions` | `backend` (Document DDD) |
| **Data Leakage via Weak Access Controls**| `Document_Management` | `UC-DOC-03` | Base (includes `UC-AUDIT-01`) | `document_user_access`, `document_dept_access`, `document_role_access` | `PUT /api/v1/documents/{id}/permissions` | `backend` (Document DDD) |
| | | `UC-DOC-04` | Base (includes `UC-AUDIT-01`) | `documents` (`deleted_at`) | `DELETE /api/v1/documents/{id}` | `backend` (Document DDD) |
| **Slow Search & Low Recall** | `AI_Knowledge_RAG` | `UC-RAG-01` | Base / Included by `UC-WF-01` | `document_chunks` (`embedding`, `tsv`) | `POST /api/v1/rag/ingest` | `ai` (`EmbeddingPort`, `PgVectorStore`) |
| **AI Data Leakage & Hallucination** | `AI_Knowledge_RAG` | `UC-RAG-02` | Base / Included by `UC-CHAT-01`, extended by `UC-RAG-03` | `document_chunks`, `documents`, ACL tables | `POST /api/v1/rag/search` | `ai` (Pre-filtered SQL Hybrid RRF) |
| | | `UC-RAG-03` | Extension of `UC-RAG-02` | `audit_logs` | `NO_ACCESSIBLE_KNOWLEDGE` | `ai` (Anti-Hallucination Guard) |
| **Time-Consuming Search & Unverified AI**| `Conversational_AI` | `UC-CHAT-01` | Base (includes `UC-RAG-02`, `UC-CHAT-03`, extended by `UC-CHAT-02`) | `conversations`, `conversation_messages` | `POST /api/v1/conversations/{id}/messages` | `backend` & `ai` (Chat Service) |
| | | `UC-CHAT-02` | Extension of `UC-CHAT-01` | `message_citations`, `document_chunks` | `GET /api/v1/citations/{id}` | `frontend` & `backend` (Citations) |
| | | `UC-CHAT-03` | Included by `UC-CHAT-01` | `conversation_messages` (`tokens`, `confidence`) | `GET /api/v1/conversations/{id}/analytics` | `backend` (Analytics) |
| **Slow Manual Workflows** | `Workflow_Automation` | `UC-WF-01` | Base (includes `UC-RAG-01`, extended by `UC-HITL-01`, `UC-HITL-03`) | `workflows`, `workflow_executions` | `POST /api/v1/workflows/{id}/execute` | `backend` (Workflow Orchestrator) |
| | | `UC-WF-02` | Base | `workflow_executions` | `GET /api/v1/workflow-executions` | `backend` (Workflow Monitor) |
| **AI Lacks Governed Action Capabilities** | `Operations_HITL` | `UC-HITL-01` | Extension of `UC-WF-01` (includes `UC-AUDIT-02`) | `action_approvals` (`preview_payload`, `idempotency_key`) | `POST /api/v1/approvals` | `backend` (HITL Engine) |
| | | `UC-HITL-02` | Base (includes `UC-AUDIT-01`, `UC-AUDIT-02`) | `action_approvals` (`status = COMMITTED`) | `POST /api/v1/approvals/{id}/review` | `backend` (HITL Engine) |
| | | `UC-HITL-03` | Extension of `UC-WF-01` (includes `UC-AUDIT-01`) | `operation_tasks` | `POST /api/v1/tasks` | `backend` (Operations Task) |
| **Lack of Accountability & Auditability**| `Audit_System` | `UC-AUDIT-01` | Included / Base (Append-only) | `audit_logs` (Append-only) | `GET /api/v1/audit-logs` | `backend` (Audit Subsystem) |
| **Disconnected Team Communications** | `Audit_System` | `UC-AUDIT-02` | Included / Base | `notifications` | `GET /api/v1/notifications` | `backend` (Notification Service) |
