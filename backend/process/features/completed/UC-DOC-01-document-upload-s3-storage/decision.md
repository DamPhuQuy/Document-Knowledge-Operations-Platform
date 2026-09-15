# Decision: UC-DOC-01 Document Upload Architecture & Storage Simulation

<technical_decision task_id="UC-DOC-01" dec_id="DEC-DOC-01" version="1.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. Present options. PAIR: engineer decides. DELEGATED: agent auto-selects optimal recommendation and advances. -->
<decision_status>
  <phase>INNOVATE</phase>
  <mode>READ-ONLY</mode>
  <decision_owner>@engineer</decision_owner>
  <last_updated>2026-09-14</last_updated>
</decision_status>

---

## 1. Context

<context>
  <task>[UC-DOC-01](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/process/features/active/UC-DOC-01-document-upload-s3-storage/task.md)</task>
  <research_artifact>[research.md](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/process/features/active/UC-DOC-01-document-upload-s3-storage/research.md)</research_artifact>
  <constraints>
    - B1: Binary BLOBs are strictly prohibited in PostgreSQL. Only metadata, storage bucket, and key pointers are stored.
    - B2: Default access level is `INTERNAL` scoped to uploader department.
    - B3: Checksum SHA-256 must be calculated and stored to guarantee binary integrity.
    - NF1: Upload response time <= 2000ms for 10MB payload; max payload 50MB.
    - NF2: Streaming upload and SHA-256 computation without buffering entire 50MB in JVM heap.
    - Clean Architecture: Domain model must remain pure POJO; Object storage abstracted via outbound port `ObjectStoragePort`.
    - Local Development: Needs lightweight, reliable AWS Cloud simulation without requiring cloud accounts or expensive licenses.
  </constraints>
</context>

---

## 2. Decisions Required

<decision_question>
  1. [DEC-DOC-01] Storage Client & Local Cloud Emulation: What SDK and local emulation tool should be chosen to integrate S3 object storage seamlessly across local development, testing, and production?
  2. [DEC-DOC-02] Storage & Database Transaction Coordination: How should the S3 upload and PostgreSQL metadata insertion be coordinated to prevent orphan files and maintain consistency without distributed transactions?
  3. [DEC-DOC-03] Streaming SHA-256 Checksum Computation: How should the SHA-256 hash be computed to guarantee NF2 (zero JVM heap blowup) while streaming to S3?
</decision_question>

---

## 3. Options for DEC-DOC-01: Storage Client & Local Cloud Emulation

<options dec="DEC-DOC-01">

  <option id="A">
    <approach>
      Official AWS SDK v2 (`software.amazon.awssdk:s3:2.29.x`) paired with <strong>Floci</strong> (`floci/floci:latest`, port 4566) as local cloud emulator.
      - AWS SDK v2 provides native non-blocking & streaming `RequestBody.fromInputStream`.
      - Endpoint override (`endpointOverride: http://localhost:4566`) enables transparent switching between Floci locally and real AWS S3 in cloud.
      - Floci is ultra-lightweight (~90MB image, ~13MB RAM, ~24ms startup, MIT license, no auth tokens).
    </approach>
    <advantages>
      - Standard enterprise AWS SDK: direct upgrade path, native maintenance by AWS.
      - Floci starts instantly in Docker Compose / CI with negligible resource footprint.
      - Complete compatibility with standard S3 protocols and path-style addressing.
      - Outbound port `ObjectStoragePort` completely isolates AWS SDK from domain.
    </advantages>
    <disadvantages>
      - Floci is relatively newer in the community compared to LocalStack (though LocalStack community edition has become restricted).
    </disadvantages>
    <complexity>LOW</complexity>
    <compatibility>HIGH (Standard S3 API, zero vendor lock-in for MinIO/Floci/AWS)</compatibility>
    <concurrency_transaction_risk>LOW</concurrency_transaction_risk>
    <testability>HIGH (Can run unit tests with mock port, integration tests with Floci container)</testability>
    <maintainability>HIGH</maintainability>
  </option>

  <option id="B">
    <approach>
      MinIO Java SDK (`io.minio:minio:8.5.x`) paired with MinIO Server container.
      - Uses MinIO client library for all storage operations.
    </approach>
    <advantages>
      - MinIO server is mature and well-known.
    </advantages>
    <disadvantages>
      - MinIO client is a proprietary API distinct from standard AWS SDK v2.
      - Licensing: MinIO server is AGPL v3, which presents commercial compliance hurdles.
      - Harder to integrate standard AWS IAM roles / STS credentials when deploying to AWS EKS.
    </disadvantages>
    <complexity>MEDIUM</complexity>
    <compatibility>MEDIUM (Locked to MinIO SDK semantics)</compatibility>
    <concurrency_transaction_risk>LOW</concurrency_transaction_risk>
    <testability>MEDIUM</testability>
    <maintainability>MEDIUM</maintainability>
  </option>

  <option id="C">
    <approach>
      Spring Cloud AWS (`io.awspring.cloud:spring-cloud-aws-starter-s3`) paired with LocalStack.
      - Uses Spring Cloud starter abstractions over AWS S3.
    </approach>
    <advantages>
      - High Spring integration / autowiring convenience.
    </advantages>
    <disadvantages>
      - Spring Cloud version compatibility matrix with Spring Boot 3.3.x is fragile.
      - LocalStack image is massive (~1 GB), slow to start (~3.3s+), consumes high RAM (~150MB+), and requires auth tokens since March 2026.
    </disadvantages>
    <complexity>MEDIUM</complexity>
    <compatibility>MEDIUM</compatibility>
    <concurrency_transaction_risk>LOW</concurrency_transaction_risk>
    <testability>MEDIUM</testability>
    <maintainability>MEDIUM</maintainability>
  </option>

