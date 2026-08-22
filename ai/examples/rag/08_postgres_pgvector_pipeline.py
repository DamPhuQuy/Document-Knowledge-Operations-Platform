"""
=============================================================================
RAG Template - Example 08: PostgreSQL (pgvector) Offline & Online Pipelines
=============================================================================
Architecture:
  1. Offline Ingestion Pipeline:
     - Document Processing -> Recursive Chunking -> Batch Embedding -> pgvector Upsert
     - Generates both dense embedding vector column + GIN full-text tsvector column.
  2. Online Hybrid Retrieval & Generation Pipeline:
     - Hybrid Search: Dense Vector Similarity (<=>) + Sparse Full-Text Search (ts_rank)
     - Reciprocal Rank Fusion (RRF) rank aggregation
     - Prompt Augmentation with Citation References [Passage 1], [Passage 2]
     - Grounded Answer Synthesis with LLM

Run:
  uv run python examples/rag/08_postgres_pgvector_pipeline.py
=============================================================================
"""

from ai.application.service.chunking_service import ChunkingService
from ai.application.service.ingestion_pipeline import OfflineIngestionPipeline
from ai.application.service.retrieval_pipeline import OnlineRetrievalPipeline
from ai.domain.model.document import Document
from ai.domain.model.rag_query import RagQuery
from ai.infrastructure.config.config import LlmConfig
from ai.infrastructure.embedding.mock_embedding_adapter import MockEmbeddingAdapter
from ai.infrastructure.vector_store.in_memory_vector_store import InMemoryVectorStore
from ai.infrastructure.vector_store.pgvector_store import PgVectorStore

# Sample Knowledge Documents
DOCUMENTS = [
    Document(
        id="doc_k8s_autoscaling",
        content=(
            "Kubernetes Horizontal Pod Autoscaler (HPA) automatically scales the number of pods "
            "based on observed CPU utilization or custom Prometheus metrics. "
            "KEDA (Kubernetes Event-driven Autoscaling) extends HPA to support external event sources "
            "such as Kafka topic lag, RabbitMQ queue depth, and AWS SQS queue lengths."
        ),
        metadata={"category": "infrastructure", "author": "DevOps Team"},
    ),
    Document(
        id="doc_postgres_pgvector",
        content=(
            "PostgreSQL with the pgvector extension enables storing and searching high-dimensional vectors. "
            "It supports exact and approximate nearest neighbor search using HNSW (Hierarchical Navigable Small World) "
            "and IVFFlat indexing algorithms. Combined with PostgreSQL full-text search (tsvector/GIN), "
            "pgvector provides native hybrid retrieval within a single transactional database."
        ),
        metadata={"category": "database", "author": "Data Engineering"},
    ),
    Document(
        id="doc_zero_downtime",
        content=(
            "Zero-downtime database migrations in PostgreSQL require avoiding long exclusive locks. "
            "Always create indexes concurrently using `CREATE INDEX CONCURRENTLY`. "
            "When adding columns with default values, use PostgreSQL 11+ instant default feature."
        ),
        metadata={"category": "database", "author": "DBA Team"},
    ),
]


def main() -> None:
    print("=" * 75)
    print("  RAG Pipeline Template: PostgreSQL + pgvector (Offline & Online Flow)")
    print("=" * 75)

    config = LlmConfig()
    embedder = MockEmbeddingAdapter(dimension=128)
    chunker = ChunkingService(default_chunk_size=200, default_chunk_overlap=30)

    # Initialize Vector Store: Try live PostgreSQL, fall back to In-Memory with Hybrid Search for local demos
    try:
        vector_store = PgVectorStore(config=config, auto_init_schema=True)
        # Test connection
        vector_store.count()
        print("\n[DB Status] Connected to PostgreSQL (pgvector) successfully.")
    except Exception as ex:
        print(f"\n[DB Status] PostgreSQL not reachable ({ex}). Using InMemory Hybrid Vector Store.")
        vector_store = InMemoryVectorStore()

    # =========================================================================
    # 1. OFFLINE INGESTION PIPELINE
    # =========================================================================
    print("\n--- 1. Executing Offline Ingestion Pipeline ---")
    ingestion_pipeline = OfflineIngestionPipeline(
        embedding_port=embedder,
        vector_store_port=vector_store,
        chunking_service=chunker,
        batch_size=10,
    )

    ingest_res = ingestion_pipeline.run_ingestion(
        documents=DOCUMENTS,
        strategy="recursive",
    )

    print(f"  * Total Ingested Documents: {ingest_res.total_documents}")
    print(f"  * Total Created Chunks:    {ingest_res.total_chunks}")
    print(f"  * Processing Time:         {ingest_res.processing_time_ms:.2f} ms")
    print(f"  * Total Chunks in DB:      {vector_store.count()}")

    # =========================================================================
    # 2. ONLINE HYBRID RETRIEVAL & GENERATION PIPELINE
    # =========================================================================
    print("\n--- 2. Executing Online Hybrid Retrieval & Generation Pipeline ---")

    # Mock or OpenAI LLM client adapter
    class DemoLlmClient:
        def generateResponse(self, messages, temperature=0.0):
            from ai.domain.model.assistant_response import AssistantResponse
            from ai.domain.model.confidence import Confidence

            return AssistantResponse(
                answer="KEDA enables event-driven autoscaling for Kubernetes by reading queue lengths [Passage 1].",
                confidence=Confidence.HIGH,
                prompt_tokens=65,
                completion_tokens=22,
            )

    retrieval_pipeline = OnlineRetrievalPipeline(
        embedding_port=embedder,
        vector_store_port=vector_store,
        llm_client_port=DemoLlmClient(),
    )

    user_query = "How does KEDA scale Kubernetes pods based on message queues?"
    print(f"  [User Query]: '{user_query}'")

    # Hybrid Retrieval (Dense Vector + Sparse FTS + RRF)
    retrieved_results = retrieval_pipeline.retrieve(
        query=user_query,
        top_k=2,
        use_hybrid=True,
    )

    print("\n  [Retrieved Hybrid Passages (Top-2)]:")
    for i, r in enumerate(retrieved_results, start=1):
        source = r.chunk.metadata.get("author", r.chunk.document_id)
        print(f"   [{i}] (Score: {r.score:.4f} | Source: {source})")
        print(f'       "{r.chunk.content}"')

    # Generate Grounded Answer
    response = retrieval_pipeline.generate_answer(
        RagQuery(query=user_query, top_k=2, use_hybrid=True)
    )

    print("\n  [Synthesized Answer]:")
    print(f"   {response.answer}")
    print(f"   Confidence: {response.confidence}")
    print(f"   Retrieved Passages: {len(response.sources)}")

    print("\n" + "=" * 75)


if __name__ == "__main__":
    main()
