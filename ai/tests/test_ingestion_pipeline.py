from ai.application.service.chunking_service import ChunkingService
from ai.application.service.ingestion_pipeline import OfflineIngestionPipeline
from ai.domain.model.document import Document
from ai.infrastructure.embedding.mock_embedding_adapter import MockEmbeddingAdapter
from ai.infrastructure.vector_store.in_memory_vector_store import InMemoryVectorStore


def test_offline_ingestion_pipeline() -> None:
    embedder = MockEmbeddingAdapter(dimension=32)
    store = InMemoryVectorStore()
    chunker = ChunkingService(default_chunk_size=100, default_chunk_overlap=20)

    pipeline = OfflineIngestionPipeline(
        embedding_port=embedder,
        vector_store_port=store,
        chunking_service=chunker,
        batch_size=2,
    )

    docs = [
        Document(
            id="doc1",
            content="This is the first document about PostgreSQL and pgvector database indexing.",
            metadata={"source": "db_manual"},
        ),
        Document(
            id="doc2",
            content="This is the second document about FastAPI REST API endpoints and clean architecture.",
            metadata={"source": "api_manual"},
        ),
    ]

    result = pipeline.run_ingestion(docs, strategy="recursive")

    assert result.total_documents == 2
    assert result.total_chunks >= 2
    assert len(result.chunk_ids) == result.total_chunks
    assert store.count() == result.total_chunks
    assert result.processing_time_ms >= 0.0


def test_offline_ingestion_empty() -> None:
    embedder = MockEmbeddingAdapter(dimension=32)
    store = InMemoryVectorStore()

    pipeline = OfflineIngestionPipeline(
        embedding_port=embedder,
        vector_store_port=store,
    )

    result = pipeline.run_ingestion([])
    assert result.total_documents == 0
    assert result.total_chunks == 0
    assert store.count() == 0
