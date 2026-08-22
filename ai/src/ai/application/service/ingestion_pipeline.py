import logging
import time

from ai.application.port_in.ingestion_use_case import IngestionResult, IngestionUseCase
from ai.application.port_out.embedding_port import EmbeddingPort
from ai.application.port_out.vector_store_port import VectorStorePort
from ai.application.service.chunking_service import ChunkingService
from ai.domain.model.chunk import Chunk
from ai.domain.model.document import Document

logger = logging.getLogger(__name__)


class OfflineIngestionPipeline(IngestionUseCase):
    """
    Offline / Batch Ingestion Pipeline Template for RAG.

    Execution Stages:
      1. Validation: Verifies document content integrity.
      2. Chunking: Splits documents into passages (recursive / fixed / markdown / token).
      3. Batch Embedding: Calls EmbeddingPort in parallel/batches for high throughput.
      4. Vector Store Indexing: Upserts vectors + text + JSONB metadata into pgvector / vector DB.
    """

    def __init__(
        self,
        embedding_port: EmbeddingPort,
        vector_store_port: VectorStorePort,
        chunking_service: ChunkingService | None = None,
        batch_size: int = 64,
    ) -> None:
        self._embedding_port = embedding_port
        self._vector_store = vector_store_port
        self._chunking = chunking_service or ChunkingService()
        self._batch_size = batch_size

    def run_ingestion(
        self,
        documents: list[Document],
        chunk_size: int | None = None,
        chunk_overlap: int | None = None,
        strategy: str = "recursive",
    ) -> IngestionResult:
        start_time = time.perf_counter()

        if not documents:
            return IngestionResult(
                total_documents=0,
                total_chunks=0,
                chunk_ids=[],
                processing_time_ms=0.0,
            )

        logger.info(
            "Starting offline ingestion for %d documents using '%s' chunking strategy.",
            len(documents),
            strategy,
        )

        # Stage 1 & 2: Chunking
        all_chunks: list[Chunk] = []
        for doc in documents:
            if not doc.content.strip():
                continue

            if strategy == "fixed":
                doc_chunks = self._chunking.chunk_fixed(
                    doc, chunk_size=chunk_size, chunk_overlap=chunk_overlap
                )
            elif strategy == "markdown":
                doc_chunks = self._chunking.chunk_markdown(doc, max_chunk_size=chunk_size or 600)
            elif strategy == "token":
                doc_chunks = self._chunking.chunk_by_tokens(
                    doc, max_tokens=chunk_size or 256, overlap_tokens=chunk_overlap or 32
                )
            else:  # default recursive
                doc_chunks = self._chunking.chunk_recursive(
                    doc, chunk_size=chunk_size, chunk_overlap=chunk_overlap
                )

            all_chunks.extend(doc_chunks)

        if not all_chunks:
            return IngestionResult(
                total_documents=len(documents),
                total_chunks=0,
                chunk_ids=[],
                processing_time_ms=(time.perf_counter() - start_time) * 1000,
            )

        logger.info("Generated %d chunks. Computing embeddings...", len(all_chunks))

        # Stage 3: Batch Vector Embedding
        for i in range(0, len(all_chunks), self._batch_size):
            batch = all_chunks[i : i + self._batch_size]
            texts = [c.content for c in batch]
            embeddings = self._embedding_port.embed_documents(texts)
            for chunk, emb in zip(batch, embeddings, strict=True):
                chunk.embedding = emb

        # Stage 4: Indexing into Vector Store (e.g. pgvector)
        logger.info("Upserting %d chunk embeddings into vector database...", len(all_chunks))
        self._vector_store.add_chunks(all_chunks)

        duration_ms = round((time.perf_counter() - start_time) * 1000, 2)
        logger.info("Offline ingestion completed in %.2f ms.", duration_ms)

        return IngestionResult(
            total_documents=len(documents),
            total_chunks=len(all_chunks),
            chunk_ids=[c.id for c in all_chunks],
            processing_time_ms=duration_ms,
            metadata={"strategy": strategy, "batch_size": self._batch_size},
        )
