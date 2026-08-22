from abc import ABC, abstractmethod
from typing import Any

from ai.domain.model.rag_query import RagQuery
from ai.domain.model.rag_response import RagResponse
from ai.domain.model.search_result import SearchResult


class RetrievalUseCase(ABC):
    """Inbound port for online hybrid retrieval and answer generation."""

    @abstractmethod
    def retrieve(
        self,
        query: str,
        top_k: int = 4,
        filter_metadata: dict[str, Any] | None = None,
        use_hybrid: bool = True,
    ) -> list[SearchResult]:
        """Performs online hybrid retrieval (Dense + Sparse Full-Text Search)."""

    @abstractmethod
    def generate_answer(self, query: RagQuery) -> RagResponse:
        """Executes full online RAG pipeline: retrieval -> grounded prompt synthesis -> answer."""
