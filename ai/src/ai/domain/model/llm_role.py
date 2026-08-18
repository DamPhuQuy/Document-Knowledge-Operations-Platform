from enum import Enum

class LlmRole(str, Enum):
    SYSTEM = "SYSTEM"
    USER = "USER"
    ASSISTANT = "ASSISTANT"
