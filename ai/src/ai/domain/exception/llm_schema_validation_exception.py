from ai.domain.exception.llm_exception import LlmException


class LlmSchemaValidationException(LlmException):
    """Exception thrown when the LLM output is not valid or doesn't conform to schema."""
