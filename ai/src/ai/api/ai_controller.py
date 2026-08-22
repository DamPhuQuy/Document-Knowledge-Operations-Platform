import asyncio
import json
from collections.abc import AsyncGenerator

from dependency_injector.wiring import Provide, inject
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import StreamingResponse

from ai.api.dto import ChatRequest, ChatResponse, ConfidenceEnum, RoleEnum
from ai.application.port_in.chat_use_case import ChatUseCase
from ai.domain.exception.exceptions import (
    LlmSchemaValidationException,
    LlmTimeoutException,
)
from ai.domain.model.llm_message import LlmMessage
from ai.domain.model.llm_role import LlmRole
from ai.infrastructure.config.container import Container

router = APIRouter(prefix="/api/v1/ai", tags=["AI"])


async def _simulate_streaming(
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
        await asyncio.sleep(0.05)


@router.post(
    "/generate",
    response_model=ChatResponse,
    summary="Generate AI Chat Response",
)
@inject
async def generate_response(
    request: ChatRequest,
    chat_use_case: ChatUseCase = Depends(Provide[Container.chat_service]),
) -> ChatResponse | StreamingResponse:
    try:
        domain_messages = []
        for msg in request.messages:
            if msg.role == RoleEnum.SYSTEM:
                role = LlmRole.SYSTEM
            elif msg.role == RoleEnum.ASSISTANT:
                role = LlmRole.ASSISTANT
            else:
                role = LlmRole.USER
            domain_messages.append(LlmMessage(role=role, content=msg.content))

        loop = asyncio.get_running_loop()
        result = await loop.run_in_executor(
            None,
            chat_use_case.sendMessage,
            domain_messages,
            request.temperature,
        )

        confidence_val = (
            result.confidence.value
            if hasattr(result.confidence, "value")
            else str(result.confidence)
        )

        if request.stream:
            return StreamingResponse(
                _simulate_streaming(
                    answer=result.answer,
                    confidence=confidence_val,
                    prompt_tokens=result.prompt_tokens,
                    completion_tokens=result.completion_tokens,
                ),
                media_type="application/x-ndjson",
            )

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
