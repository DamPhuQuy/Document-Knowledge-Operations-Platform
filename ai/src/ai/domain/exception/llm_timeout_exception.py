from ai.domain.exception.llm_exception import LlmException


class LlmTimeoutException(LlmException):
    """Exception thrown when connection to LLM provider times out."""