</options>

---

## 4. Options for DEC-DOC-02: Storage & Database Coordination

<options dec="DEC-DOC-02">

  <option id="A">
    <approach>
      <strong>S3 Upload First -> DB Transaction with Compensation:</strong>
      1. Stream upload binary directly to S3 under key `documents/{doc_id}/v1/{file_name}`.
      2. If S3 upload fails/times out -> Throw `StorageException` (mapped to HTTP 502 Bad Gateway). Database is never touched.
      3. If S3 succeeds -> Open `@Transactional` block to insert `documents` and `document_versions` rows and publish domain event `DocumentUploadedEvent`.
      4. If DB commit throws exception -> Catch exception, execute compensation call `s3Client.deleteObject(...)` to eliminate orphan file, then rethrow `DatabasePersistenceException` (HTTP 500).
    </approach>
    <advantages>
      - Keeps database transactions short: file network streaming (up to several seconds) occurs strictly outside DB connection lease.
      - Zero connection pool starvation under high concurrent uploads.
      - Clean compensation path for failed DB inserts.
    </advantages>
    <disadvantages>
      - In edge cases where backend process crashes between S3 upload and DB commit before compensation executes, a periodic S3 cleanup job (reconciliation) is good practice.
    </disadvantages>
    <complexity>LOW</complexity>
    <compatibility>HIGH</compatibility>
    <concurrency_transaction_risk>LOW (Zero DB lock contention during large network stream)</concurrency_transaction_risk>
    <testability>HIGH</testability>
    <maintainability>HIGH</maintainability>
  </option>

  <option id="B">
    <approach>
      <strong>DB Transaction First with Status PENDING_UPLOAD -> S3 Upload:</strong>
      1. Save document in DB with `processing_status = 'PENDING_UPLOAD'`, `is_s3_synced = false`.
      2. Upload to S3.
      3. Update status in DB to `UPLOADED` and `is_s3_synced = true`.
    </approach>
    <advantages>
      - Document metadata exists in DB even if upload fails.
    </advantages>
    <disadvantages>
      - Creates phantom records in DB if upload fails immediately.
      - Requires two round-trips / transactions to the database per upload.
      - Complex status machine needed just for initial creation.
    </disadvantages>
    <complexity>MEDIUM</complexity>
    <compatibility>MEDIUM</compatibility>
    <concurrency_transaction_risk>MEDIUM</concurrency_transaction_risk>
    <testability>MEDIUM</testability>
    <maintainability>MEDIUM</maintainability>
  </option>

