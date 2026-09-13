# Use Case Specifications: Document Management (`Document_Management`)
## Bounded Context 2

> **Source of Truth:** Complete Specification for Document Management Use Cases (`UC-DOC-01`, `UC-DOC-02`, `UC-DOC-03`, `UC-DOC-04`).
> **Orchestrated by:** [`../MVP.md`](../MVP.md)

---

### Use Case Specification: `UC-DOC-01`
- **Use Case Name:** Document Upload & S3 Object Storage
- **Stereotype:** Base Use Case
- **Actor(s):** Knowledge Worker / Staff (`ROLE_STAFF`) (primary), S3 Storage (`EXT-01`) (secondary)
- **Includes:** `UC-AUDIT-01` (Immutable Audit Trail Logging)
- **Extended By:** `UC-DOC-02` (Manage Document Versioning) at Extension Point `Existing Document Revision`
- **Summary Description:** Uploads raw document files (PDF, DOCX, TXT, XLSX), stores binaries on AWS S3, computes SHA-256 integrity checksums, creates Version 1 metadata in PostgreSQL, and records an immutable audit log.
- **Priority:** Must Have (P0)
- **Status:** Complete Specification
- **Pre-Condition:**
  1. User is authenticated with `write:documents` permission.
  2. Object Storage (AWS S3) is accessible.
- **Post-Condition(s):**
  1. Binary file is saved in S3 at `documents/{doc_id}/v1/{file_name}`.
  2. Record created in `documents` with `current_version = 1` and `processing_status = 'COMPLETED'`.
  3. Version 1 record created in `document_versions`.
  4. Audit log recorded via `UC-AUDIT-01`.
- **Basic Path:**
  1. User selects a local file and inputs title, description, and security access level.
  2. Client submits a `multipart/form-data` request to `POST /api/v1/documents`.
  3. Backend validates file type, MIME type, and file size ($\le 50\text{ MB}$).
  4. Backend computes the SHA-256 checksum of the incoming stream.
  5. Backend uploads the binary stream to S3 Object Storage (`EXT-01`).
  6. Backend inserts a new row into the `documents` table.
  7. Backend inserts a new row into `document_versions` table referencing Version 1.
  8. Backend invokes `UC-AUDIT-01` to record `UPLOAD_DOC` event in `audit_logs`.
  9. Backend returns HTTP 201 Created with document metadata.
- **Alternative Paths:**
  - 3a. Unsupported file extension: System returns HTTP 415 Unsupported Media Type.
  - 3b. File size exceeds 50MB: System returns HTTP 413 Payload Too Large.
  - 5a. S3 upload failure/timeout: Transaction rolls back, temporary file deleted, returns HTTP 502 Bad Gateway.
- **Business Rules:**
  - B1: Binary BLOBs are strictly prohibited in PostgreSQL; only S3 storage pointers are stored.
  - B2: Default access level is `INTERNAL` scoped to the uploader's department.
  - B3: Checksum SHA-256 must be verified to prevent corrupted uploads.
- **Non-Functional Requirements:**
  - NF1: Upload processing overhead (excluding network transfer) $< 500\text{ ms}$.
  - NF2: SHA-256 hash must be computed in a streaming fashion without loading entire large files into JVM heap.

---

### Use Case Specification: `UC-DOC-02`
- **Use Case Name:** Manage Document Versioning
- **Stereotype:** Extension Use Case
- **Actor(s):** Document Owner / Manager (primary), S3 Storage (`EXT-01`) (secondary)
- **Extends:** `UC-DOC-01` (Document Upload & S3 Object Storage)
- **Extension Point:** `Existing Document Revision Upload`
- **Condition:** Executed when the user uploads a replacement revision for an existing document record rather than creating a new document.
- **Includes:** `UC-AUDIT-01` (Immutable Audit Trail Logging)
- **Summary Description:** Allows authors to upload updated revisions of an existing document, creating immutable historical snapshots in `document_versions` while updating the active pointer in `documents`.
- **Priority:** Must Have
- **Status:** Complete Specification
- **Pre-Condition:**
  1. User has `write:documents` permission and owns the document or has `EDIT` ACL.
  2. Parent document exists and is not soft-deleted.
- **Post-Condition(s):**
  1. New version record created with `version_number = current_version + 1`.
  2. `documents.current_version` points to the new version.
  3. Historical version records remain intact and immutable.
- **Basic Path:**
  1. User selects "Upload New Version" on the document details page.
  2. User provides new file and change summary note (`change_summary`).
  3. Backend verifies user's edit permissions on the document.
  4. Backend uploads new file to S3 under `documents/{doc_id}/v{next_version}/{file_name}`.
  5. Backend creates a new record in `document_versions`.
  6. Backend updates `documents.current_version`, `storage_key`, `checksum_sha256`, and sets `processing_status = 'PENDING'`.
  7. Backend emits `DocumentVersionCreatedEvent` for re-indexing.
  8. Backend invokes `UC-AUDIT-01` to record version update in `audit_logs`.
  9. Returns HTTP 200 OK with new version details.
