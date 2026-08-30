# Use Case Specifications: Audit Subsystem & Notifications (`Audit_System`)
## Bounded Context 7

> **Source of Truth:** Complete Specification for Immutable Audit Logging & Notification Use Cases (`UC-AUDIT-01`, `UC-AUDIT-02`).
> **Orchestrated by:** [`../MVP.md`](../MVP.md)

---

### Use Case Specification: `UC-AUDIT-01`
- **Use Case Name:** Immutable Audit Trail Logging
- **Stereotype:** Base / Included Use Case
- **Actor(s):** Audit Subsystem (`SYS-04`) (primary), Legal / Compliance Auditor (secondary)
- **Includes:** None
- **Extends / Extended By:** None (Included by `UC-IAM-01`, `UC-DOC-01`, `UC-DOC-03`, `UC-HITL-02`, etc.)
- **Summary Description:** Automatically records every security-sensitive action, data mutation, and RAG access into an append-only, immutable database audit table with client IP, user agent, timestamps, and before/after details.
- **Priority:** Must Have
- **Status:** Complete Specification
- **Pre-Condition:**
  1. A security, CRUD, or workflow event is triggered in the system.
- **Post-Condition(s):**
  1. An immutable record is appended into `audit_logs`.
  2. Log data is queryable by authorized compliance auditors.
- **Basic Path:**
  1. Application interceptor or domain event listener captures user action.
  2. Subsystem extracts `user_id`, `action`, `resource_type`, `resource_id`, `ip_address`, `user_agent`, and `details` JSONB.
  3. Subsystem inserts row into `audit_logs` table.
  4. Auditor queries `GET /api/v1/audit-logs` with filters (date range, user, action).
  5. System returns audit trail data.
- **Alternative Paths:**
  - 1a. Action performed by internal background worker: `user_id` is recorded as NULL or system bot ID.
- **Business Rules (Audit Immutability Invariant):**
  - B1: The `audit_logs` table is strictly APPEND-ONLY. No `UPDATE` or `DELETE` operations or API endpoints are permitted.
  - B2: Database roles assigned to application backends must not have `DROP` or `TRUNCATE` permissions on `audit_logs`.
- **Non-Functional Requirements:**
  - NF1: Audit logging must execute asynchronously with zero latency penalty on main user request threads.

---

### Use Case Specification: `UC-AUDIT-02`
- **Use Case Name:** Real-time In-App Notifications
- **Stereotype:** Base / Included Use Case
- **Actor(s):** End User (primary), Notification Subsystem (`SYS-05`) (secondary)
- **Includes:** None
- **Extends / Extended By:** None (Included by `UC-HITL-01`, `UC-HITL-02`)
- **Summary Description:** Delivers real-time notifications to users regarding document indexing completion, pending approval assignments, and task updates.
- **Priority:** Must Have
- **Status:** Complete Specification
- **Pre-Condition:**
  1. A notification event is emitted targeting a specific `user_id`.
- **Post-Condition(s):**
  1. Notification record inserted into `notifications` table (`is_read = FALSE`).
  2. User views and marks notifications as read.
- **Basic Path:**
  1. System generates notification (e.g., "Document X successfully indexed").
  2. Record stored in `notifications` table.
  3. User fetches unread notifications via `GET /api/v1/notifications`.
  4. User clicks "Mark as Read" on a notification.
  5. System updates `notifications.is_read = TRUE`.
- **Alternative Paths:**
  - 4a. User clicks "Mark All as Read": System updates all unread notifications for this user in one query.
- **Business Rules:**
  - B1: Notification types include `INFO`, `SUCCESS`, `WARNING`, and `ACTION_REQUIRED`.
- **Non-Functional Requirements:**
  - NF1: Notification delivery latency $< 500\text{ ms}$.