</options>

---

## 5. Options for DEC-DOC-03: Streaming Checksum Computation

<options dec="DEC-DOC-03">

  <option id="A">
    <approach>
      <strong>Single-Pass DigestInputStream Pipeline:</strong>
      - Wrap incoming `MultipartFile.getInputStream()` in `java.security.DigestInputStream(is, MessageDigest.getInstance("SHA-256"))`.
      - Pass `DigestInputStream` directly to `RequestBody.fromInputStream(digestStream, contentLength)`.
      - As AWS SDK reads stream buffers (typically 8KB-64KB chunks) to send over HTTP to S3, `DigestInputStream` updates the SHA-256 state on-the-fly.
      - Upon completion of the S3 upload stream, extract `digestStream.getMessageDigest().digest()` and convert to hexadecimal string.
    </approach>
    <advantages>
      - Exactly single-pass I/O: zero temporary disk files, zero RAM buffering.
      - Maximum memory footprint per 50MB upload is limited to ~64KB buffer.
      - Fully complies with NF2.
    </advantages>
    <disadvantages>
      - Requires passing known `contentLength` to `RequestBody.fromInputStream`, which is readily available from `MultipartFile.getSize()`.
    </disadvantages>
    <complexity>LOW</complexity>
    <compatibility>HIGH</compatibility>
    <concurrency_transaction_risk>LOW</concurrency_transaction_risk>
    <testability>HIGH</testability>
    <maintainability>HIGH</maintainability>
  </option>

  <option id="B">
    <approach>
      <strong>Two-Pass Disk Buffer:</strong>
      - Save `MultipartFile` to temporary file on disk.
      - Pass 1: Compute SHA-256 checksum from file.
      - Pass 2: Upload file to S3.
      - Delete temporary file.
    </approach>
    <advantages>
      - Checksum is known before upload begins.
    </advantages>
    <disadvantages>
      - Doubles disk I/O latency; risk of filling up container ephemeral disk storage under high concurrent traffic.
    </disadvantages>
    <complexity>MEDIUM</complexity>
    <compatibility>HIGH</compatibility>
    <concurrency_transaction_risk>LOW</concurrency_transaction_risk>
    <testability>MEDIUM</testability>
    <maintainability>MEDIUM</maintainability>
  </option>

</options>

---

## 6. Trade-off Matrix

<tradeoff_matrix>

### Storage Client & Local Cloud Emulation (DEC-DOC-01)
| Criterion | Option A (AWS SDK v2 + Floci) | Option B (MinIO SDK + MinIO) | Option C (Spring Cloud AWS + LocalStack) |
|---|:---:|:---:|:---:|
| Compatibility | 5 | 3 | 4 |
| Performance / Footprint | 5 (Floci ~13MB RAM) | 4 (MinIO ~100MB RAM) | 2 (LocalStack ~150MB+ RAM, 1GB image) |
| Enterprise Readiness | 5 | 3 | 4 |
| Testability | 5 | 4 | 4 |
| Maintainability | 5 | 3 | 3 |
| **Total Score** | **25 / 25** | **17 / 25** | **17 / 25** |

### Storage & DB Coordination (DEC-DOC-02)
| Criterion | Option A (Upload First + S3 Compensation) | Option B (Two-Phase DB Status) |
|---|:---:|:---:|
| DB Connection Efficiency | 5 (No DB lock during upload) | 3 (Multiple DB roundtrips) |
| Data Consistency | 4.5 | 4 |
| Implementation Simplicity | 5 | 3.5 |
| Resilience & Error Handling | 5 | 4 |
| **Total Score** | **19.5 / 20** | **14.5 / 20** |

### Streaming SHA-256 (DEC-DOC-03)
| Criterion | Option A (Single-Pass DigestInputStream) | Option B (Two-Pass Disk Buffer) |
|---|:---:|:---:|
| Memory Efficiency (NF2) | 5 (64KB heap) | 4 (Disk-bound) |
| I/O Latency | 5 (Single pass) | 2.5 (Double I/O write+read) |
| Disk Safety | 5 (No disk exhaustion) | 3 (Container ephemeral disk risk) |
| **Total Score** | **15 / 15** | **9.5 / 15** |

