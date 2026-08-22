from pydantic import BaseModel

from ai.api.dto.request.role_enum import RoleEnum


class MessageModel(BaseModel):
    role: RoleEnum
    content: str
