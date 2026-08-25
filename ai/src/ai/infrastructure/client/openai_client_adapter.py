import json
from typing import Any, NoReturn

from openai import OpenAI
from openai.types.responses import Response

from ai.application.port_out.llm_client_port import LlmClientPort
from ai.domain.exception.exceptions import (
    LlmProviderException,
    LlmSchemaValidationException,
    LlmTimeoutException,
)
from ai.domain.model.assistant_response import AssistantResponse
from ai.domain.model.confidence import Confidence
from ai.domain.model.llm_message import LlmMessage
from ai.domain.model.llm_role import LlmRole
from ai.infrastructure.config.config import LlmConfig


def strip_json_content(content: str) -> str:
    content = content.strip()
    if content.startswith("```json"):
        content = content[7:]
    elif content.startswith("```"):
        content = content[3:]
    content = content.removesuffix("```")
    return content.strip()


def extract_instructions_and_input(
    messages: list[LlmMessage],
) -> tuple[str | None, list[dict[str, Any]]]:
    instructions: str | None = None
    input_items: list[dict[str, Any]] = []

    for msg in messages:
        if msg.role == LlmRole.SYSTEM:
            instructions = msg.content
        else:
            role = "assistant" if msg.role == LlmRole.ASSISTANT else "user"
            input_items.append({"role": role, "content": msg.content})

    return instructions, input_items


def extract_answer(data: dict[str, Any], fallback_content: str) -> str:
    answer = data.get("answer", "")
    if answer:
        return str(answer)
    if "response" in data:
        return str(data["response"])
    return fallback_content


def parse_confidence(confidence_val: Any) -> Confidence:
    confidence_str = str(confidence_val if confidence_val is not None else "MEDIUM").upper()
    try:
        return Confidence(confidence_str)
    except ValueError:
        return Confidence.MEDIUM


def parse_response(response: Response) -> AssistantResponse:
    raw_content = response.output_text or ""
    if not raw_content:
        raise LlmSchemaValidationException("OpenAI Responses API returned no text output message")

    sanitized_content = strip_json_content(raw_content)
    data = json.loads(sanitized_content)

    answer = extract_answer(data, sanitized_content)
    confidence = parse_confidence(data.get("confidence"))

    prompt_tokens = response.usage.input_tokens if response.usage else 0
    completion_tokens = response.usage.output_tokens if response.usage else 0

    return AssistantResponse(
        answer=answer,
        confidence=confidence,
        prompt_tokens=prompt_tokens,
        completion_tokens=completion_tokens,
    )


def handle_exception(ex: Exception) -> NoReturn:
    if isinstance(ex, (LlmTimeoutException, LlmSchemaValidationException)):
        raise ex
    if isinstance(ex, json.JSONDecodeError):
        raise LlmSchemaValidationException(f"LLM did not return valid JSON: {ex!s}") from ex
    err_msg = str(ex).lower()
    if "timeout" in err_msg or "connection" in err_msg:
        raise LlmTimeoutException(f"OpenAI connection/timeout failure: {ex!s}") from ex
    raise LlmProviderException(f"OpenAI provider error: {ex!s}") from ex


class OpenAiClientAdapter(LlmClientPort):
    def __init__(self, config: LlmConfig) -> None:
        self.__config = config
        self.__client = OpenAI(api_key=config.api_key, base_url=config.base_url)

    def generateResponse(
        self, messages: list[LlmMessage], temperature: float = 0.0
    ) -> AssistantResponse:
        temp = temperature if temperature > 0 else self.__config.default_temperature
        instructions, input_items = extract_instructions_and_input(messages)

        kwargs: dict[str, Any] = {
            "model": self.__config.model,
            "input": input_items,
            "temperature": temp,
            "text": {"format": {"type": "json_object"}},
        }
        if instructions:
            kwargs["instructions"] = instructions

        try:
            response = self.__client.responses.create(**kwargs)
            return parse_response(response)
        except Exception as ex:
            handle_exception(ex)
