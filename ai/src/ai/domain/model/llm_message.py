from ai.domain.model.llm_role import LlmRole


class LlmMessage:
    def __init__(self, role: LlmRole, content: str) -> None:
        self.__role = role
        self.__content = content

    @property
    def role(self) -> LlmRole:
        return self.__role

    @property
    def content(self) -> str:
        return self.__content

    @classmethod
    def system(cls, content: str) -> "LlmMessage":
        return cls(LlmRole.SYSTEM, content)

    @classmethod
    def user(cls, content: str) -> "LlmMessage":
        return cls(LlmRole.USER, content)

    @classmethod
    def assistant(cls, content: str) -> "LlmMessage":
        return cls(LlmRole.ASSISTANT, content)
