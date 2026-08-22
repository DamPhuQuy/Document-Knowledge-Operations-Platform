import chromadb

from ai.domain.model.chunk import Chunk
from ai.infrastructure.embedding.mock_embedding_adapter import MockEmbeddingAdapter
from ai.infrastructure.vector_store.chroma_vector_store import ChromaVectorStore
from ai.infrastructure.vector_store.in_memory_vector_store import InMemoryVectorStore


def test_mock_embedding_adapter() -> None:
    embedder = MockEmbeddingAdapter(dimension=64)
    assert embedder.dimension == 64

    v1 = embedder.embed_query("machine learning")
    v2 = embedder.embed_query("machine learning")
    v3 = embedder.embed_query("cyber security")

    assert len(v1) == 64
    assert v1 == v2
    assert v1 != v3


def test_in_memory_vector_store() -> None:
    embedder = MockEmbeddingAdapter(dimension=32)
    store = InMemoryVectorStore()

    c1 = Chunk(
        id="c1",
        document_id="d1",
        content="FastAPI REST API with Python",
        embedding=embedder.embed_query("FastAPI REST API with Python"),
        metadata={"tag": "python"},
    )
    c2 = Chunk(
        id="c2",
        document_id="d2",
        content="Spring Boot Microservice in Java",
        embedding=embedder.embed_query("Spring Boot Microservice in Java"),
        metadata={"tag": "java"},
    )

    store.add_chunks([c1, c2])
    assert store.count() == 2

    # Similarity search
    q_vec = embedder.embed_query("FastAPI web framework")
    res = store.similarity_search(q_vec, top_k=2, filter_metadata={"tag": "python"})
    assert len(res) == 1
    assert res[0].chunk.id == "c1"

    # Full text search
    fts_res = store.full_text_search("Spring Boot", top_k=2)
    assert len(fts_res) == 1
    assert fts_res[0].chunk.id == "c2"

    # Hybrid search
    hybrid_res = store.hybrid_search("FastAPI", q_vec, top_k=2)
    assert len(hybrid_res) >= 1
    assert hybrid_res[0].chunk.id == "c1"

    store.delete(["c1"])
    assert store.count() == 1

    store.clear()
    assert store.count() == 0


def test_chroma_vector_store() -> None:
    embedder = MockEmbeddingAdapter(dimension=32)
    ephemeral_client = chromadb.EphemeralClient()
    store = ChromaVectorStore(
        collection_name="test_collection",
        client=ephemeral_client,
    )

    c1 = Chunk(
        id="ch1",
        document_id="doc_a",
        content="Kubernetes pod deployment guide",
        embedding=embedder.embed_query("Kubernetes pod deployment guide"),
        metadata={"tier": "infra"},
    )
    c2 = Chunk(
        id="ch2",
        document_id="doc_b",
        content="PostgreSQL database cluster setup",
        embedding=embedder.embed_query("PostgreSQL database cluster setup"),
        metadata={"tier": "db"},
    )

    store.add_chunks([c1, c2])
    assert store.count() == 2

    q_vec = embedder.embed_query("Kubernetes cluster pods")
    results = store.similarity_search(q_vec, top_k=2)
    assert len(results) == 2
    assert results[0].score >= 0.0

    store.delete(["ch1"])
    assert store.count() == 1

    store.clear()
    assert store.count() == 0
