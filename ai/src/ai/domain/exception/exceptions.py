class LlmException(Exception):
    """Base exception class for all LLM operations."""
    def __init__(self, message: str, cause: Exception = None):
        super().__init__(message)
        self.cause = cause

class LlmTimeoutException(LlmException):
    """Exception thrown when connection to LLM provider times out."""
    pass

class LlmProviderException(LlmException):
    """Exception thrown when the LLM provider returns an API or server error."""
    pass

class LlmSchemaValidationException(LlmException):
    """Exception thrown when the LLM output is not valid or doesn't conform to schema."""
    pass
