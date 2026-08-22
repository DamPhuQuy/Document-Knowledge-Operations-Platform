from dependency_injector import containers, providers

from ai.application.service.chat_service import ChatService
from ai.application.service.chunking_service import ChunkingService
from ai.application.service.ingestion_pipeline import OfflineIngestionPipeline
from ai.application.service.rag_service import RagService
from ai.application.service.retrieval_pipeline import OnlineRetrievalPipeline
from ai.infrastructure.client.openai_client_adapter import OpenAiClientAdapter
from ai.infrastructure.config.config import LlmConfig
from ai.infrastructure.embedding.openai_embedding_adapter import OpenAiEmbeddingAdapter
from ai.infrastructure.reranker.rrf_reranker import ReciprocalRankFusionReranker
from ai.infrastructure.vector_store.chroma_vector_store import ChromaVectorStore
from ai.infrastructure.vector_store.in_memory_vector_store import InMemoryVectorStore
from ai.infrastructure.vector_store.pgvector_store import PgVectorStore


class Container(containers.DeclarativeContainer):
    # Configure modules where dependencies should be injected
    wiring_config = containers.WiringConfiguration(
        modules=[
            "ai.api.ai_controller",
            "ai.api.rag_controller",
        ]
    )

    # 1. Configuration provider
    config = providers.Singleton(LlmConfig)

    # 2. LLM Client Outbound Adapter
    openai_adapter = providers.Singleton(
        OpenAiClientAdapter,
        config=config,
    )

    # 3. Embedding Outbound Adapter
    openai_embedding_adapter = providers.Singleton(
        OpenAiEmbeddingAdapter,
        config=config,
    )

    # 4. Vector Stores (Postgres pgvector as primary production store, in-memory & Chroma as alternatives)
    pgvector_store = providers.Singleton(
        PgVectorStore,
        config=config,
    )

    in_memory_vector_store = providers.Singleton(
        InMemoryVectorStore,
    )

    chroma_vector_store = providers.Singleton(
        ChromaVectorStore,
        collection_name="knowledge_ops_rag",
        persist_directory=config.provided.chroma_persist_dir,
    )

    # 5. Reranker & Chunking Services
    reranker = providers.Singleton(
        ReciprocalRankFusionReranker,
    )

    chunking_service = providers.Singleton(
        ChunkingService,
        default_chunk_size=config.provided.default_chunk_size,
        default_chunk_overlap=config.provided.default_chunk_overlap,
    )

    # 6. Chat Service
    chat_service = providers.Singleton(
        ChatService,
        llm_client_port=openai_adapter,
    )

    # 7. Offline Ingestion Pipeline Template
    ingestion_pipeline = providers.Singleton(
        OfflineIngestionPipeline,
        embedding_port=openai_embedding_adapter,
        vector_store_port=pgvector_store,
        chunking_service=chunking_service,
    )

    # 8. Online Hybrid Retrieval & Generation Pipeline Template
    retrieval_pipeline = providers.Singleton(
        OnlineRetrievalPipeline,
        embedding_port=openai_embedding_adapter,
        vector_store_port=pgvector_store,
        llm_client_port=openai_adapter,
        reranker_port=reranker,
    )

    # 9. Unified RAG Facade Service
    rag_service = providers.Singleton(
        RagService,
        ingestion_pipeline=ingestion_pipeline,
        retrieval_pipeline=retrieval_pipeline,
    )
