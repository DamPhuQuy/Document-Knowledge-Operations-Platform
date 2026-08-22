import uuid
from dataclasses import dataclass, field
from typing import Any


@dataclass(frozen=True)
class Document:
    """Represents a raw source document."""

    content: str
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    metadata: dict[str, Any] = field(default_factory=dict)
