# 2. Actor Matrix & Role-Based Access Control (RBAC)
## Enterprise Document Knowledge & Operations Platform

> **Source of Truth:** Human Stakeholder Personas, Machine & System Actors, UML Actor Hierarchy, and Functional Permissions Matrix (`<action>:<resource>` vs Roles).
> **Orchestrated by:** [`MVP.md`](MVP.md)

---

## 1. Human Stakeholders & Personas

| Actor Code | Role Name | System Role (`roles`) | Responsibilities & Business Scope |
| :--- | :--- | :--- | :--- |
| **ACT-00** | **System User** | `Authenticated User` | Base abstract human actor authenticated with a valid JWT session; inherits baseline self-service profile and notification capabilities. |
| **ACT-01** | **System Administrator** | `ROLE_ADMIN` | Manages users, department hierarchy, system configurations, reviews full audit logs, and oversees high-level automated workflows. |
| **ACT-02** | **Department Manager** | `ROLE_MANAGER` | Manages department documents, reviews and approves/rejects sensitive operational actions (HITL Approvals), assigns and tracks operation tasks. |
| **ACT-03** | **Knowledge Worker / Staff** | `ROLE_STAFF` | Uploads documents, manages personal workspaces, queries the AI Assistant within granted ACL permissions, and submits action approval requests. |
| **ACT-04** | **Compliance & Legal Auditor** | `ROLE_LEGAL_AUDITOR` | Queries compliance documents, inspects verbatim AI citations against source materials, and examines immutable audit logs for compliance audits. |
| **ACT-05** | **External Client / Partner** | `ROLE_CUSTOMER` | Limited access to `PUBLIC` documents or specifically shared ACLs; prohibited from accessing internal department knowledge. |

---

## 2. Machine & Supporting System Actors

Supporting system actors for the **Ultra-Lean MVP** and future extensions:

| Actor Code | System Actor Name | Technical Component | Automated Responsibilities | Status |
| :--- | :--- | :--- | :--- | :--- |
| **EXT-03** | **AWS Cloud & Edge Platform** | AWS EC2 / S3 / Cloudflare / GitHub Actions | Hosts Docker runtime, terminates HTTPS/TLS, manages security groups, and automates CI/CD deployment. | **Active MVP (P0)** |
| **EXT-01** | **AWS S3 Object Storage** | AWS S3 / Compatible Blob Storage | Remote cloud blob storage for binary files (PDF, DOCX, XLSX) with SSE-AES256 and presigned URLs. | **Active MVP (P0)** |
| **SYS-04** | **Audit Subsystem** | `backend` (`@EventListener` & Interceptor)| Immutably records all authentication, CRUD mutations, and ACL updates into the `audit_logs` table. | **Active MVP (P0)** |
| **SYS-03** | **Workflow Orchestrator** | `backend` (Spring Boot App Service) | *(Deferred)* Coordinates multi-step document pipelines and handles event triggers. | *Deferred* |
| **SYS-05** | **Notification Subsystem**| `backend` (Notification Service) | *(Deferred)* Dispatches real-time in-app alerts and pending approval notifications. | *Deferred* |
| **SYS-01** | **AI Ingestion Worker** | `ai` (FastAPI Background Task) | *(Deferred)* Ingests files, extracts text, performs chunking, and indexes 1536d vectors. | *Deferred* |
| **SYS-02** | **Hybrid RAG Engine** | `ai` (FastAPI + pgvector + BM25 FTS) | *(Deferred)* Executes Pre-filtered SQL hybrid retrieval and formats grounded citations. | *Deferred* |
| **EXT-02** | **Embedding & LLM API** | OpenAI / Gemini API Provider | *(Deferred)* External foundation models for embeddings and completions. | *Deferred* |

---

## 3. Actor Hierarchy & Generalization

In UML standards, specialized human actors inherit general capabilities from the base `System User`:

```mermaid
flowchart TD
    User["System User\n(ACT-00: Authenticated)"]:::baseActor
    Admin["System Administrator\n(ACT-01: ROLE_ADMIN)"]:::actor
    Manager["Department Manager\n(ACT-02: ROLE_MANAGER)"]:::actor
    Staff["Knowledge Worker / Staff\n(ACT-03: ROLE_STAFF)"]:::actor
    Auditor["Compliance & Legal Auditor\n(ACT-04: ROLE_LEGAL_AUDITOR)"]:::actor
    Customer["External Client\n(ACT-05: ROLE_CUSTOMER)"]:::actor

    Admin -- "|> specializes" --> User
    Manager -- "|> specializes" --> User
    Staff -- "|> specializes" --> User
    Auditor -- "|> specializes" --> User
    Customer -- "|> specializes" --> User

    classDef baseActor fill:#e9ecef,stroke:#495057,stroke-width:2px,color:#212529;
    classDef actor fill:#f8f9fa,stroke:#495057,stroke-width:2px,color:#212529;
```

---

## 4. Functional Permissions Matrix (Permissions vs. Roles)

Fine-grained permissions follow the standard format `<action>:<resource>` (stored in `permissions` and `role_permissions`):

| Permission Code | Business Meaning | `ADMIN` | `MANAGER` | `STAFF` | `LEGAL_AUDITOR` | `CUSTOMER` | MVP Status |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| `read:documents` | Search, view, and query available documents | Allowed | Allowed | Allowed | Allowed | Allowed *(Public)* | **Active MVP** |
| `write:documents` | Upload new documents and publish revisions | Allowed | Allowed | Allowed | - | - | **Active MVP** |
| `delete:documents` | Soft-delete documents from the workspace | Allowed | Allowed | - | - | - | **Active MVP** |
| `manage:permissions` | Grant or revoke document ACL access | Allowed | Allowed | Allowed *(Own docs)* | - | - | **Active MVP** |
| `manage:users` | Provision, deactivate, and assign user roles | Allowed | - | - | - | - | **Active MVP** |
| `read:audit_logs` | Inspect system-wide security audit trail | Allowed | - | - | Allowed | - | **Active MVP** |
| `manage:workflows` | Define and trigger automated workflows | Allowed | Allowed | - | - | - | *Deferred* |
| `approve:actions` | Approve or reject sensitive HITL actions | Allowed | Allowed | - | - | - | *Deferred* |

