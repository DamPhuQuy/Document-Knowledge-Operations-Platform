from abc import ABC, abstractmethod
from typing import Any

from ai.domain.model.chunk import Chunk
from ai.domain.model.search_result import SearchResult


class VectorStorePort(ABC):
    """Outbound port for vector database and hybrid search operations."""

    @abstractmethod
    def add_chunks(self, chunks: list[Chunk]) -> None:
        """Upserts chunks with their embeddings and metadata into storage."""

    @abstractmethod
    def similarity_search(
        self,
        query_vector: list[float],
        top_k: int = 4,
        filter_metadata: dict[str, Any] | None = None,
    ) -> list[SearchResult]:
        """Performs dense vector similarity search."""

    @abstractmethod
    def full_text_search(
        self,
        query_text: str,
        top_k: int = 4,
        filter_metadata: dict[str, Any] | None = None,
    ) -> list[SearchResult]:
        """Performs sparse lexical/full-text search."""

    @abstractmethod
    def hybrid_search(
        self,
        query_text: str,
        query_vector: list[float],
        top_k: int = 4,
        filter_metadata: dict[str, Any] | None = None,
        k: int = 60,
    ) -> list[SearchResult]:
        """Performs combined hybrid search (Dense + Sparse with Reciprocal Rank Fusion)."""

    @abstractmethod
    def delete(self, chunk_ids: list[str]) -> None:
        """Deletes chunks by their IDs."""

    @abstractmethod
    def count(self) -> int:
        """Returns total count of stored chunks."""

    @abstractmethod
    def clear(self) -> None:
        """Clears all stored chunks."""
