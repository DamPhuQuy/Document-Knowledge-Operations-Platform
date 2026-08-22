from abc import ABC, abstractmethod


class EmbeddingPort(ABC):
    """Outbound port for generating dense text vector embeddings."""

    @abstractmethod
    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        """Generates embedding vectors for a list of document chunk strings."""

    @abstractmethod
    def embed_query(self, text: str) -> list[float]:
        """Generates an embedding vector for a single search query."""

    @property
    @abstractmethod
    def dimension(self) -> int:
        """Returns vector dimensionality (e.g. 1536)."""
