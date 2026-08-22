from pydantic import BaseModel, Field

from ai.api.dto.request.document_input import DocumentInput


class IngestRequest(BaseModel):
    documents: list[DocumentInput]
    chunk_size: int | None = Field(default=None, description="Custom chunk size in characters")
    chunk_overlap: int | None = Field(
        default=None, description="Custom chunk overlap in characters"
    )
    strategy: str = Field(
        default="recursive", description="Chunking strategy: recursive, fixed, markdown, token"
    )
