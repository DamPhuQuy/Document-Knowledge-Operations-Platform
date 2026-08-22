from typing import Any

from pydantic import BaseModel, Field


class DocumentInput(BaseModel):
    content: str = Field(..., description="Raw text content of the document")
    id: str | None = Field(default=None, description="Optional custom document ID")
    metadata: dict[str, Any] = Field(default_factory=dict, description="Custom metadata key-values")
