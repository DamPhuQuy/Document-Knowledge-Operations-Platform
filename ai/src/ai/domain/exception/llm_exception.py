class LlmException(Exception):
    """Base exception class for all LLM operations."""

    def __init__(self, message: str, cause: Exception | None = None) -> None:
        super().__init__(message)
        self.cause = cause
