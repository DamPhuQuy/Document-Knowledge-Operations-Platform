from ai.domain.exception.llm_exception import LlmException


class LlmProviderException(LlmException):
    """Exception thrown when the LLM provider returns an API or server error."""
