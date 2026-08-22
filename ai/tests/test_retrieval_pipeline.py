from unittest.mock import MagicMock

from ai.application.service.retrieval_pipeline import OnlineRetrievalPipeline
from ai.domain.model.assistant_response import AssistantResponse
from ai.domain.model.chunk import Chunk
from ai.domain.model.confidence import Confidence
from ai.domain.model.rag_query import RagQuery
from ai.infrastructure.embedding.mock_embedding_adapter import MockEmbeddingAdapter
from ai.infrastructure.vector_store.in_memory_vector_store import InMemoryVectorStore


def test_online_retrieval_pipeline() -> None:
    embedder = MockEmbeddingAdapter(dimension=32)
    store = InMemoryVectorStore()

    c1 = Chunk(
        id="c1",
        document_id="doc_vpn",
        content="WireGuard VPN requires MFA verification every 12 hours.",
        embedding=embedder.embed_query("WireGuard VPN requires MFA verification every 12 hours."),
        metadata={"source": "vpn_policy.md"},
    )
    store.add_chunks([c1])

    mock_llm = MagicMock()
    mock_llm.generateResponse.return_value = AssistantResponse(
        answer="WireGuard VPN requires MFA verification every 12 hours [Passage 1].",
        confidence=Confidence.HIGH,
        prompt_tokens=40,
        completion_tokens=15,
    )

    pipeline = OnlineRetrievalPipeline(
        embedding_port=embedder,
        vector_store_port=store,
        llm_client_port=mock_llm,
    )

    # Test retrieval
    results = pipeline.retrieve("How often is MFA required for VPN?", top_k=1, use_hybrid=True)
    assert len(results) == 1
    assert results[0].chunk.id == "c1"

    # Test generation
    response = pipeline.generate_answer(
        RagQuery(query="How often is MFA required for VPN?", use_hybrid=True)
    )
    assert response.confidence == Confidence.HIGH
    assert len(response.sources) == 1
    assert "MFA" in response.answer
    assert mock_llm.generateResponse.called


def test_online_retrieval_pipeline_empty() -> None:
    embedder = MockEmbeddingAdapter(dimension=32)
    store = InMemoryVectorStore()
    mock_llm = MagicMock()

    pipeline = OnlineRetrievalPipeline(
        embedding_port=embedder,
        vector_store_port=store,
        llm_client_port=mock_llm,
    )

    response = pipeline.generate_answer(RagQuery(query="Unknown question"))
    assert response.confidence == Confidence.LOW
    assert len(response.sources) == 0
    assert not mock_llm.generateResponse.called
