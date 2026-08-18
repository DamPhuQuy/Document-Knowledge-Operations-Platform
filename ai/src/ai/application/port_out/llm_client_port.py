from abc import ABC, abstractmethod
from ai.domain.model.llm_message import LlmMessage
from ai.domain.model.assistant_response import AssistantResponse

class LlmClientPort(ABC):
    @abstractmethod
    def generateResponse(
        self, messages: list[LlmMessage], temperature: float = 0.0
    ) -> AssistantResponse:
        pass
