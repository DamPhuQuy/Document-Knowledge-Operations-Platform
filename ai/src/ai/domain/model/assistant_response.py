from ai.domain.model.confidence import Confidence


class AssistantResponse:
    def __init__(
        self, answer: str, confidence: Confidence, prompt_tokens: int, completion_tokens: int
    ) -> None:
        self.__answer = answer
        self.__confidence = confidence
        self.__prompt_tokens = prompt_tokens
        self.__completion_tokens = completion_tokens

    @property
    def answer(self) -> str:
        return self.__answer

    @property
    def confidence(self) -> Confidence:
        return self.__confidence

    @property
    def prompt_tokens(self) -> int:
        return self.__prompt_tokens

    @property
    def completion_tokens(self) -> int:
        return self.__completion_tokens
