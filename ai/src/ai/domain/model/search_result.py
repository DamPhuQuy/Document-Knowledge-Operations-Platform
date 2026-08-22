from dataclasses import dataclass

from ai.domain.model.chunk import Chunk


@dataclass(frozen=True)
class SearchResult:
    """Represents a matched chunk with a relevance / similarity score."""

    chunk: Chunk
    score: float
