from typing import Any

import numpy as np

from ai.application.port_out.vector_store_port import VectorStorePort
from ai.domain.model.chunk import Chunk
from ai.domain.model.search_result import SearchResult


class InMemoryVectorStore(VectorStorePort):
    """
    In-memory vector store using NumPy for fast cosine similarity and token search.
    Provides complete transparency for learning and fast offline unit testing.
    """

    def __init__(self) -> None:
        self._chunks: dict[str, Chunk] = {}
        self._vectors: list[np.ndarray[Any, np.dtype[np.float32]]] = []
        self._chunk_id_index: list[str] = []

    def add_chunks(self, chunks: list[Chunk]) -> None:
        for chunk in chunks:
            if chunk.embedding is None:
                raise ValueError(f"Chunk {chunk.id} has no embedding vector.")
            vec = np.array(chunk.embedding, dtype=np.float32)
            norm = np.linalg.norm(vec)
            if norm > 0:
                vec = vec / norm

            if chunk.id in self._chunks:
                pos = self._chunk_id_index.index(chunk.id)
                self._vectors[pos] = vec
                self._chunks[chunk.id] = chunk
            else:
                self._chunks[chunk.id] = chunk
                self._vectors.append(vec)
                self._chunk_id_index.append(chunk.id)

    def similarity_search(
        self,
        query_vector: list[float],
        top_k: int = 4,
        filter_metadata: dict[str, Any] | None = None,
    ) -> list[SearchResult]:
        if not self._vectors:
            return []

        q_vec = np.array(query_vector, dtype=np.float32)
        norm = np.linalg.norm(q_vec)
        if norm > 0:
            q_vec = q_vec / norm

        matrix = np.stack(self._vectors)
        scores: np.ndarray[Any, np.dtype[np.float32]] = np.dot(matrix, q_vec)

        matched_results: list[SearchResult] = []
        for idx, score in enumerate(scores):
            chunk_id = self._chunk_id_index[idx]
            chunk = self._chunks[chunk_id]

            if filter_metadata:
                match = all(chunk.metadata.get(k) == v for k, v in filter_metadata.items())
                if not match:
                    continue

            matched_results.append(SearchResult(chunk=chunk, score=float(score)))

        matched_results.sort(key=lambda r: r.score, reverse=True)
        return matched_results[:top_k]

    def full_text_search(
        self,
        query_text: str,
        top_k: int = 4,
        filter_metadata: dict[str, Any] | None = None,
    ) -> list[SearchResult]:
        query_words = set(query_text.lower().split())
        scored: list[SearchResult] = []

        for cid in self._chunk_id_index:
            chunk = self._chunks[cid]
            if filter_metadata:
                match = all(chunk.metadata.get(k) == v for k, v in filter_metadata.items())
                if not match:
                    continue

            content_words = set(chunk.content.lower().split())
            intersection = query_words.intersection(content_words)
            if intersection:
                score = len(intersection) / max(1, len(query_words))
                scored.append(SearchResult(chunk=chunk, score=score))

        scored.sort(key=lambda r: r.score, reverse=True)
        return scored[:top_k]

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
        for cid in chunk_ids:
            if cid in self._chunks:
                pos = self._chunk_id_index.index(cid)
                del self._chunks[cid]
                del self._vectors[pos]
                del self._chunk_id_index[pos]

    def count(self) -> int:
        return len(self._chunks)

    def clear(self) -> None:
        self._chunks.clear()
        self._vectors.clear()
        self._chunk_id_index.clear()
