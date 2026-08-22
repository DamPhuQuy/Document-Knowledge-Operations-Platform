from enum import StrEnum


class LlmRole(StrEnum):
    SYSTEM = "SYSTEM"
    USER = "USER"
    ASSISTANT = "ASSISTANT"
