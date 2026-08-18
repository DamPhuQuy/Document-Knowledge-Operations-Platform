import asyncio
import json
from enum import Enum
from typing import AsyncGenerator
from fastapi import APIRouter, HTTPException, Depends
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field
from dependency_injector.wiring import inject, Provide

from ai.infrastructure.config.container import Container
from ai.application.port_in.chat_use_case import ChatUseCase
from ai.domain.model.llm_message import LlmMessage
from ai.domain.model.llm_role import LlmRole
from ai.domain.exception.exceptions import (
    LlmTimeoutException,
    LlmSchemaValidationException,
)

router = APIRouter(prefix="/api/v1/ai", tags=["AI"])

class RoleEnum(str, Enum):
    SYSTEM = "system"
    USER = "user"
    ASSISTANT = "assistant"

class MessageModel(BaseModel):
    role: RoleEnum
    content: str

class ChatRequest(BaseModel):
    messages: list[MessageModel]
    temperature: float = Field(default=0.2, ge=0.0, le=2.0)
    stream: bool = Field(default=False, description="Whether to stream the response chunk by chunk")

class ConfidenceEnum(str, Enum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"

class ChatResponse(BaseModel):
    answer: str
    confidence: ConfidenceEnum
    prompt_tokens: int
    completion_tokens: int

async def _simulate_streaming(answer: str, confidence: str, prompt_tokens: int, completion_tokens: int) -> AsyncGenerator[str, None]:
    """
    Simulates streaming by yielding words from the final answer with a small delay,
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
            data.update({
                "confidence": confidence,
                "prompt_tokens": prompt_tokens,
                "completion_tokens": completion_tokens
            })
        yield json.dumps(data) + "\n"
        await asyncio.sleep(0.05)  # Simulate network/generation latency

@router.post("/generate", response_model=ChatResponse)
@inject
async def generate_response(
    request: ChatRequest,
    chat_use_case: ChatUseCase = Depends(Provide[Container.chat_service])
):
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

        # Currently the core domain ChatUseCase runs synchronously.
        # Run in a threadpool to prevent blocking the FastAPI async event loop.
        loop = asyncio.get_running_loop()
        result = await loop.run_in_executor(
            None,
            chat_use_case.sendMessage,
            domain_messages,
            request.temperature
        )

        confidence_val = result.confidence.value if hasattr(result.confidence, "value") else str(result.confidence)

        if request.stream:
            # Return NDJSON Stream
            return StreamingResponse(
                _simulate_streaming(
                    answer=result.answer,
                    confidence=confidence_val,
                    prompt_tokens=result.prompt_tokens,
                    completion_tokens=result.completion_tokens
                ),
                media_type="application/x-ndjson"
            )

        # Standard JSON response
        return ChatResponse(
            answer=result.answer,
            confidence=ConfidenceEnum(confidence_val),
            prompt_tokens=result.prompt_tokens,
            completion_tokens=result.completion_tokens
        )

    except LlmTimeoutException as ex:
        raise HTTPException(status_code=504, detail=str(ex))
    except LlmSchemaValidationException as ex:
        raise HTTPException(status_code=400, detail=str(ex))
    except Exception as ex:
        raise HTTPException(status_code=500, detail=f"Internal Server Error: {str(ex)}")
