from abc import ABC, abstractmethod

from ai.application.port_in.ingestion_result import IngestionResult
from ai.domain.model.document import Document


class IngestionUseCase(ABC):
    """Inbound port for offline / batch document ingestion pipelines."""

    @abstractmethod
    def run_ingestion(
        self,
        documents: list[Document],
        chunk_size: int | None = None,
        chunk_overlap: int | None = None,
        strategy: str = "recursive",
    ) -> IngestionResult:
        """Processes, chunks, embeds, and indexes raw documents into the vector database."""
