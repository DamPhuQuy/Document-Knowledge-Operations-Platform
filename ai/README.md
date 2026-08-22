# AI & RAG Microservice (Python)

## 1. Tổng quan

Dịch vụ độc lập `ai` chịu trách nhiệm cung cấp khả năng AI Assistant và nền tảng **RAG (Retrieval-Augmented Generation)** cho hệ thống. Dự án được triển khai bằng **Python 3.11+**, quản lý package qua **`uv`** và kết nối trực tiếp với backend thông qua giao thức **HTTP REST API** (FastAPI) trên cổng `8000`.

Dịch vụ được thiết kế nghiêm ngặt theo kiến trúc **Clean Architecture / Ports & Adapters (Hexagonal Architecture)** với:
- **Offline Ingestion Pipeline**: Chunking, batch embedding, indexing vào PostgreSQL + `pgvector` & `tsvector` Full-Text Search.
- **Online Hybrid Retrieval & Generation Pipeline**: Tìm kiếm kết hợp (Dense Semantic + Sparse FTS) qua Reciprocal Rank Fusion (RRF), trích dẫn nguồn có kiểm chứng và sinh câu trả lời không ảo giác (Zero Hallucination).

---

## 2. Kiến trúc Clean Architecture (Hexagonal)

```
                        ┌───────────────────────────────────────────────────────────┐
                        │                         API Layer                         │
                        │                 ai_controller, rag_controller             │
                        └─────────────────────────────┬─────────────────────────────┘
                                                      │ calls
                                                      ▼
                        ┌───────────────────────────────────────────────────────────┐
                        │                     APPLICATION Layer                     │
                        │      Use Cases: ChatUseCase, IngestionUseCase,            │
                        │                 RetrievalUseCase, RagUseCase              │
                        │      Services:  OfflineIngestionPipeline,                 │
                        │                 OnlineRetrievalPipeline, RagService,      │
                        │                 ChunkingService, ChatService              │
                        │      Ports Out: EmbeddingPort, VectorStorePort,           │
                        │                 LlmClientPort, RerankerPort               │
                        └──────────────────────┬─────────────┬──────────────────────┘
                         implements            │             │ uses
                                               ▼             ▼
    ┌───────────────────────────────────────────────┐   ┌───────────────────────────────────────────┐
    │              INFRASTRUCTURE Layer             │   │               DOMAIN Layer                │
    │  PgVectorStore (PostgreSQL), ChromaVectorStore│   │  Document, Chunk, SearchResult,           │
    │  InMemoryVectorStore, OpenAiEmbeddingAdapter, │   │  RagQuery, RagResponse, Confidence,       │
    │  OpenAiClientAdapter, ReciprocalRankFusion    │   │  LlmMessage, LlmRole, Domain Exceptions   │
    └───────────────────────────────────────────────┘   └───────────────────────────────────────────┘
```

---

## 3. Cấu trúc thư mục

```text
ai/
├── src/ai/
│   ├── api/                                    # Inbound Adapters: FastAPI HTTP Routers & Endpoints
│   │   ├── dto/                                # API Data Transfer Objects (Pydantic Models & Enums)
│   │   ├── ai_controller.py                    # LLM Chat Endpoints (/api/v1/ai)
│   │   └── rag_controller.py                   # RAG Endpoints (/api/v1/rag)
│   ├── application/
│   │   ├── port_in/                            # Inbound Ports (Interfaces Use Cases)
│   │   │   ├── chat_use_case.py
│   │   │   ├── ingestion_use_case.py           # Interface cho Offline Ingestion Pipeline
│   │   │   ├── retrieval_use_case.py           # Interface cho Online Hybrid Retrieval
│   │   │   └── rag_use_case.py                 # Unified RAG Facade Port
│   │   ├── port_out/                           # Outbound Ports (Interfaces SPI)
│   │   │   ├── embedding_port.py               # Vector Embedding Port
│   │   │   ├── vector_store_port.py            # Vector Database Port
│   │   │   ├── reranker_port.py                # Reranking Port
│   │   │   └── llm_client_port.py              # LLM Generation Port
│   │   └── service/                            # Core Pipelines & Application Services
│   │       ├── chunking_service.py             # Fixed, Recursive, Markdown, Token Chunkers
│   │       ├── ingestion_pipeline.py           # Offline Ingestion Pipeline Template
│   │       ├── retrieval_pipeline.py           # Online Hybrid Retrieval & Generation Pipeline
│   │       ├── rag_service.py                  # Unified RAG Facade Service
│   │       └── chat_service.py                 # Chat LLM Service
│   ├── domain/                                 # Pure Python Domain Entities (No Framework)
│   │   ├── exception/
│   │   │   └── exceptions.py                   # Standardized Domain Exceptions
│   │   └── model/
│   │       ├── document.py                     # Document, Chunk, SearchResult
│   │       ├── rag_query.py                    # RagQuery specification
│   │       ├── rag_response.py                 # Grounded RagResponse with citations
│   │       ├── assistant_response.py           # Assistant response DTO
│   │       ├── confidence.py                   # Confidence Enum (LOW, MEDIUM, HIGH)
│   │       ├── llm_message.py                  # LlmMessage entity
│   │       └── llm_role.py                     # LlmRole Enum (SYSTEM, USER, ASSISTANT)
│   ├── infrastructure/                         # Outbound Adapters & Framework Integrations
│   │   ├── client/
│   │   │   └── openai_client_adapter.py        # OpenAI Chat Completions Adapter
│   │   ├── embedding/
│   │   │   ├── openai_embedding_adapter.py     # OpenAI Embeddings Adapter (text-embedding-3-small)
│   │   │   └── mock_embedding_adapter.py       # Deterministic Local Hash Embedder (offline/testing)
│   │   ├── vector_store/
│   │   │   ├── pgvector_store.py               # PostgreSQL + pgvector + GIN tsvector Hybrid Store
│   │   │   ├── chroma_vector_store.py          # ChromaDB Adapter
│   │   │   └── in_memory_vector_store.py       # NumPy In-Memory Vector Store
│   │   ├── reranker/
│   │   │   └── rrf_reranker.py                 # Reciprocal Rank Fusion (RRF) Reranker
│   │   └── config/
│   │       ├── config.py                       # Configuration Loader (.env)
│   │       └── container.py                    # Dependency Injection Container (dependency-injector)
│   └── main.py                                 # Điểm khởi chạy REST API Server
├── examples/rag/                               # 📚 Interactive Educational RAG Cookbook
│   ├── 01_naive_rag.py                         # Naive RAG baseline from scratch
│   ├── 02_chunking_strategies.py               # Chunking algorithms benchmark & visualizer
│   ├── 03_embeddings_and_vector_search.py      # Vector mathematics & ChromaDB operations
│   ├── 04_hybrid_search.py                     # BM25 + Dense Semantic + RRF Fusion
│   ├── 05_query_transformation_and_hyde.py     # Multi-Query Expansion & HyDE
│   ├── 06_reranking_and_compression.py         # Two-stage retrieval & Context Compression
│   ├── 07_rag_evaluation.py                    # The RAG Triad (Context & Answer Relevance, Faithfulness)
│   └── 08_postgres_pgvector_pipeline.py        # PostgreSQL pgvector Offline & Online Pipelines
├── tests/                                      # Comprehensive Automated Unit & API Tests
├── pyproject.toml                              # Quản lý dependencies (uv) & linters
└── .env                                        # Biến môi trường
```

