# AI & RAG Microservice (Python)

AI and Hybrid RAG microservice built with **Python 3.11+**, **FastAPI**, **`uv`**, and **PostgreSQL (pgvector + tsvector)**.

---

## 1. Quick Start

### A. Environment Activation & Dependencies
```bash
cd ai

# Option 1: Using uv (Recommended)
uv sync

# Option 2: Using active virtual environment
source .venv/bin/activate
pip install -e .
```

### B. Environment Variables
```bash
cp .env.example .env
```

Key `.env` variables:
```env
LLM_API_KEY=your-api-key
LLM_BASE_URL=https://api.openai.com/v1
LLM_MODEL=gpt-4o-mini
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=doc_knowledge_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
```

### C. Run Service
```bash
# Run FastAPI server
uv run python src/ai/main.py
# or with active .venv:
# python src/ai/main.py
```
- **API Base URL**: `http://localhost:8000`
- **Swagger Docs**: `http://localhost:8000/docs`

---

## 2. Testing & Quality Checks

```bash
# Run test suite
uv run pytest

# Lint and format checks
uv run ruff check .
uv run ruff format --check .
```

---

## 3. Core Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/rag/ingest` | Chunk, embed, and index documents into pgvector |
| `POST` | `/api/v1/rag/ask` | Hybrid retrieval (Dense + FTS + RRF) with cited answer |
| `POST` | `/api/v1/rag/search` | Raw similarity & hybrid retrieval |
| `GET` | `/health` | Service health status |

---

## 4. Project Structure

```text
ai/
├── src/ai/
│   ├── api/            # FastAPI controllers & DTOs
│   ├── application/    # Ingestion & Hybrid Retrieval use cases/services
│   ├── domain/         # Models (Document, Chunk, SearchResult, RagResponse)
│   ├── infrastructure/ # pgvector store, OpenAI clients, RRF reranker
│   └── main.py         # Entry point
├── tests/              # Unit and integration tests
├── pyproject.toml      # Package & dependency definitions
└── .env.example        # Environment configuration template
```
