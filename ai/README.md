# AI & RAG Microservice (Python)

## 1. Overview

The `ai` service is an independent microservice responsible for AI Assistant capabilities and the **RAG (Retrieval-Augmented Generation)** platform. Built with **Python 3.11+**, packaged with **`uv`**, and exposed via **FastAPI** on port `8000`.

The service strictly follows **Clean Architecture / Ports & Adapters (Hexagonal Architecture)** with two core pipelines:
- **Offline Ingestion Pipeline**: Chunking, batch embedding generation, and indexing into PostgreSQL (`pgvector` + `tsvector` Full-Text Search).
- **Online Hybrid Retrieval & Generation Pipeline**: Hybrid search combining dense semantic embeddings and sparse full-text search via Reciprocal Rank Fusion (RRF), source attribution, and grounded answer generation.

---

## 2. Hexagonal Architecture (Ports & Adapters)

```text
                        +-----------------------------------------------------------+
                        |                         API Layer                         |
                        |                 ai_controller, rag_controller             |
                        +-----------------------------+-----------------------------+
                                                      | calls
                                                      v
                        +-----------------------------------------------------------+
                        |                     APPLICATION Layer                     |
                        |      Use Cases: ChatUseCase, IngestionUseCase,            |
                        |                 RetrievalUseCase, RagUseCase              |
                        |      Services:  OfflineIngestionPipeline,                 |
                        |                 OnlineRetrievalPipeline, RagService,      |
                        |                 ChunkingService, ChatService              |
                        |      Ports Out: EmbeddingPort, VectorStorePort,           |
                        |                 LlmClientPort, RerankerPort               |
                        +----------------------+-------------+----------------------+
                         implements            |             | uses
                                               v             v
    +-----------------------------------------------+   +-------------------------------------------+
    |              INFRASTRUCTURE Layer             |   |               DOMAIN Layer                |
    |  PgVectorStore (PostgreSQL), ChromaVectorStore|   |  Document, Chunk, SearchResult,           |
    |  InMemoryVectorStore, OpenAiEmbeddingAdapter, |   |  RagQuery, RagResponse, Confidence,       |
    |  OpenAiClientAdapter, ReciprocalRankFusion    |   |  LlmMessage, LlmRole, Domain Exceptions   |
    +-----------------------------------------------+   +-------------------------------------------+
```

---

## 3. Directory Structure

