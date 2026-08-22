from dataclasses import dataclass, field
from typing import Any


@dataclass(frozen=True)
class RagQuery:
    """Represents an incoming retrieval-augmented generation query."""

    query: str
    top_k: int = 4
    score_threshold: float | None = None
    filter_metadata: dict[str, Any] = field(default_factory=dict)
    use_hybrid: bool = True
    hybrid_alpha: float = 0.5  # Weight between dense (alpha) and sparse (1-alpha) or RRF
    temperature: float = 0.0
