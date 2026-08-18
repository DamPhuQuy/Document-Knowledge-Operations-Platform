from abc import ABC, abstractmethod
from typing import List
from ai.domain.model.llm_message import LlmMessage
from ai.domain.model.assistant_response import AssistantResponse


class LlmClientPort(ABC):
    @abstractmethod
    def generateResponse(
        self, messages: List[LlmMessage], temperature: float = 0.0
    ) -> AssistantResponse:
        pass
