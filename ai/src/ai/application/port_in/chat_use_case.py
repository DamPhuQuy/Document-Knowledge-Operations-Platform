from abc import ABC, abstractmethod

from ai.domain.model.assistant_response import AssistantResponse
from ai.domain.model.llm_message import LlmMessage


class ChatUseCase(ABC):
    @abstractmethod
    def sendMessage(
        self, messages: list[LlmMessage], temperature: float = 0.0
    ) -> AssistantResponse:
        pass
