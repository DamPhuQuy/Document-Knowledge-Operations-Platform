from typing import Any

from pydantic import BaseModel, Field


class SearchRequest(BaseModel):
    query: str = Field(..., description="Search query string")
    top_k: int = Field(default=4, ge=1, le=50)
    filter_metadata: dict[str, Any] = Field(default_factory=dict)
    use_hybrid: bool = Field(default=True, description="Enable hybrid search (Dense + FTS + RRF)")