```text
ai/
├── src/ai/
│   ├── api/                                    # Inbound Adapters: FastAPI HTTP Routers & Endpoints
│   │   ├── dto/                                # API Data Transfer Objects (Pydantic Models)
│   │   ├── ai_controller.py                    # LLM Chat Endpoints (/api/v1/ai)
│   │   └── rag_controller.py                   # RAG Endpoints (/api/v1/rag)
│   ├── application/
│   │   ├── port_in/                            # Inbound Ports (Use Case Interfaces)
│   │   │   ├── chat_use_case.py
│   │   │   ├── ingestion_use_case.py           # Ingestion Interface
│   │   │   ├── retrieval_use_case.py           # Retrieval Interface
│   │   │   └── rag_use_case.py                 # Unified RAG Port
│   │   ├── port_out/                           # Outbound Ports (SPI Interfaces)
│   │   │   ├── embedding_port.py               # Vector Embedding Port
│   │   │   ├── vector_store_port.py            # Vector Database Port
│   │   │   ├── reranker_port.py                # Reranking Port
│   │   │   └── llm_client_port.py              # LLM Generation Port
│   │   └── service/                            # Core Pipelines & Application Services
│   │       ├── chunking_service.py             # Fixed, Recursive, Markdown, Token Chunkers
│   │       ├── ingestion_pipeline.py           # Offline Ingestion Pipeline
│   │       ├── retrieval_pipeline.py           # Online Hybrid Retrieval Pipeline
│   │       ├── rag_service.py                  # Unified RAG Service
│   │       └── chat_service.py                 # Chat LLM Service
│   ├── domain/                                 # Pure Python Domain Entities
│   │   ├── exception/
│   │   │   └── exceptions.py                   # Domain Exceptions
│   │   └── model/
│   │       ├── document.py                     # Document, Chunk, SearchResult
│   │       ├── rag_query.py                    # RagQuery specification
│   │       ├── rag_response.py                 # Grounded RagResponse with citations
│   │       ├── assistant_response.py           # Assistant response model
│   │       ├── confidence.py                   # Confidence Enum (LOW, MEDIUM, HIGH)
│   │       ├── llm_message.py                  # LlmMessage entity
│   │       └── llm_role.py                     # LlmRole Enum (SYSTEM, USER, ASSISTANT)
│   ├── infrastructure/                         # Outbound Adapters & External Drivers
│   │   ├── client/
│   │   │   └── openai_client_adapter.py        # OpenAI Chat Adapter
│   │   ├── embedding/
│   │   │   ├── openai_embedding_adapter.py     # OpenAI Embeddings Adapter
│   │   │   └── mock_embedding_adapter.py       # Deterministic Local Embedder (testing)
│   │   ├── vector_store/
│   │   │   ├── pgvector_store.py               # PostgreSQL + pgvector + GIN tsvector
│   │   │   ├── chroma_vector_store.py          # ChromaDB Adapter
│   │   │   └── in_memory_vector_store.py       # NumPy In-Memory Store
│   │   ├── reranker/
│   │   │   └── rrf_reranker.py                 # Reciprocal Rank Fusion (RRF)
│   │   └── config/
│   │       ├── config.py                       # Configuration Loader (.env)
│   │       └── container.py                    # Dependency Injection Container
│   └── main.py                                 # Application Entry Point
├── examples/rag/                               # Educational RAG Cookbooks
│   ├── 01_naive_rag.py                         # Naive RAG baseline
│   ├── 02_chunking_strategies.py               # Chunking algorithms benchmark
│   ├── 03_embeddings_and_vector_search.py      # Vector search & ChromaDB operations
│   ├── 04_hybrid_search.py                     # BM25 + Dense Semantic + RRF Fusion
│   ├── 05_query_transformation_and_hyde.py     # Multi-Query Expansion & HyDE
│   ├── 06_reranking_and_compression.py         # Two-stage retrieval & Context Compression
│   ├── 07_rag_evaluation.py                    # RAG Triad evaluation metrics
│   └── 08_postgres_pgvector_pipeline.py        # PostgreSQL pgvector end-to-end pipeline
├── tests/                                      # Unit & Integration Tests
├── pyproject.toml                              # Project configuration & dependencies
├── .env.example                                # Local / default environment template
├── .env.dev.example                            # Development environment template
├── .env.prod.example                           # Production environment template
└── .env                                        # Active environment configuration
```

---

## 4. Environment Variables (`.env`)

Copy the example environment file for local development:

```bash
cp .env.example .env
```

### Key Configuration Settings

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

## 5. Getting Started

### Install Dependencies with `uv`
```bash
uv sync
```

### Run Tests and Linters
```bash
uv run pytest
uv run ruff check .
uv run ruff format --check .
```

### Run REST API Server
```bash
uv run python src/ai/main.py
```
Swagger UI is accessible at `http://localhost:8000/docs`.

### Run Cookbook Examples
```bash
uv run python examples/rag/08_postgres_pgvector_pipeline.py
```

---

## 6. REST API Endpoints

### 1. `POST /api/v1/rag/ingest`
Executes the **Offline Ingestion Pipeline**: splits text into chunks, generates batch vector embeddings, and persists them into the vector database.

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
Executes the **Online Hybrid Retrieval & Generation Pipeline**: performs hybrid search (Dense + FTS + RRF) and generates an answer with cited sources.

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
Performs raw vector similarity or hybrid search without LLM answer generation.

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

- Slice 1 — LLM Foundation [Completed]
- Slice 2 — Conversation State [Completed]
- Slice 3 — RAG & PostgreSQL pgvector Hybrid Pipeline [Completed]
- Slice 4 — Agentic Tool Calling [Planned]
