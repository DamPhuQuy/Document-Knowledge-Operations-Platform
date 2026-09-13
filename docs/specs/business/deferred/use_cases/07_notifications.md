# Use Case Specifications: Real-time In-App Notifications (`Notification_System`)

> **Status:** Deferred (Post-MVP Fast-Follow Extension)
> **Relocated from:** `docs/specs/business/use_cases/07_audit_notifications.md`
> **Rationale:** Real-time push/in-app notifications and WebSocket/polling overhead are deferred to prioritize immediate cloud infrastructure deployment and core document S3 persistence.

---

### Use Case Specification: `UC-AUDIT-02`

- **Use Case Name:** Real-time In-App Notifications
- **Stereotype:** Base / Included Use Case
- **Actor(s):** End User (primary), Notification Subsystem (`SYS-05`) (secondary)
- **Includes:** None
- **Extends / Extended By:** None (Included by `UC-HITL-01`, `UC-HITL-02`)
- **Summary Description:** Delivers real-time notifications to users regarding document indexing completion, pending approval assignments, and task updates.
- **Priority:** Deferred (Phase 2 / Post-MVP)
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
