from abc import ABC, abstractmethod

from ai.domain.model.search_result import SearchResult


class RerankerPort(ABC):
    """Outbound port for reranking search results."""

    @abstractmethod
    def rerank(self, query: str, results: list[SearchResult], top_n: int = 3) -> list[SearchResult]:
        """Reranks candidate search results against the query."""
