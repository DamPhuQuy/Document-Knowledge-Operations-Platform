from unittest.mock import MagicMock

from ai.application.port_in.ingestion_result import IngestionResult
from ai.application.service.rag_service import RagService
from ai.domain.model.chunk import Chunk
from ai.domain.model.confidence import Confidence
from ai.domain.model.document import Document
from ai.domain.model.rag_query import RagQuery
from ai.domain.model.rag_response import RagResponse
from ai.domain.model.search_result import SearchResult


def test_rag_service_facade() -> None:
    mock_ingestion = MagicMock()
    mock_ingestion.run_ingestion.return_value = IngestionResult(
        total_documents=1,
        total_chunks=2,
        chunk_ids=["c1", "c2"],
        processing_time_ms=10.0,
    )

    mock_retrieval = MagicMock()
    mock_retrieval.retrieve.return_value = [
        SearchResult(chunk=Chunk(id="c1", document_id="d1", content="Content"), score=0.9)
    ]
    mock_retrieval.generate_answer.return_value = RagResponse(
        answer="Grounded answer",
        confidence=Confidence.HIGH,
        sources=[
            SearchResult(chunk=Chunk(id="c1", document_id="d1", content="Content"), score=0.9)
        ],
    )

    rag_service = RagService(
        ingestion_pipeline=mock_ingestion,
        retrieval_pipeline=mock_retrieval,
    )

    # Ingest
    res_ingest = rag_service.ingest_documents([Document(content="Test")])
    assert res_ingest.total_chunks == 2
    mock_ingestion.run_ingestion.assert_called_once()

    # Search
    res_search = rag_service.search("query")
    assert len(res_search) == 1
    mock_retrieval.retrieve.assert_called_once()

    # Ask
    res_ask = rag_service.ask(RagQuery(query="query"))
    assert res_ask.answer == "Grounded answer"
    mock_retrieval.generate_answer.assert_called_once()
