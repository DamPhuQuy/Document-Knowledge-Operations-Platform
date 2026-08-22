from pydantic import BaseModel, Field

from ai.api.dto.message_model import MessageModel


class ChatRequest(BaseModel):
    messages: list[MessageModel]
    temperature: float = Field(default=0.2, ge=0.0, le=2.0)
    stream: bool = Field(
        default=False, description="Whether to stream the response chunk by chunk"
    )
