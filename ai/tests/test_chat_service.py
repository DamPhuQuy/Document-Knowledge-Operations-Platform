from unittest.mock import MagicMock

from ai.application.service.chat_service import ChatService
from ai.domain.model.assistant_response import AssistantResponse
from ai.domain.model.confidence import Confidence
from ai.domain.model.llm_message import LlmMessage


def test_chat_service() -> None:
    mock_llm = MagicMock()
    mock_llm.generateResponse.return_value = AssistantResponse(
        answer="Hello! How can I help?",
        confidence=Confidence.HIGH,
        prompt_tokens=10,
        completion_tokens=8,
    )

    chat_service = ChatService(llm_client_port=mock_llm)
    messages = [LlmMessage.user("Hello")]
    response = chat_service.sendMessage(messages, temperature=0.2)

    assert response.answer == "Hello! How can I help?"
    assert response.confidence == Confidence.HIGH
    mock_llm.generateResponse.assert_called_once_with(messages, 0.2)
