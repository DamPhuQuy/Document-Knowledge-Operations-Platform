from pydantic import BaseModel

from ai.api.dto.enums import ConfidenceEnum


class ChatResponse(BaseModel):
    answer: str
    confidence: ConfidenceEnum
    prompt_tokens: int
    completion_tokens: int
