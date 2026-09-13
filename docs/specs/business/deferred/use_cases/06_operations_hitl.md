# Use Case Specifications: Operations & Human-In-The-Loop (`Operations_HITL`)
## Bounded Context 6

> **Source of Truth:** Complete Specification for 2-Phase HITL Action Approvals & Task Management Use Cases (`UC-HITL-01`, `UC-HITL-02`, `UC-HITL-03`).
> **Orchestrated by:** [`../MVP.md`](../MVP.md)

---

### Use Case Specification: `UC-HITL-01`
- **Use Case Name:** 2-Phase Action Approval Request & Preview
- **Stereotype:** Base / Extension Use Case
- **Actor(s):** Knowledge Worker / Staff / AI Assistant (primary), Notification Subsystem (`SYS-05`) (secondary)
- **Extends:** `UC-WF-01` (Trigger-Based Workflow Pipeline Execution)
- **Extension Point:** `Sensitive Operational Step Triggered`
- **Condition:** Triggered when an automated workflow step or manual action involves sensitive state mutations requiring manager sign-off.
- **Includes:** `UC-AUDIT-02` (Real-time In-App Notifications)
- **Summary Description:** Initiates a 2-Phase Human-in-the-Loop action request for sensitive state modifications (e.g., publishing confidential documents or bulk deletions), staging a diff preview and unique idempotency key without committing changes.
- **Priority:** Must Have
- **Status:** Complete Specification
- **Pre-Condition:**
  1. A sensitive action is initiated that requires human authorization.
- **Post-Condition(s):**
  1. A new approval request is created in `action_approvals` with `status = 'PENDING'`.
  2. Staged changes are stored in `preview_payload` JSONB column.
  3. Associated workflow execution (if any) pauses in `WAITING_APPROVAL` status.
  4. In-App notification with type `ACTION_REQUIRED` is dispatched to designated managers via `UC-AUDIT-02`.
- **Basic Path:**
  1. User or AI agent prepares a sensitive operational command (e.g., `PUBLISH_DOCUMENT`).
  2. System generates before/after state diff and packages it into `preview_payload`.
  3. System generates a unique cryptographically random `idempotency_key`.
  4. System inserts approval record into `action_approvals` table.
  5. System pauses parent workflow execution (`status = 'WAITING_APPROVAL'`).
  6. System invokes `UC-AUDIT-02` to dispatch notification to department managers.
  7. Returns HTTP 202 Accepted with approval request ID.
- **Alternative Paths:**
  - 3a. Duplicate `idempotency_key` submitted: System returns existing approval record without re-creating.
- **Business Rules (2-Phase Safety Invariant):**
  - B1: Direct unapproved commits for sensitive actions are strictly prohibited.
  - B2: `preview_payload` must contain complete reproducible snapshot data.
- **Non-Functional Requirements:**
  - NF1: Approval request creation latency $< 100\text{ ms}$.

---

### Use Case Specification: `UC-HITL-02`
- **Use Case Name:** Manager Action Review, Approval & Idempotent Commit
- **Stereotype:** Base Use Case
- **Actor(s):** Department Manager / Admin (primary), Notification Subsystem (`SYS-05`) (secondary)
- **Includes:** `UC-AUDIT-01` (Immutable Audit Trail Logging), `UC-AUDIT-02` (Real-time In-App Notifications)
- **Extends / Extended By:** None
- **Summary Description:** Allows authorized managers to review staged diffs, provide review notes, and either approve (executing an idempotent commit to the database) or reject the action.
- **Priority:** Must Have
- **Status:** Complete Specification
- **Pre-Condition:**
  1. Manager is authenticated with `approve:actions` permission.
  2. Approval record exists in `PENDING` status.
- **Post-Condition(s):**
  1. If approved: Staged changes are committed to domain tables, `action_approvals.status = 'COMMITTED'`, parent workflow resumes.
  2. If rejected: `action_approvals.status = 'REJECTED'`, parent workflow cancelled.
  3. Immutable audit log recorded via `UC-AUDIT-01`.
  4. Requestor receives status notification via `UC-AUDIT-02`.
- **Basic Path:**
  1. Manager opens Pending Approvals queue and selects an approval request.
  2. System renders the visual Diff Preview from `preview_payload`.
  3. Manager reviews changes, enters review notes, and clicks "Approve & Commit".
  4. Backend verifies manager authority and checks `idempotency_key`.
  5. Backend executes staged operation in a transactional boundary (`@Transactional`).
  6. Backend records execution output in `execution_result`.
  7. Backend updates `action_approvals.status = 'COMMITTED'`, `reviewed_by_user_id`, `committed_at`.
  8. Backend resumes linked workflow execution.
  9. Backend invokes `UC-AUDIT-01` to log `APPROVE_ACTION` in `audit_logs`.
  10. Backend invokes `UC-AUDIT-02` to notify the requestor.
  11. Returns HTTP 200 OK.
- **Alternative Paths:**
  - 3a. Manager clicks "Reject": Manager enters mandatory rejection reason, system sets `status = 'REJECTED'`, cancels linked workflow, invokes `UC-AUDIT-01` and `UC-AUDIT-02`, and notifies requestor.
  - 4a. Action was already committed (duplicate click/network retry): System detects existing `COMMITTED` status via `idempotency_key` and returns previous result without re-executing.
- **Business Rules:**
  - B1: Only users with `approve:actions` within the appropriate department can approve.
  - B2: Idempotent execution is guaranteed via unique database constraint on `idempotency_key`.
- **Non-Functional Requirements:**
  - NF1: Commit execution must complete within $< 500\text{ ms}$.

---

### Use Case Specification: `UC-HITL-03`
- **Use Case Name:** Operational Exception Task Management
- **Stereotype:** Base / Extension Use Case
- **Actor(s):** Knowledge Worker / Support Staff (primary), Department Manager (secondary)
- **Extends:** `UC-WF-01` (Trigger-Based Workflow Pipeline Execution)
- **Extension Point:** `Workflow Step Execution Failure`
- **Condition:** Triggered when an automated workflow step encounters an unrecoverable exception requiring human manual correction.
- **Includes:** `UC-AUDIT-01` (Immutable Audit Trail Logging)
- **Summary Description:** Manages operational exception tasks spawned by automated workflow failures or manual requests (e.g., OCR correction, missing document metadata).
- **Priority:** Should Have
- **Status:** Complete Specification
- **Pre-Condition:**
  1. An exception occurs in automated pipeline or a user manually logs a task.
- **Post-Condition(s):**
  1. Task created in `operation_tasks` with assigned priority and assignee.
  2. Audit log recorded via `UC-AUDIT-01`.
- **Basic Path:**
  1. Pipeline failure triggers task creation with title, description, and metadata error logs.
  2. System assigns task to department queue or specific staff user (`assignee_id`).
  3. Assignee updates task status (`PENDING` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `COMPLETED`).
  4. Assignee submits resolution notes.
  5. System marks task completed and optionally re-triggers failed workflow.
  6. System invokes `UC-AUDIT-01` to record task completion.
- **Alternative Paths:**
  - 3a. Assignee cancels task: Task marked `CANCELLED` with explanation.
- **Business Rules:**
  - B1: Tasks support priorities: `LOW`, `MEDIUM`, `HIGH`, `URGENT`.
- **Non-Functional Requirements:**
  - NF1: Real-time task counter updates on manager dashboard.
