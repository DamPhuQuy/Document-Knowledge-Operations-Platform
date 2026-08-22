from typing import Any

from pydantic import BaseModel, Field


class AskRequest(BaseModel):
    query: str = Field(..., description="User question to answer using RAG")
    top_k: int = Field(default=4, ge=1, le=20)
    score_threshold: float | None = Field(default=None, ge=0.0, le=1.0)
    filter_metadata: dict[str, Any] = Field(default_factory=dict)
    use_hybrid: bool = Field(
        default=True, description="Use Hybrid Search (Dense + Full-Text Search)"
    )
    temperature: float = Field(default=0.0, ge=0.0, le=2.0)
