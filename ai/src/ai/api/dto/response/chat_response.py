from pydantic import BaseModel

from ai.api.dto.response.confidence_enum import ConfidenceEnum


class ChatResponse(BaseModel):
    answer: str
    confidence: ConfidenceEnum
    prompt_tokens: int
    completion_tokens: int
