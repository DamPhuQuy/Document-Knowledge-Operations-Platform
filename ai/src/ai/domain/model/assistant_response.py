from dataclasses import dataclass
from ai.domain.model.confidence import Confidence

@dataclass(frozen=True)
class AssistantResponse:
    answer: str
    confidence: Confidence
    prompt_tokens: int
    completion_tokens: int
