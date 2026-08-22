from typing import Any

import chromadb
from chromadb.api import ClientAPI
from chromadb.api.models.Collection import Collection

from ai.application.port_out.vector_store_port import VectorStorePort
from ai.domain.exception.exceptions import VectorStoreException
from ai.domain.model.chunk import Chunk
from ai.domain.model.search_result import SearchResult


class ChromaVectorStore(VectorStorePort):
    """
    ChromaDB adapter implementing VectorStorePort.
    Supports in-memory ephemeral storage or persistent disk storage.
    """

    def __init__(
        self,
        collection_name: str = "rag_knowledge_base",
        persist_directory: str | None = None,
        client: ClientAPI | None = None,
    ) -> None:
        try:
            if client is not None:
                self._client = client
            elif persist_directory:
                self._client = chromadb.PersistentClient(path=persist_directory)
            else:
                self._client = chromadb.EphemeralClient()

            self._collection: Collection = self._client.get_or_create_collection(
                name=collection_name,
                metadata={"hnsw:space": "cosine"},
            )
        except Exception as ex:
            raise VectorStoreException(f"Failed to initialize ChromaDB: {ex!s}", ex) from ex

    def add_chunks(self, chunks: list[Chunk]) -> None:
        if not chunks:
            return
        try:
            ids: list[str] = []
            embeddings: list[list[float]] = []
            documents: list[str] = []
            metadatas: list[dict[str, Any]] = []

            for c in chunks:
                if c.embedding is None:
                    raise ValueError(f"Chunk {c.id} missing embedding vector")
                ids.append(c.id)
                embeddings.append(c.embedding)
                documents.append(c.content)

                meta = {
                    "document_id": c.document_id,
                    "chunk_index": c.chunk_index,
                }
                for k, v in c.metadata.items():
                    if isinstance(v, (str, int, float, bool)):
                        meta[k] = v
                    else:
                        meta[k] = str(v)
                metadatas.append(meta)

            self._collection.upsert(
                ids=ids,
                embeddings=embeddings,  # type: ignore[arg-type]
                documents=documents,
                metadatas=metadatas,  # type: ignore[arg-type]
            )
        except Exception as ex:
            raise VectorStoreException(f"ChromaDB add_chunks failed: {ex!s}", ex) from ex

    def similarity_search(
        self,
        query_vector: list[float],
        top_k: int = 4,
        filter_metadata: dict[str, Any] | None = None,
    ) -> list[SearchResult]:
        try:
            where_clause: dict[str, Any] | None = None
            if filter_metadata:
                where_clause = {k: v for k, v in filter_metadata.items()}

            kwargs: dict[str, Any] = {
                "query_embeddings": [query_vector],
                "n_results": min(top_k, max(1, self.count())),
            }
            if where_clause:
                kwargs["where"] = where_clause

            if self.count() == 0:
                return []

            results = self._collection.query(**kwargs)

            search_results: list[SearchResult] = []
            if not results or not results["ids"] or not results["ids"][0]:
                return search_results

            ids = results["ids"][0]
            docs = results["documents"][0] if results.get("documents") else []
            metas = results["metadatas"][0] if results.get("metadatas") else []
            distances = results["distances"][0] if results.get("distances") else []

            for i, chunk_id in enumerate(ids):
                content = docs[i] if i < len(docs) else ""
                metadata = metas[i] if i < len(metas) and metas[i] else {}
                doc_id = str(metadata.get("document_id", "unknown"))
                chunk_index = int(metadata.get("chunk_index", 0))

                dist = float(distances[i]) if i < len(distances) else 0.0
                sim_score = max(0.0, 1.0 - dist)

                chunk = Chunk(
                    id=chunk_id,
                    document_id=doc_id,
                    content=content,
                    chunk_index=chunk_index,
                    metadata=metadata,
                )
                search_results.append(SearchResult(chunk=chunk, score=sim_score))

            return search_results
        except Exception as ex:
            raise VectorStoreException(f"ChromaDB similarity_search failed: {ex!s}", ex) from ex

    def full_text_search(
        self,
        query_text: str,
        top_k: int = 4,
        filter_metadata: dict[str, Any] | None = None,
    ) -> list[SearchResult]:
        # ChromaDB document contains search
        try:
            where_doc = {"$contains": query_text}
            results = self._collection.get(
                where_document=where_doc,
                limit=top_k,
            )
            search_results: list[SearchResult] = []
            if not results or not results["ids"]:
                return search_results

            ids = results["ids"]
            docs = results["documents"] or []
            metas = results["metadatas"] or []

            for i, chunk_id in enumerate(ids):
                content = docs[i] if i < len(docs) else ""
                metadata = metas[i] if i < len(metas) and metas[i] else {}
                chunk = Chunk(
                    id=chunk_id,
                    document_id=str(metadata.get("document_id", "unknown")),
                    chunk_index=int(metadata.get("chunk_index", 0)),
                    content=content,
                    metadata=metadata,
                )
                search_results.append(SearchResult(chunk=chunk, score=1.0))
            return search_results
        except Exception:
            return []

    def hybrid_search(
        self,
        query_text: str,
        query_vector: list[float],
        top_k: int = 4,
        filter_metadata: dict[str, Any] | None = None,
        k: int = 60,
    ) -> list[SearchResult]:
        dense = self.similarity_search(
            query_vector, top_k=top_k * 2, filter_metadata=filter_metadata
        )
        sparse = self.full_text_search(query_text, top_k=top_k * 2, filter_metadata=filter_metadata)

        rrf_scores: dict[str, float] = {}
        chunk_map: dict[str, Chunk] = {}

        for rank, item in enumerate(dense, start=1):
            cid = item.chunk.id
            chunk_map[cid] = item.chunk
            rrf_scores[cid] = rrf_scores.get(cid, 0.0) + (1.0 / (k + rank))

        for rank, item in enumerate(sparse, start=1):
            cid = item.chunk.id
            chunk_map[cid] = item.chunk
            rrf_scores[cid] = rrf_scores.get(cid, 0.0) + (1.0 / (k + rank))

        sorted_fused = sorted(rrf_scores.items(), key=lambda x: x[1], reverse=True)
        return [
            SearchResult(chunk=chunk_map[cid], score=score) for cid, score in sorted_fused[:top_k]
        ]

    def delete(self, chunk_ids: list[str]) -> None:
        if not chunk_ids:
            return
        try:
            self._collection.delete(ids=chunk_ids)
        except Exception as ex:
            raise VectorStoreException(f"ChromaDB delete failed: {ex!s}", ex) from ex

    def count(self) -> int:
        try:
            return self._collection.count()
        except Exception as ex:
            raise VectorStoreException(f"ChromaDB count failed: {ex!s}", ex) from ex

    def clear(self) -> None:
        try:
            name = self._collection.name
            self._client.delete_collection(name=name)
            self._collection = self._client.get_or_create_collection(
                name=name, metadata={"hnsw:space": "cosine"}
            )
        except Exception as ex:
            raise VectorStoreException(f"ChromaDB clear failed: {ex!s}", ex) from ex