- **Alternative Paths:**
  - 3a. User lacks edit permission on this document: Returns HTTP 403 Forbidden.
- **Business Rules:**
  - B1: Historical versions in `document_versions` are immutable and cannot be overwritten.
  - B2: Chunks and embeddings are bound to specific `document_version_id` to prevent version mismatch.
- **Non-Functional Requirements:**
  - NF1: Version transition must be ACID-compliant with zero downtime for readers.

---

### Use Case Specification: `UC-DOC-03`
- **Use Case Name:** Configure Document Access Control Matrix
- **Stereotype:** Base Use Case
- **Actor(s):** Document Owner / Manager (primary), IAM Subsystem (secondary)
- **Includes:** `UC-AUDIT-01` (Immutable Audit Trail Logging)
- **Extends / Extended By:** None
- **Summary Description:** Configures the 4-tier security classification (`PUBLIC`, `INTERNAL`, `RESTRICTED`, `CONFIDENTIAL`) and explicit ACL entries for users, departments, and roles.
- **Priority:** Must Have
- **Status:** Complete Specification
- **Pre-Condition:**
  1. User is the document uploader, department manager, or system admin.
  2. Target document exists and is active.
- **Post-Condition(s):**
  1. `documents.access_level` is updated.
  2. Records in `document_user_access`, `document_department_access`, and/or `document_role_access` are inserted or removed.
  3. Pre-filtered RAG search immediately reflects updated visibility rules.
- **Basic Path:**
  1. Document owner opens Access Control settings modal.
  2. Owner sets security classification (`PUBLIC`, `INTERNAL`, `RESTRICTED`, or `CONFIDENTIAL`).
  3. For `CONFIDENTIAL` or granular access, owner adds explicit user, department, or role permissions (`VIEW`, `EDIT`, `ADMIN`).
  4. Owner submits access control configuration.
  5. Backend validates permissions and updates ACL tables in a single transaction.
  6. Backend invokes `UC-AUDIT-01` to log `UPDATE_ACL` in `audit_logs`.
  7. System returns HTTP 200 OK.
- **Alternative Paths:**
  - 1a. User is not owner and lacks `manage:permissions`: Returns HTTP 403 Forbidden.
- **Business Rules (Document Access Matrix):**
  - B1: `PUBLIC` is readable by all authenticated users.
  - B2: `INTERNAL` is readable only if `user.is_internal = TRUE`.
  - B3: `RESTRICTED` is readable only if `user.department_id = document.department_id`.
  - B4: `CONFIDENTIAL` requires explicit ACL in `document_user_access`, `document_department_access`, or `document_role_access`, or uploader ownership.
- **Non-Functional Requirements:**
  - NF1: ACL updates must take immediate effect across all AI search queries without cache delay.

---

### Use Case Specification: `UC-DOC-04`
- **Use Case Name:** Document Soft Deletion
- **Stereotype:** Base Use Case
- **Actor(s):** Document Owner / Admin (primary)
- **Includes:** `UC-AUDIT-01` (Immutable Audit Trail Logging)
- **Extends / Extended By:** None
- **Summary Description:** Soft-deletes a document by populating `deleted_at`, instantly removing it from search results and RAG retrieval pipelines while preserving audit integrity.
- **Priority:** Must Have
- **Status:** Complete Specification
- **Pre-Condition:**
  1. User has `delete:documents` permission or owns the document.
- **Post-Condition(s):**
  1. `documents.deleted_at` timestamp is set to `CURRENT_TIMESTAMP`.
  2. Document is hidden from standard API listings and excluded from vector retrieval.
- **Basic Path:**
  1. User clicks "Delete Document" and confirms action.
  2. Backend sets `deleted_at = CURRENT_TIMESTAMP` in `documents`.
  3. Backend invokes `UC-AUDIT-01` to record `DELETE_DOC` in `audit_logs`.
  4. Returns HTTP 204 No Content.
- **Alternative Paths:**
  - 1a. Document already deleted: Returns HTTP 404 Not Found.
- **Business Rules:**
  - B1: Physical database records and S3 files are retained for compliance retention periods.
  - B2: All SQL queries and RAG retrieval queries must enforce `WHERE deleted_at IS NULL`.
- **Non-Functional Requirements:**
  - NF1: Deletion exclusion in queries must use index filter `WHERE deleted_at IS NULL` to ensure zero performance degradation.
