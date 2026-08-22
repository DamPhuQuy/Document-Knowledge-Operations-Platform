from abc import ABC, abstractmethod
from typing import Any

from ai.application.port_in.ingestion_use_case import IngestionResult
from ai.domain.model.document import Document
from ai.domain.model.rag_query import RagQuery
from ai.domain.model.rag_response import RagResponse
from ai.domain.model.search_result import SearchResult


class RagUseCase(ABC):
    """Unified Inbound port for RAG capabilities (facade over ingestion & retrieval)."""

    @abstractmethod
    def ask(self, query: RagQuery) -> RagResponse:
        """Answers a user question grounded in retrieved knowledge base passages."""

    @abstractmethod
    def ingest_documents(
        self,
        documents: list[Document],
        chunk_size: int | None = None,
        chunk_overlap: int | None = None,
    ) -> IngestionResult:
        """Runs the offline ingestion pipeline for documents."""

    @abstractmethod
    def search(
        self,
        query: str,
        top_k: int = 4,
        filter_metadata: dict[str, Any] | None = None,
        use_hybrid: bool = True,
    ) -> list[SearchResult]:
        """Performs raw hybrid search without LLM generation."""
