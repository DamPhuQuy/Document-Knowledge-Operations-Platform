from dataclasses import dataclass
from ai.domain.model.llm_role import LlmRole

@dataclass(frozen=True)
class LlmMessage:
    role: LlmRole
    content: str

    @classmethod
    def system(cls, content: str) -> "LlmMessage":
        return cls(LlmRole.SYSTEM, content)

    @classmethod
    def user(cls, content: str) -> "LlmMessage":
        return cls(LlmRole.USER, content)

    @classmethod
    def assistant(cls, content: str) -> "LlmMessage":
        return cls(LlmRole.ASSISTANT, content)
