# Database Modules Overview

A collection of DBML files describing independent database bounded contexts for visualization and relationship inspection on [dbdiagram.io](https://dbdiagram.io).

---

### 1. [`01_iam_organization.dbml`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/docs/database/modules/01_iam_organization.dbml) — IAM & Organization
- **Scope:** Manages user identities (`users`), department hierarchy (`departments`), multi-level RBAC (`roles`, `permissions`, `user_roles`, `role_permissions`), and authentication session lifecycle (`refresh_tokens`).

### 2. [`02_document_management.dbml`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/docs/database/modules/02_document_management.dbml) — Document Management & Storage
- **Scope:** Manages document metadata (`documents`), file version history on S3 object storage (`document_versions`), and granular access control lists (ACL) across users (`document_user_access`), departments (`document_department_access`), and roles (`document_role_access`).

### 3. [`03_ai_knowledge_rag.dbml`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/docs/database/modules/03_ai_knowledge_rag.dbml) — AI Knowledge & Hybrid RAG
- **Scope:** Manages extracted document chunks per version (`document_chunks`), stores vector embeddings (`pgvector` with HNSW indexing), and full-text search indexes (`tsvector`) for hybrid semantic retrieval.

### 4. [`04_conversational_ai.dbml`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/docs/database/modules/04_conversational_ai.dbml) — Conversational AI & Citations
- **Scope:** Manages chat sessions (`conversations`), message histories (`conversation_messages`), and source citation mappings (`message_citations`) linking AI responses to verified source documents and chunks.

### 5. [`05_workflow_automation.dbml`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/docs/database/modules/05_workflow_automation.dbml) — Workflow Automation
- **Scope:** Defines automated document-triggered processing pipelines (`workflows`) and tracks execution progress, inputs, and step outputs (`workflow_executions`).

### 6. [`06_operations_hitl.dbml`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/docs/database/modules/06_operations_hitl.dbml) — Operations & Human-in-the-Loop
- **Scope:** Provides two-phase human review before executing sensitive state changes (`action_approvals`) and manages operational tasks requiring human intervention (`operation_tasks`).

### 7. [`07_audit_notifications.dbml`](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/docs/database/modules/07_audit_notifications.dbml) — Audit Trail & Notifications
- **Scope:** Maintains immutable audit logs for security, traceability, and compliance (`audit_logs`), along with user notification dispatching (`notifications`).

