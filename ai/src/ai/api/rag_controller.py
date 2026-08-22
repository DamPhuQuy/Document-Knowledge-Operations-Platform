import asyncio

from dependency_injector.wiring import Provide, inject
from fastapi import APIRouter, Depends, HTTPException

from ai.api.dto import (
    AskRequest,
    AskResponse,
    IngestRequest,
    IngestResponse,
    SearchRequest,
    SearchResultItem,
)
from ai.application.port_in.rag_use_case import RagUseCase
from ai.domain.exception.exceptions import RagException
from ai.domain.model.document import Document
from ai.domain.model.rag_query import RagQuery
from ai.infrastructure.config.container import Container

router = APIRouter(prefix="/api/v1/rag", tags=["RAG"])


@router.post("/ingest", response_model=IngestResponse)
@inject
async def ingest_documents_endpoint(
    request: IngestRequest,
    rag_use_case: RagUseCase = Depends(Provide[Container.rag_service]),
) -> IngestResponse:
    """Executes the offline document ingestion pipeline (Chunk -> Batch Embed -> Index into pgvector)."""
    try:
        domain_docs = [
            Document(
                content=d.content,
                id=d.id or "",
                metadata=d.metadata,
            )
            if d.id
            else Document(content=d.content, metadata=d.metadata)
            for d in request.documents
        ]

        loop = asyncio.get_running_loop()
        result = await loop.run_in_executor(
            None,
            rag_use_case.ingest_documents,
            domain_docs,
            request.chunk_size,
            request.chunk_overlap,
        )

        return IngestResponse(
            total_documents=result.total_documents,
            total_chunks=result.total_chunks,
            chunk_ids=result.chunk_ids,
            processing_time_ms=result.processing_time_ms,
        )
    except RagException as ex:
        raise HTTPException(status_code=400, detail=str(ex)) from ex
    except Exception as ex:
        raise HTTPException(status_code=500, detail=f"Ingestion failed: {ex!s}") from ex


@router.post("/search", response_model=list[SearchResultItem])
@inject
async def search_endpoint(
    request: SearchRequest,
    rag_use_case: RagUseCase = Depends(Provide[Container.rag_service]),
) -> list[SearchResultItem]:
    """Executes hybrid retrieval (Dense pgvector + Sparse full-text search + RRF)."""
    try:
        loop = asyncio.get_running_loop()
        results = await loop.run_in_executor(
            None,
            rag_use_case.search,
            request.query,
            request.top_k,
            request.filter_metadata if request.filter_metadata else None,
            request.use_hybrid,
        )

        return [
            SearchResultItem(
                chunk_id=r.chunk.id,
                document_id=r.chunk.document_id,
                content=r.chunk.content,
                score=r.score,
                metadata=r.chunk.metadata,
            )
            for r in results
        ]
    except Exception as ex:
        raise HTTPException(status_code=500, detail=f"Search failed: {ex!s}") from ex


@router.post("/ask", response_model=AskResponse)
@inject
async def ask_endpoint(
    request: AskRequest,
    rag_use_case: RagUseCase = Depends(Provide[Container.rag_service]),
) -> AskResponse:
    """Executes the online hybrid RAG pipeline to generate a grounded answer with citations."""
    try:
        domain_query = RagQuery(
            query=request.query,
            top_k=request.top_k,
            score_threshold=request.score_threshold,
            filter_metadata=request.filter_metadata,
            use_hybrid=request.use_hybrid,
            temperature=request.temperature,
        )

        loop = asyncio.get_running_loop()
        result = await loop.run_in_executor(None, rag_use_case.ask, domain_query)

        source_items = [
            SearchResultItem(
                chunk_id=s.chunk.id,
                document_id=s.chunk.document_id,
                content=s.chunk.content,
                score=s.score,
                metadata=s.chunk.metadata,
            )
            for s in result.sources
        ]

        conf_val = (
            result.confidence.value
            if hasattr(result.confidence, "value")
            else str(result.confidence)
        )

        return AskResponse(
            answer=result.answer,
            confidence=conf_val,
            sources=source_items,
            prompt_tokens=result.prompt_tokens,
            completion_tokens=result.completion_tokens,
            retrieved_count=len(result.sources),
        )
    except RagException as ex:
        raise HTTPException(status_code=400, detail=str(ex)) from ex
    except Exception as ex:
        raise HTTPException(status_code=500, detail=f"RAG query failed: {ex!s}") from ex
