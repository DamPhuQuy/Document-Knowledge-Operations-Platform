import json
from typing import Any

from openai import OpenAI

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


def _strip_json_content(content: str) -> str:
    content = content.strip()
    if content.startswith("```json"):
        content = content[7:]
    elif content.startswith("```"):
        content = content[3:]
    content = content.removesuffix("```")
    return content.strip()


class OpenAiClientAdapter(LlmClientPort):
    def __init__(self, config: LlmConfig) -> None:
        self.__config = config
        self.__client = OpenAI(api_key=config.api_key, base_url=config.base_url)

    def generateResponse(
        self, messages: list[LlmMessage], temperature: float = 0.0
    ) -> AssistantResponse:
        try:
            temp = temperature if temperature > 0 else self.__config.default_temperature

            openai_messages: list[dict[str, Any]] = []
            for msg in messages:
                if msg.role == LlmRole.SYSTEM:
                    role_str = "system"
                elif msg.role == LlmRole.ASSISTANT:
                    role_str = "assistant"
                else:
                    role_str = "user"
                openai_messages.append({"role": role_str, "content": msg.content})

            response = self.__client.chat.completions.create(
                model=self.__config.model,
                messages=openai_messages,  # type: ignore[arg-type]
                temperature=temp,
                response_format={"type": "json_object"},
            )

            choice = response.choices[0]
            raw_content = choice.message.content or ""
            if not raw_content:
                raise LlmSchemaValidationException("OpenAI returned empty content message")

            sanitized_content = _strip_json_content(raw_content)
            data = json.loads(sanitized_content)

            answer = data.get("answer", "")
            if not answer:
                answer = str(data["response"]) if "response" in data else sanitized_content

            confidence_str = str(data.get("confidence", "MEDIUM")).upper()
            try:
                confidence = Confidence(confidence_str)
            except ValueError:
                confidence = Confidence.MEDIUM

            prompt_tokens = response.usage.prompt_tokens if response.usage else 0
            completion_tokens = response.usage.completion_tokens if response.usage else 0

            return AssistantResponse(
                answer=answer,
                confidence=confidence,
                prompt_tokens=prompt_tokens,
                completion_tokens=completion_tokens,
            )

        except json.JSONDecodeError as ex:
            raise LlmSchemaValidationException(f"LLM did not return valid JSON: {ex!s}") from ex
        except (LlmTimeoutException, LlmSchemaValidationException):
            raise
        except Exception as ex:
            err_msg = str(ex).lower()
            if "timeout" in err_msg or "connection" in err_msg:
                raise LlmTimeoutException(f"OpenAI connection/timeout failure: {ex!s}") from ex
            raise LlmProviderException(f"OpenAI provider error: {ex!s}") from ex
