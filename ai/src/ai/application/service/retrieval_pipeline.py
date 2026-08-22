import logging
from typing import Any

from ai.application.port_in.retrieval_use_case import RetrievalUseCase
from ai.application.port_out.embedding_port import EmbeddingPort
from ai.application.port_out.llm_client_port import LlmClientPort
from ai.application.port_out.reranker_port import RerankerPort
from ai.application.port_out.vector_store_port import VectorStorePort
from ai.domain.model.confidence import Confidence
from ai.domain.model.llm_message import LlmMessage
from ai.domain.model.rag_query import RagQuery
from ai.domain.model.rag_response import RagResponse
from ai.domain.model.search_result import SearchResult

logger = logging.getLogger(__name__)


class OnlineRetrievalPipeline(RetrievalUseCase):
    """
    Online Hybrid Retrieval & Generation Pipeline Template for RAG.

    Execution Stages:
      1. Query Processing: Encodes query using EmbeddingPort.
      2. Hybrid Retrieval: Queries pgvector/vector store using Dense + Sparse FTS + RRF.
      3. Context Assembly: Formats grounded passage blocks with metadata and source citations.
      4. Prompt Engineering: Standard structured system instructions enforcing factual answers.
      5. LLM Synthesis: Generates structured response via LlmClientPort.
    """

    SYSTEM_PROMPT = (
        "You are an expert AI Knowledge Assistant.\n"
        "Answer the user query accurately, concisely, and factually based ONLY on the provided context passages.\n"
        "If the context does not contain enough information to answer the question, state: "
        "'I could not find sufficient information in the knowledge base.'\n"
        "Do NOT extrapolate or invent facts beyond the provided context.\n\n"
        "Respond in valid JSON format:\n"
        '{\n  "answer": "Grounded answer citing [Passage X] where appropriate",\n'
        '  "confidence": "HIGH" | "MEDIUM" | "LOW"\n}'
    )

    def __init__(
        self,
        embedding_port: EmbeddingPort,
        vector_store_port: VectorStorePort,
        llm_client_port: LlmClientPort,
        reranker_port: RerankerPort | None = None,
    ) -> None:
        self._embedding_port = embedding_port
        self._vector_store = vector_store_port
        self._llm_client = llm_client_port
        self._reranker = reranker_port

    def retrieve(
        self,
        query: str,
        top_k: int = 4,
        filter_metadata: dict[str, Any] | None = None,
        use_hybrid: bool = True,
    ) -> list[SearchResult]:
        query_vector = self._embedding_port.embed_query(query)

        if use_hybrid:
            results = self._vector_store.hybrid_search(
                query_text=query,
                query_vector=query_vector,
                top_k=top_k,
                filter_metadata=filter_metadata,
            )
        else:
            results = self._vector_store.similarity_search(
                query_vector=query_vector,
                top_k=top_k,
                filter_metadata=filter_metadata,
            )

        if self._reranker and results:
            results = self._reranker.rerank(query, results, top_n=top_k)

        return results

    def generate_answer(self, query: RagQuery) -> RagResponse:
        # Stage 1: Retrieval
        results = self.retrieve(
            query=query.query,
            top_k=query.top_k,
            filter_metadata=query.filter_metadata if query.filter_metadata else None,
            use_hybrid=query.use_hybrid,
        )

        # Optional threshold filtering
        if query.score_threshold is not None:
            results = [r for r in results if r.score >= query.score_threshold]

        # Stage 2: Empty Context Fallback
        if not results:
            return RagResponse(
                answer="I could not find any relevant information in the knowledge base to answer your question.",
                sources=[],
                confidence=Confidence.LOW,
                prompt_tokens=0,
                completion_tokens=0,
                metadata={"retrieved_count": 0},
            )

        # Stage 3: Context Assembly
        passage_blocks: list[str] = []
        for i, res in enumerate(results, start=1):
            source = res.chunk.metadata.get("source", res.chunk.document_id)
            passage_blocks.append(
                f"[Passage {i}] (Source: {source}, Score: {res.score:.4f}):\n{res.chunk.content}"
            )

        context_text = "\n\n".join(passage_blocks)
        user_message_content = (
            f"Context Passages:\n{context_text}\n\n"
            f"User Question: {query.query}\n\n"
            "Please provide a factual, grounded answer citing relevant passage numbers."
        )

        # Stage 4 & 5: Prompt & Generation
        messages = [
            LlmMessage.system(self.SYSTEM_PROMPT),
            LlmMessage.user(user_message_content),
        ]

        llm_res = self._llm_client.generateResponse(
            messages=messages,
            temperature=query.temperature,
        )

        return RagResponse(
            answer=llm_res.answer,
            sources=results,
            confidence=llm_res.confidence,
            prompt_tokens=llm_res.prompt_tokens,
            completion_tokens=llm_res.completion_tokens,
            metadata={
                "retrieved_count": len(results),
                "hybrid_used": query.use_hybrid,
            },
        )
