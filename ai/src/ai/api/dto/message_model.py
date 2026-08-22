from pydantic import BaseModel

from ai.api.dto.enums import RoleEnum


class MessageModel(BaseModel):
    role: RoleEnum
    content: str
