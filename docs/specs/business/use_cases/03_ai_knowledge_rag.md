# Use Case Specifications: AI Knowledge & Hybrid RAG (`AI_Knowledge_RAG`)
## Bounded Context 3

> **Source of Truth:** Complete Specification for AI Ingestion & Hybrid RAG Retrieval Use Cases (`UC-RAG-01`, `UC-RAG-02`, `UC-RAG-03`).
> **Orchestrated by:** [`../MVP.md`](../MVP.md)

---

### Use Case Specification: `UC-RAG-01`
- **Use Case Name:** Document Ingestion & Vector Indexing
- **Stereotype:** Base / Included Use Case
- **Actor(s):** AI Ingestion Worker (`SYS-01`) (primary), S3 (`EXT-01`) & Embedding Provider (`EXT-02`) (secondary)
- **Includes:** None
- **Extends / Extended By:** None (Included by `UC-WF-01`)
- **Summary Description:** Asynchronously extracts textual content from uploaded document versions, partitions content into recursive semantic chunks with page metadata, computes 1536-dimensional embeddings, and writes HNSW vector and GIN tsvector indexes into PostgreSQL.
- **Priority:** Must Have
- **Status:** Complete Specification
- **Pre-Condition:**
  1. Document version exists in `PENDING` state with binary file available on S3.
  2. AI Microservice (FastAPI) is healthy and connected to embedding provider.
- **Post-Condition(s):**
  1. Extracted chunks are stored in `document_chunks` with `embedding` and `tsv` data.
  2. Document status transitions to `INDEXED` (or `FAILED` upon error).
- **Basic Path:**
  1. Ingestion Worker picks up pending ingestion job.
  2. Worker downloads document binary from S3 (`EXT-01`).
  3. Worker extracts raw text and structural metadata (page numbers, section headers).
  4. Worker splits text using recursive character chunking (`chunk_size = 500-1000 tokens`, `overlap = 50-100 tokens`).
  5. Worker generates 1536-dimensional dense vector embeddings via Embedding API (`EXT-02`).
  6. Worker inserts chunk records into `document_chunks` with `embedding` and `to_tsvector('simple', content)`.
  7. Worker updates `documents.processing_status = 'INDEXED'`.
  8. Worker logs completion metrics (chunk count, duration, token usage).
- **Alternative Paths:**
  - 3a. Corrupted or encrypted PDF: Extraction fails, `documents.processing_status` set to `FAILED`, error recorded in `operation_tasks`.
  - 5a. Embedding API timeout/rate limit: Worker retries with exponential backoff up to 3 times before failing gracefully.
- **Business Rules:**
  - B1: Chunk tokens must not exceed embedding model context limits.
  - B2: Every chunk must store its source `page_number` for citation verification.
- **Non-Functional Requirements:**
  - NF1: 20-page standard PDF document ingestion must complete within $< 5\text{ seconds}$.
  - NF2: Vector index must use `hnsw` with `vector_cosine_ops`.

---

### Use Case Specification: `UC-RAG-02`
- **Use Case Name:** Pre-filtered Hybrid RRF Search
- **Stereotype:** Base / Included Use Case
- **Actor(s):** Hybrid RAG Engine (`SYS-02`) (primary), Embedding API (`EXT-02`) (secondary)
- **Includes:** None
- **Extended By:** `UC-RAG-03` (Anti-Hallucination Safe Abstention) at Extension Point `Zero Accessible Knowledge Found`
- **Summary Description:** Executes unified hybrid retrieval combining Dense Vector Search (HNSW Cosine) and Sparse Lexical Search (BM25/FTS) with strict SQL-level pre-filtering against user security context, merging rankings via Reciprocal Rank Fusion (RRF).
- **Priority:** Must Have
- **Status:** Complete Specification
- **Pre-Condition:**
  1. Incoming search request contains valid `query` and authenticated `user_context` (`user_id`, `department_id`, `role_ids`, `is_internal`).
  2. Chunks exist in `document_chunks` with valid embeddings and `tsv` vectors.
- **Post-Condition(s):**
  1. Returns Top-K relevant chunks strictly belonging to documents the user is authorized to read.
  2. Zero data leakage across department or security classification boundaries.
- **Basic Path:**
  1. Search Engine receives query string and user security context.
  2. Search Engine generates query embedding vector via Embedding API (`EXT-02`).
  3. Search Engine executes single SQL query combining vector cosine distance (`<=>`), full-text search (`tsv @@ plainto_tsquery`), and ACL `WHERE` clauses.
  4. Search Engine computes Reciprocal Rank Fusion (RRF) score:
     $$\text{RRF}(d) = \frac{1}{60 + \text{Rank}_{\text{dense}}(d)} + \frac{1}{60 + \text{Rank}_{\text{sparse}}(d)}$$
  5. Search Engine sorts candidate chunks by RRF score and takes Top-K ($K = 5$).
  6. Returns structured chunk results with content, document ID, title, page number, and similarity score.
- **Alternative Paths:**
  - 3a. User has access to 0 matching documents or similarity $< 0.50$: Activates `UC-RAG-03` extension.
- **Business Rules (Pre-filtering Guarantee):**
  - B1: Pre-filtering is mandatory at the SQL layer; Post-filtering in application code is strictly forbidden.
  - B2: Documents marked `deleted_at IS NOT NULL` are excluded unconditionally.
- **Non-Functional Requirements:**
  - NF1: Query execution latency across 100,000 chunks must be $< 25\text{ ms}$.
  - NF2: Zero data leakage rate: $100\%$ precision in access barrier enforcement.

---

### Use Case Specification: `UC-RAG-03`
- **Use Case Name:** Anti-Hallucination Safe Abstention
- **Stereotype:** Extension Use Case
- **Actor(s):** Hybrid RAG Engine (`SYS-02`) (primary)
- **Extends:** `UC-RAG-02` (Pre-filtered Hybrid RRF Search)
- **Extension Point:** `Zero Accessible Knowledge Found`
- **Condition:** Executed when Pre-filtered retrieval returns 0 accessible chunks or top chunk cosine similarity is $< 0.50$.
- **Includes:** None
- **Summary Description:** Intercepts out-of-scope or unauthorized queries when zero relevant accessible chunks are retrieved, returning a standardized abstention message rather than generating ungrounded responses.
- **Priority:** Must Have
- **Status:** Complete Specification
- **Pre-Condition:**
  1. Pre-filtered retrieval returns 0 chunks or maximum similarity score $< 0.50$.
- **Post-Condition(s):**
  1. LLM text generation is bypassed to save token cost and eliminate hallucination.
  2. User receives standard refusal response code `NO_ACCESSIBLE_KNOWLEDGE`.
- **Basic Path:**
  1. Retrieval pipeline evaluates candidate chunks from `UC-RAG-02`.
  2. Pipeline determines retrieved chunk list is empty or relevance threshold is not met.
  3. Pipeline activates Abstention Guard.
  4. System formats standardized response: *"The system cannot find accessible documents within your permissions to answer this query."*
  5. System returns HTTP 200 with structured abstention payload.
- **Alternative Paths:**
  - 1a. Relevant chunks found: Pipeline continues normal flow.
- **Business Rules:**
  - B1: System must never invent information when evidence is absent.
- **Non-Functional Requirements:**
  - NF1: Abstention decision latency $< 30\text{ ms}$ (no LLM inference cost incurred).