---

## 4. Cấu hình biến môi trường (`.env`)

```env
# LLM Settings
LLM_API_KEY=your-api-key
LLM_BASE_URL=https://api.openai.com/v1
LLM_MODEL=gpt-4o-mini
LLM_TEMPERATURE=0.2
AI_HTTP_PORT=8000

# Embedding Settings
EMBEDDING_MODEL=text-embedding-3-small
EMBEDDING_DIMENSIONS=1536

# PostgreSQL with pgvector Settings
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=knowledge_ops
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_RAG_TABLE=document_chunks

# RAG Defaults
DEFAULT_CHUNK_SIZE=500
DEFAULT_CHUNK_OVERLAP=50
RAG_TOP_K=4
```

---

## 5. Hướng dẫn cài đặt và khởi chạy

### Cài đặt dependencies với `uv`
```bash
uv sync
```

### Chạy Unit Tests & Validations
```bash
uv run pytest
uv run ruff check .
uv run ruff format --check .
```

### Chạy REST API Server
```bash
uv run python src/ai/main.py
```
Truy cập Swagger UI tại `http://localhost:8000/docs`.

### Chạy các Cookbook Examples
```bash
uv run python examples/rag/08_postgres_pgvector_pipeline.py
```

---

## 6. REST API Endpoints

### 1. `POST /api/v1/rag/ingest`
Thực thi **Offline Ingestion Pipeline**: chia nhỏ văn bản, tạo vector embedding theo lô và lưu trữ vào cơ sở dữ liệu vector.

**Request Body:**
```json
{
  "documents": [
    {
      "id": "doc_vpn",
      "content": "WireGuard VPN requires MFA verification every 12 hours. Download configuration from https://portal.internal/vpn.",
      "metadata": {"category": "it", "author": "Infra Team"}
    }
  ],
  "chunk_size": 500,
  "chunk_overlap": 50,
  "strategy": "recursive"
}
```

**Response Body (200 OK):**
```json
{
  "total_documents": 1,
  "total_chunks": 1,
  "chunk_ids": ["doc_vpn_rec_0"],
  "processing_time_ms": 14.5
}
```

---

### 2. `POST /api/v1/rag/ask`
Thực thi **Online Hybrid Retrieval & Generation Pipeline**: tìm kiếm kết hợp (Dense + FTS + RRF) và sinh câu trả lời có kèm trích dẫn nguồn.

**Request Body:**
```json
{
  "query": "How often is MFA required for VPN access?",
  "top_k": 4,
  "use_hybrid": true,
  "temperature": 0.0
}
```

**Response Body (200 OK):**
```json
{
  "answer": "MFA verification is required every 12 hours when connecting via WireGuard VPN [Passage 1].",
  "confidence": "HIGH",
  "sources": [
    {
      "chunk_id": "doc_vpn_rec_0",
      "document_id": "doc_vpn",
      "content": "WireGuard VPN requires MFA verification every 12 hours...",
      "score": 0.9421,
      "metadata": {"category": "it", "author": "Infra Team"}
    }
  ],
  "prompt_tokens": 85,
  "completion_tokens": 24,
  "retrieved_count": 1
}
```

---

### 3. `POST /api/v1/rag/search`
Tìm kiếm vector tương đồng thô hoặc hybrid search không gọi LLM.

**Request Body:**
```json
{
  "query": "VPN access policy",
  "top_k": 3,
  "use_hybrid": true
}
```

---

## Roadmap

- Slice 1 — LLM Foundation ✅ (Clean Architecture REST API)
- Slice 2 — Conversation State ✅
- Slice 3 — RAG & PostgreSQL pgvector Hybrid Pipeline ✅
- Slice 4 — Agentic Tool Calling
