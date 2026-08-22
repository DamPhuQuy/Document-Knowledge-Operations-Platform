import asyncio
import json
from collections.abc import AsyncGenerator

from fastapi import APIRouter, HTTPException, status
from fastapi.responses import StreamingResponse

from ai.api.dto import ChatRequest, ChatResponse, ConfidenceEnum, RoleEnum
from ai.application.port_in.chat_use_case import ChatUseCase
from ai.domain.exception.exceptions import (
    LlmSchemaValidationException,
    LlmTimeoutException,
)
from ai.domain.model.llm_message import LlmMessage
from ai.domain.model.llm_role import LlmRole


class AiController:
    def __init__(self, chat_use_case: ChatUseCase) -> None:
        self._chat_use_case = chat_use_case
        self.router = APIRouter(prefix="/api/v1/ai", tags=["AI"])
        self._register_routes()

    def _register_routes(self) -> None:
        """Register API endpoints to the controller router."""
        self.router.add_api_route(
            "/generate",
            self.generate_response,
            methods=["POST"],
            response_model=ChatResponse,
            summary="Generate AI Chat Response",
        )

    async def _simulate_streaming(
        self,
        answer: str,
        confidence: str,
        prompt_tokens: int,
        completion_tokens: int,
    ) -> AsyncGenerator[str, None]:
        """Simulates streaming by yielding words from the final answer with a small delay,
        formatted as Newline-Delimited JSON (NDJSON).
        """
        words = answer.split(" ")
        for i, word in enumerate(words):
            # Add space back except for the first word
            chunk = f" {word}" if i > 0 else word
            data = {
                "chunk": chunk,
                "done": i == len(words) - 1,
            }
            if i == len(words) - 1:
                data.update(
                    {
                        "confidence": confidence,
                        "prompt_tokens": prompt_tokens,
                        "completion_tokens": completion_tokens,
                    }
                )
            yield json.dumps(data) + "\n"
            await asyncio.sleep(0.05)  # Simulate network/generation latency

    async def generate_response(
        self, request: ChatRequest
    ) -> ChatResponse | StreamingResponse:
        try:
            # Map REST request to domain models
            domain_messages = []
            for msg in request.messages:
                if msg.role == RoleEnum.SYSTEM:
                    role = LlmRole.SYSTEM
                elif msg.role == RoleEnum.ASSISTANT:
                    role = LlmRole.ASSISTANT
                else:
                    role = LlmRole.USER
                domain_messages.append(LlmMessage(role=role, content=msg.content))

            # Run in a threadpool to prevent blocking the FastAPI async event loop.
            loop = asyncio.get_running_loop()
            result = await loop.run_in_executor(
                None,
                self._chat_use_case.sendMessage,
                domain_messages,
                request.temperature,
            )

            confidence_val = (
                result.confidence.value
                if hasattr(result.confidence, "value")
                else str(result.confidence)
            )

            if request.stream:
                # Return NDJSON Stream
                return StreamingResponse(
                    self._simulate_streaming(
                        answer=result.answer,
                        confidence=confidence_val,
                        prompt_tokens=result.prompt_tokens,
                        completion_tokens=result.completion_tokens,
                    ),
                    media_type="application/x-ndjson",
                )

            # Standard JSON response
            return ChatResponse(
                answer=result.answer,
                confidence=ConfidenceEnum(confidence_val),
                prompt_tokens=result.prompt_tokens,
                completion_tokens=result.completion_tokens,
            )

        except LlmTimeoutException as ex:
            raise HTTPException(
                status_code=status.HTTP_504_GATEWAY_TIMEOUT,
                detail=str(ex),
            ) from ex
        except LlmSchemaValidationException as ex:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=str(ex),
            ) from ex
        except Exception as ex:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"Internal Server Error: {ex!s}",
            ) from ex
