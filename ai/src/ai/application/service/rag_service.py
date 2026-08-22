from typing import Any

from ai.application.port_in.ingestion_use_case import IngestionResult, IngestionUseCase
from ai.application.port_in.rag_use_case import RagUseCase
from ai.application.port_in.retrieval_use_case import RetrievalUseCase
from ai.domain.model.document import Document
from ai.domain.model.rag_query import RagQuery
from ai.domain.model.rag_response import RagResponse
from ai.domain.model.search_result import SearchResult


class RagService(RagUseCase):
    """
    Unified Application Service implementing RagUseCase.
    Orchestrates the Offline Ingestion Pipeline and Online Hybrid Retrieval Pipeline.
    """

    def __init__(
        self,
        ingestion_pipeline: IngestionUseCase,
        retrieval_pipeline: RetrievalUseCase,
    ) -> None:
        self._ingestion = ingestion_pipeline
        self._retrieval = retrieval_pipeline

    def ingest_documents(
        self,
        documents: list[Document],
        chunk_size: int | None = None,
        chunk_overlap: int | None = None,
    ) -> IngestionResult:
        """Executes the offline document ingestion pipeline."""
        return self._ingestion.run_ingestion(
            documents=documents,
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
        )

    def search(
        self,
        query: str,
        top_k: int = 4,
        filter_metadata: dict[str, Any] | None = None,
        use_hybrid: bool = True,
    ) -> list[SearchResult]:
        """Executes hybrid vector & full-text retrieval."""
        return self._retrieval.retrieve(
            query=query,
            top_k=top_k,
            filter_metadata=filter_metadata,
            use_hybrid=use_hybrid,
        )

    def ask(self, query: RagQuery) -> RagResponse:
        """Executes the full online RAG answer generation pipeline."""
        return self._retrieval.generate_answer(query)
