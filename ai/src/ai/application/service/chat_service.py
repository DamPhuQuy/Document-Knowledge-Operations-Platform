from typing import List
from ai.application.port_in.chat_use_case import ChatUseCase
from ai.application.port_out.llm_client_port import LlmClientPort
from ai.domain.model.llm_message import LlmMessage
from ai.domain.model.assistant_response import AssistantResponse


class ChatService(ChatUseCase):
    def __init__(self, llm_client_port: LlmClientPort):
        self.llm_client_port = llm_client_port

    def sendMessage(
        self, messages: List[LlmMessage], temperature: float = 0.0
    ) -> AssistantResponse:
        # Business logic can be performed here (e.g. prompt audit, guardrails, context compaction)
        return self.llm_client_port.generateResponse(messages, temperature)
