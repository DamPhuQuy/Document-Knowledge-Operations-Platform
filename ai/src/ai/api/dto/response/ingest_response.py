from pydantic import BaseModel


class IngestResponse(BaseModel):
    total_documents: int
    total_chunks: int
    chunk_ids: list[str]
    processing_time_ms: float