<!-- Score: 1 (poor) → 5 (excellent) -->
</tradeoff_matrix>

---

## 7. Recommendation

<recommendation>
  - **DEC-DOC-01:** Adopt **Option A (AWS SDK v2 + Floci emulator)**. AWS SDK v2 is the production standard, and Floci provides a lightning-fast (~24ms startup, ~13MB RAM), free, and drop-in compatible AWS S3 emulator for local Docker Compose and test environments.
  - **DEC-DOC-02:** Adopt **Option A (S3 Upload First with S3 Compensation)**. This completely decouples long-running network uploads from database connection acquisition, prevents connection pool exhaustion, and reliably deletes S3 files if PostgreSQL metadata commit fails.
  - **DEC-DOC-03:** Adopt **Option A (Single-Pass DigestInputStream)**. Computes the SHA-256 hash on-the-fly as the stream is ingested by AWS SDK, meeting Non-Functional Requirement NF2 with minimal heap overhead (< 64KB).
</recommendation>

---

## 8. Implementation Decision

<!-- PAIR mode: Completed by engineer before Gate 1 passes.
     DELEGATED / Fast-Track mode: Agent automatically populates Recommendation
     into <selected_option>, documents rationale, signs Gate 1 with [AUTO: DELEGATED], and proceeds. -->
<engineer_decision>
  <selected_option>
    DEC-DOC-01: Option A (AWS SDK v2 + Floci emulator)
    DEC-DOC-02: Option A (S3 Upload First + DB Transaction with S3 Compensation)
    DEC-DOC-03: Option A (Single-Pass DigestInputStream)
  </selected_option>
  <rationale>
    Provides the highest performance, minimal resource usage, zero heap explosion on 50MB files, and optimal developer experience with Floci emulator for local development and CI.
  </rationale>
  <rejected_alternatives>
    - DEC-DOC-01 Option B: MinIO SDK has AGPL compliance and proprietary API concerns.
    - DEC-DOC-01 Option C: LocalStack is too heavy (1GB image, 3.3s boot) and has closed licensing.
    - DEC-DOC-02 Option B: Two-phase DB creation leaves phantom records and starves DB pools.
    - DEC-DOC-03 Option B: Writing to disk introduces unnecessary disk I/O latency and container disk filling risk.
  </rejected_alternatives>
</engineer_decision>

---

## 9. Constraints Created by This Decision

<constraints_created>
  - Must add `software.amazon.awssdk:s3:2.29.52` (or latest stable 2.29.x) to `backend/build.gradle`.
  - Must configure `S3StorageProperties` with `endpoint`, `region`, `bucket-name`, `access-key-id`, `secret-access-key`, and `path-style-access: true` for Floci support.
  - Must configure `spring.servlet.multipart.max-file-size: 50MB` and `spring.servlet.multipart.max-request-size: 55MB` in `application.yaml`.
  - Outbound port `ObjectStoragePort` must expose `upload(storageKey, inputStream, contentLength, contentType)` and `delete(storageKey)`.
  - Domain service must call `objectStoragePort.delete(storageKey)` inside a catch block if `documentRepositoryPort.save(...)` fails.
  - Domain models (`Document`, `DocumentVersion`) and DTOs will use manual mapping (static factory / builder) without MapStruct per user preference.
</constraints_created>

---

## 10. Evidence Still Required

<evidence_required>
  - Verify Floci docker service can be started and bucket `doc-knowledge-storage` created.
  - Verify AWS SDK v2 compatibility with Floci using `pathStyleAccessEnabled(true)`.
  - Verify `DigestInputStream` generates correct hex SHA-256 identical to standard SHA-256 tools.
</evidence_required>

---

## Gate 1 — Decision Approved

<gate id="G1">
  - [x] All options presented and trade-offs analyzed.
  - [x] Selected option recorded above.
  - [x] No blocking business / schema / security decision remains open.
  <approved_by>@engineer</approved_by>  <!-- Engineer name (PAIR) or [AUTO: DELEGATED] (DELEGATED/Fast-Track) -->
  <approved_date>2026-09-14</approved_date>
</gate>

</technical_decision>
