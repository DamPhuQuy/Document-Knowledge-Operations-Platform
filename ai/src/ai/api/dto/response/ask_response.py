from pydantic import BaseModel

from ai.api.dto.response.search_result_item import SearchResultItem


class AskResponse(BaseModel):
    answer: str
    confidence: str
    sources: list[SearchResultItem]
    prompt_tokens: int
    completion_tokens: int
    retrieved_count: int
