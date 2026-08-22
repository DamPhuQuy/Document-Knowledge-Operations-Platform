from typing import Any

from pydantic import BaseModel


class SearchResultItem(BaseModel):
    chunk_id: str
    document_id: str
    content: str
    score: float
    metadata: dict[str, Any]
