from dataclasses import dataclass, field
from typing import Any

from ai.domain.model.confidence import Confidence
from ai.domain.model.search_result import SearchResult


@dataclass(frozen=True)
class RagResponse:
    """Represents a generated answer grounded in retrieved passages."""

    answer: str
    sources: list[SearchResult] = field(default_factory=list)
    confidence: Confidence = Confidence.HIGH
    prompt_tokens: int = 0
    completion_tokens: int = 0
    metadata: dict[str, Any] = field(default_factory=dict)
