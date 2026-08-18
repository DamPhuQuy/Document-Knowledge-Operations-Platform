import json

from openai import OpenAI
from ai.application.port_out.llm_client_port import LlmClientPort
from ai.domain.model.llm_message import LlmMessage
from ai.domain.model.llm_role import LlmRole
from ai.domain.model.assistant_response import AssistantResponse
from ai.domain.model.confidence import Confidence
from ai.domain.exception.exceptions import (
    LlmTimeoutException,
    LlmProviderException,
    LlmSchemaValidationException,
)
from ai.infrastructure.config.config import LlmConfig

def _strip_json_content(content: str) -> str:
    content = content.strip()
    if content.startswith("```json"):
        content = content[7:]
    elif content.startswith("```"):
        content = content[3:]
    if content.endswith("```"):
        content = content[:-3]
    return content.strip()

class OpenAiClientAdapter(LlmClientPort):
    def __init__(self, config: LlmConfig):
        self.__config = config
        self.__client = OpenAI(api_key=config.api_key, base_url=config.base_url)

    def generateResponse(self, messages: list[LlmMessage], temperature: float = None) -> AssistantResponse:
        try:
            # Extract system instructions and input messages
            instructions = None
            input_items = []
            for msg in messages:
                if msg.role == LlmRole.SYSTEM:
                    instructions = msg.content
                else:
                    role = "assistant" if msg.role == LlmRole.ASSISTANT else "user"
                    input_items.append({"role": role, "content": msg.content})

            temp = temperature if temperature is not None and temperature > 0 else self.__config.default_temperature

            kwargs = {
                "model": self.__config.model,
                "input": input_items,
                "temperature": temp,
                "text": {"format": {"type": "json_object"}}
            }
            if instructions:
                kwargs["instructions"] = instructions

            # Invoke using the new Responses API: client.responses.create()
            response = self.__client.responses.create(**kwargs)

            raw_content = response.output_text
            if not raw_content:
                raise LlmSchemaValidationException("OpenAI Responses API returned no text output message")

            sanitized_content = _strip_json_content(raw_content)

            data = json.loads(sanitized_content)
            answer = data.get("answer", "")
            if not answer:
                raise LlmSchemaValidationException("LLM output is missing required 'answer' field")

            confidence_str = data.get("confidence", "MEDIUM").upper()
            try:
                confidence = Confidence(confidence_str)
            except ValueError:
                confidence = Confidence.MEDIUM

            # Mapping input_tokens and output_tokens from responses.create usage
            prompt_tokens = response.usage.input_tokens if response.usage else 0
            completion_tokens = response.usage.output_tokens if response.usage else 0

            return AssistantResponse(
                answer=answer,
                confidence=confidence,
                prompt_tokens=prompt_tokens,
                completion_tokens=completion_tokens
            )

        except json.JSONDecodeError as ex:
            raise LlmSchemaValidationException(f"LLM did not return valid JSON: {str(ex)}", ex)
        except Exception as ex:
            if "timeout" in str(ex).lower() or "connection" in str(ex).lower():
                raise LlmTimeoutException(f"OpenAI connection/timeout failure: {str(ex)}", ex)
            raise LlmProviderException(f"OpenAI provider error: {str(ex)}", ex)
