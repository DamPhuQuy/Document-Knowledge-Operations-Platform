import json
from unittest.mock import MagicMock, patch

import pytest

from ai.domain.exception.exceptions import (
    LlmProviderException,
    LlmSchemaValidationException,
    LlmTimeoutException,
)
from ai.domain.model.confidence import Confidence
from ai.domain.model.llm_message import LlmMessage
from ai.infrastructure.client.openai_client_adapter import OpenAiClientAdapter
from ai.infrastructure.config.config import LlmConfig


@pytest.fixture
def llm_config() -> MagicMock:
    config = MagicMock(spec=LlmConfig)
    config.api_key = "test-key"
    config.base_url = "https://api.openai.com/v1"
    config.model = "gpt-4o"
    config.default_temperature = 0.7
    return config


def test_openai_generate_response_success(llm_config: LlmConfig) -> None:
    with patch("ai.infrastructure.client.openai_client_adapter.OpenAI") as mock_openai_cls:
        mock_client = MagicMock()
        mock_openai_cls.return_value = mock_client

        mock_usage = MagicMock()
        mock_usage.input_tokens = 15
        mock_usage.output_tokens = 25

        mock_response = MagicMock()
        mock_response.output_text = json.dumps(
            {
                "answer": "Test answer",
                "confidence": "HIGH",
            }
        )
        mock_response.usage = mock_usage

        mock_client.responses.create.return_value = mock_response

        adapter = OpenAiClientAdapter(llm_config)
        messages = [
            LlmMessage.system("System prompt"),
            LlmMessage.assistant("Prev answer"),
            LlmMessage.user("User question"),
        ]
        result = adapter.generateResponse(messages, temperature=0.3)

        assert result.answer == "Test answer"
        assert result.confidence == Confidence.HIGH
        assert result.prompt_tokens == 15
        assert result.completion_tokens == 25

        mock_client.responses.create.assert_called_once_with(
            model="gpt-4o",
            instructions="System prompt",
            input=[
                {"role": "assistant", "content": "Prev answer"},
                {"role": "user", "content": "User question"},
            ],
            temperature=0.3,
            text={"format": {"type": "json_object"}},
        )


def test_openai_generate_response_markdown_json_and_fallbacks(llm_config: LlmConfig) -> None:
    with patch("ai.infrastructure.client.openai_client_adapter.OpenAI") as mock_openai_cls:
        mock_client = MagicMock()
        mock_openai_cls.return_value = mock_client

        # Test markdown json wrapping and "response" fallback
        mock_response = MagicMock()
        mock_response.output_text = (
            "```json\n"
            + json.dumps({"response": "Fallback response", "confidence": "INVALID"})
            + "\n```"
        )
        mock_response.usage = None

        mock_client.responses.create.return_value = mock_response

        adapter = OpenAiClientAdapter(llm_config)
        result = adapter.generateResponse([LlmMessage.user("Hi")], temperature=0.0)

        assert result.answer == "Fallback response"
        assert result.confidence == Confidence.MEDIUM
        assert result.prompt_tokens == 0
        assert result.completion_tokens == 0


def test_openai_empty_content_raises(llm_config: LlmConfig) -> None:
    with patch("ai.infrastructure.client.openai_client_adapter.OpenAI") as mock_openai_cls:
        mock_client = MagicMock()
        mock_openai_cls.return_value = mock_client

        mock_response = MagicMock()
        mock_response.output_text = ""

        mock_client.responses.create.return_value = mock_response

        adapter = OpenAiClientAdapter(llm_config)
        with pytest.raises(LlmSchemaValidationException, match="no text output"):
            adapter.generateResponse([LlmMessage.user("Hi")])


def test_openai_invalid_json_raises(llm_config: LlmConfig) -> None:
    with patch("ai.infrastructure.client.openai_client_adapter.OpenAI") as mock_openai_cls:
        mock_client = MagicMock()
        mock_openai_cls.return_value = mock_client

        mock_response = MagicMock()
        mock_response.output_text = "{not valid json}"

        mock_client.responses.create.return_value = mock_response

        adapter = OpenAiClientAdapter(llm_config)
        with pytest.raises(LlmSchemaValidationException, match="valid JSON"):
            adapter.generateResponse([LlmMessage.user("Hi")])


def test_openai_timeout_error_handling(llm_config: LlmConfig) -> None:
    with patch("ai.infrastructure.client.openai_client_adapter.OpenAI") as mock_openai_cls:
        mock_client = MagicMock()
        mock_openai_cls.return_value = mock_client
        mock_client.responses.create.side_effect = Exception("Connection timed out")

        adapter = OpenAiClientAdapter(llm_config)
        with pytest.raises(LlmTimeoutException):
            adapter.generateResponse([LlmMessage.user("Hi")])


def test_openai_provider_error_handling(llm_config: LlmConfig) -> None:
    with patch("ai.infrastructure.client.openai_client_adapter.OpenAI") as mock_openai_cls:
        mock_client = MagicMock()
        mock_openai_cls.return_value = mock_client
        mock_client.responses.create.side_effect = RuntimeError("Rate limit exceeded")

        adapter = OpenAiClientAdapter(llm_config)
        with pytest.raises(LlmProviderException):
            adapter.generateResponse([LlmMessage.user("Hi")])
