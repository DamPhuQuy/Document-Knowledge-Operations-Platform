from unittest.mock import MagicMock

from fastapi.testclient import TestClient

from ai.application.port_in.ingestion_result import IngestionResult
from ai.domain.model.assistant_response import AssistantResponse
from ai.domain.model.chunk import Chunk
from ai.domain.model.confidence import Confidence
from ai.domain.model.rag_response import RagResponse
from ai.domain.model.search_result import SearchResult
from ai.main import app, container


def test_api_generate() -> None:
    mock_chat = MagicMock()
    mock_chat.sendMessage.return_value = AssistantResponse(
        answer="Test response",
        confidence=Confidence.HIGH,
        prompt_tokens=10,
        completion_tokens=5,
    )

    with container.chat_service.override(mock_chat):
        client = TestClient(app)
        res = client.post(
            "/api/v1/ai/generate",
            json={"messages": [{"role": "user", "content": "Hi"}], "stream": False},
        )
        assert res.status_code == 200
        data = res.json()
        assert data["answer"] == "Test response"
        assert data["confidence"] == "HIGH"


def test_api_rag_endpoints() -> None:
    mock_rag = MagicMock()
    mock_rag.ingest_documents.return_value = IngestionResult(
        total_documents=1,
        total_chunks=2,
        chunk_ids=["c1", "c2"],
        processing_time_ms=15.2,
    )
    mock_rag.search.return_value = [
        SearchResult(
            chunk=Chunk(id="c1", document_id="d1", content="Chunk 1", metadata={"cat": "test"}),
            score=0.95,
        )
    ]
    mock_rag.ask.return_value = RagResponse(
        answer="RAG answer",
        sources=[
            SearchResult(
                chunk=Chunk(id="c1", document_id="d1", content="Chunk 1", metadata={"cat": "test"}),
                score=0.95,
            )
        ],
        confidence=Confidence.HIGH,
        prompt_tokens=20,
        completion_tokens=10,
    )

    with container.rag_service.override(mock_rag):
        client = TestClient(app)

        # Ingest test
        ingest_res = client.post(
            "/api/v1/rag/ingest",
            json={"documents": [{"content": "Doc 1 text", "id": "d1"}]},
        )
        assert ingest_res.status_code == 200
        assert ingest_res.json()["total_chunks"] == 2

        # Search test
        search_res = client.post(
            "/api/v1/rag/search",
            json={"query": "test", "top_k": 2},
        )
        assert search_res.status_code == 200
        assert len(search_res.json()) == 1

        # Ask test
        ask_res = client.post(
            "/api/v1/rag/ask",
            json={"query": "What is in chunk 1?"},
        )
        assert ask_res.status_code == 200
        assert ask_res.json()["answer"] == "RAG answer"
        assert ask_res.json()["confidence"] == "HIGH"
