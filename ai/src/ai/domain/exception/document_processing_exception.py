from ai.domain.exception.rag_exception import RagException


class DocumentProcessingException(RagException):
    """Exception thrown when document chunking or parsing fails."""
