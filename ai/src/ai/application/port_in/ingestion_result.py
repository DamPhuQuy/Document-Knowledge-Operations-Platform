from dataclasses import dataclass, field
from typing import Any


@dataclass
class IngestionResult:
    """Outcome summary of an offline ingestion batch."""

    total_documents: int
    total_chunks: int
    chunk_ids: list[str]
    processing_time_ms: float = 0.0
    metadata: dict[str, Any] = field(default_factory=dict)
