# Use Case Specifications: Workflow Automation (`Workflow_Automation`)
## Bounded Context 5

> **Source of Truth:** Complete Specification for Workflow Automation & Pipeline Execution Use Cases (`UC-WF-01`, `UC-WF-02`).
> **Orchestrated by:** [`../MVP.md`](../MVP.md)

---

### Use Case Specification: `UC-WF-01`
- **Use Case Name:** Trigger-Based Workflow Pipeline Execution
- **Stereotype:** Base Use Case
- **Actor(s):** Workflow Orchestrator (`SYS-03`) (primary), Document Management Subsystem (secondary)
- **Includes:** `UC-RAG-01` (Document Ingestion & Vector Indexing)
- **Extended By:**
  - `UC-HITL-01` (2-Phase Action Request & Preview) at Extension Point `Sensitive Operational Step Triggered`
  - `UC-HITL-03` (Operational Exception Task Handling) at Extension Point `Workflow Step Execution Failure`
- **Summary Description:** Automatically triggers multi-step processing workflows upon system events (e.g., `ON_DOCUMENT_UPLOADED`), orchestrating parsing, indexing, classification, and safety checks according to active workflow definitions.
- **Priority:** Must Have
- **Status:** Complete Specification
- **Pre-Condition:**
  1. A target event is published (e.g., `DocumentUploadedEvent`).
  2. An active workflow template exists in `workflows` for this trigger event.
- **Post-Condition(s):**
  1. A new execution instance is created in `workflow_executions` with `status = 'RUNNING'`.
  2. Step results are sequentially recorded in `step_results` JSONB column.
  3. Status terminates as `COMPLETED` or `FAILED`.
- **Basic Path:**
  1. System detects event `ON_DOCUMENT_UPLOADED` for document `doc-100`.
  2. Orchestrator queries active workflow definition from `workflows`.
  3. Orchestrator instantiates a record in `workflow_executions` with `status = 'RUNNING'`.
  4. Orchestrator executes Step 1 (Text Extraction & Metadata Parsing).
  5. Orchestrator records Step 1 output in `step_results` and advances `current_step`.
  6. Orchestrator invokes `UC-RAG-01` (Vector Embedding & Indexing via AI Service).
  7. Orchestrator records Step 2 output in `step_results`.
  8. Orchestrator sets `status = 'COMPLETED'` and records `completed_at`.
- **Alternative Paths:**
  - 4a. Step 1 encounters sensitive state mutation: System invokes `UC-HITL-01` extension to pause pipeline and await human approval.
  - 4b. Step 1 fails: Orchestrator records error in `error_message`, updates `status = 'FAILED'`, and invokes `UC-HITL-03` extension to create an operation task for manual intervention.
- **Business Rules:**
  - B1: Workflow execution must be idempotent; re-triggering with same payload must not duplicate database states.
- **Non-Functional Requirements:**
  - NF1: End-to-end automated pipeline completion time $< 10\text{ seconds}$ for standard documents.

---

### Use Case Specification: `UC-WF-02`
- **Use Case Name:** Workflow Execution Monitoring
- **Stereotype:** Base Use Case
- **Actor(s):** Department Manager / System Admin (primary), Workflow Engine (secondary)
- **Includes:** None
- **Extends / Extended By:** None
- **Summary Description:** Provides operational visibility into running and historical workflow execution instances, showing real-time step progress, execution logs, payload data, and error diagnostics.
- **Priority:** Must Have
- **Status:** Complete Specification
- **Pre-Condition:**
  1. User holds `manage:workflows` or `ROLE_ADMIN`/`ROLE_MANAGER`.
- **Post-Condition(s):**
  1. Filtered list of execution instances with step status and timestamps is displayed.
- **Basic Path:**
  1. Manager opens Workflow Operations dashboard.
  2. System fetches list of executions with filters (status, document, date range).
  3. Manager clicks on a specific execution instance.
  4. System displays full execution details: `current_step`, `step_results`, `error_message`, duration.
- **Alternative Paths:**
  - 2a. No executions found for filter: System displays empty state message.
- **Business Rules:**
  - B1: Managers can only view workflows executed within their own department scope.
- **Non-Functional Requirements:**
  - NF1: Execution list query response time $< 150\text{ ms}$.
