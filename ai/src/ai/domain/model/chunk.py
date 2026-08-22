import uuid
from dataclasses import dataclass, field
from typing import Any


@dataclass
class Chunk:
    """Represents a text passage chunk extracted from a document."""

    content: str
    document_id: str
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    chunk_index: int = 0
    metadata: dict[str, Any] = field(default_factory=dict)
    embedding: list[float] | None = None
