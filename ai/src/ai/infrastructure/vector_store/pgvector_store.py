import json
import logging
from typing import Any

import psycopg
from pgvector.psycopg import register_vector

from ai.application.port_out.vector_store_port import VectorStorePort
from ai.domain.exception.exceptions import VectorStoreException
from ai.domain.model.chunk import Chunk
from ai.domain.model.search_result import SearchResult
from ai.infrastructure.config.config import LlmConfig

logger = logging.getLogger(__name__)


class PgVectorStore(VectorStorePort):
    """
    PostgreSQL + pgvector Vector Store Adapter.
    Provides:
      - Dense Vector Similarity Search (HNSW index with cosine distance `<=>`)
      - Sparse Full-Text Search (GIN index with `tsvector` / `websearch_to_tsquery`)
      - Hybrid Retrieval using Reciprocal Rank Fusion (RRF)
      - JSONB metadata filtering
    """

    def __init__(
        self,
        config: LlmConfig,
        table_name: str | None = None,
        auto_init_schema: bool = True,
    ) -> None:
        self._config = config
        self._table = table_name or config.postgres_table
        self._dimensions = config.embedding_dimensions
        self._dsn = config.postgres_dsn

        if auto_init_schema:
            self._init_schema_safe()

    def _get_connection(self) -> psycopg.Connection[Any]:
        try:
            conn = psycopg.connect(self._dsn, autocommit=True)
            register_vector(conn)
            return conn
        except Exception as ex:
            raise VectorStoreException(
                f"Failed to connect to PostgreSQL ({self._config.postgres_host}:{self._config.postgres_port}): {ex!s}",
                ex,
            ) from ex

    def _init_schema_safe(self) -> None:
        """Initializes pgvector extension, table schema, and HNSW/GIN indexes."""
        try:
            with self._get_connection() as conn, conn.cursor() as cur:
                cur.execute("CREATE EXTENSION IF NOT EXISTS vector;")
                cur.execute(
                    f"""
                    CREATE TABLE IF NOT EXISTS {self._table} (
                        id VARCHAR(255) PRIMARY KEY,
                        document_id VARCHAR(255) NOT NULL,
                        chunk_index INT NOT NULL DEFAULT 0,
                        content TEXT NOT NULL,
                        metadata JSONB NOT NULL DEFAULT '{{}}'::jsonb,
                        embedding vector({self._dimensions}),
                        tsv tsvector GENERATED ALWAYS AS (to_tsvector('english', content)) STORED
                    );
                    """
                )
                cur.execute(
                    f"CREATE INDEX IF NOT EXISTS idx_{self._table}_embedding ON {self._table} USING hnsw (embedding vector_cosine_ops);"
                )
                cur.execute(
                    f"CREATE INDEX IF NOT EXISTS idx_{self._table}_tsv ON {self._table} USING gin (tsv);"
                )
                cur.execute(
                    f"CREATE INDEX IF NOT EXISTS idx_{self._table}_doc ON {self._table} (document_id);"
                )
        except Exception as ex:
            logger.warning(
                "PostgreSQL pgvector schema initialization skipped or failed: %s",
                ex,
            )

    def add_chunks(self, chunks: list[Chunk]) -> None:
        if not chunks:
            return

        query = f"""
            INSERT INTO {self._table} (id, document_id, chunk_index, content, metadata, embedding)
            VALUES (%s, %s, %s, %s, %s, %s)
            ON CONFLICT (id) DO UPDATE SET
                document_id = EXCLUDED.document_id,
                chunk_index = EXCLUDED.chunk_index,
                content = EXCLUDED.content,
                metadata = EXCLUDED.metadata,
                embedding = EXCLUDED.embedding;
        """

        try:
            with self._get_connection() as conn, conn.cursor() as cur:
                params = [
                    (
                        c.id,
                        c.document_id,
                        c.chunk_index,
                        c.content,
                        json.dumps(c.metadata),
                        c.embedding,
                    )
                    for c in chunks
                ]
                cur.executemany(query, params)
        except Exception as ex:
            raise VectorStoreException(f"PgVectorStore add_chunks failed: {ex!s}", ex) from ex

    def similarity_search(
        self,
        query_vector: list[float],
        top_k: int = 4,
        filter_metadata: dict[str, Any] | None = None,
    ) -> list[SearchResult]:
        """Dense semantic search using pgvector cosine distance `<=>`."""
        filter_sql = ""
        params: list[Any] = [query_vector]

        if filter_metadata:
            filter_sql = " AND metadata @> %s::jsonb"
            params.append(json.dumps(filter_metadata))

        params.append(top_k)

        query = f"""
            SELECT
                id, document_id, chunk_index, content, metadata,
                1 - (embedding <=> %s::vector) AS similarity_score
            FROM {self._table}
            WHERE embedding IS NOT NULL {filter_sql}
            ORDER BY embedding <=> %s::vector
            LIMIT %s;
        """

        # Parameters: [query_vector, (filter_json), query_vector, top_k]
        exec_params: list[Any] = [query_vector]
        if filter_metadata:
            exec_params.append(json.dumps(filter_metadata))
        exec_params.append(query_vector)
        exec_params.append(top_k)

        try:
            with self._get_connection() as conn, conn.cursor() as cur:
                cur.execute(query, exec_params)
                rows = cur.fetchall()

                results: list[SearchResult] = []
                for row in rows:
                    chunk = Chunk(
                        id=str(row[0]),
                        document_id=str(row[1]),
                        chunk_index=int(row[2]),
                        content=str(row[3]),
                        metadata=row[4] if isinstance(row[4], dict) else json.loads(row[4]),
                    )
                    score = max(0.0, float(row[5]))
                    results.append(SearchResult(chunk=chunk, score=score))

                return results
        except Exception as ex:
            raise VectorStoreException(
                f"PgVectorStore similarity_search failed: {ex!s}", ex
            ) from ex

    def full_text_search(
        self,
        query_text: str,
        top_k: int = 4,
        filter_metadata: dict[str, Any] | None = None,
    ) -> list[SearchResult]:
        """Sparse lexical search using PostgreSQL Full-Text Search (GIN tsvector)."""
        filter_sql = ""
        exec_params: list[Any] = [query_text, query_text]

        if filter_metadata:
            filter_sql = " AND metadata @> %s::jsonb"
            exec_params.append(json.dumps(filter_metadata))

        exec_params.append(top_k)

        query = f"""
            SELECT
                id, document_id, chunk_index, content, metadata,
                ts_rank(tsv, websearch_to_tsquery('english', %s)) AS rank_score
            FROM {self._table}
            WHERE tsv @@ websearch_to_tsquery('english', %s) {filter_sql}
            ORDER BY rank_score DESC
            LIMIT %s;
        """

        try:
            with self._get_connection() as conn, conn.cursor() as cur:
                cur.execute(query, exec_params)
                rows = cur.fetchall()

                results: list[SearchResult] = []
                for row in rows:
                    chunk = Chunk(
                        id=str(row[0]),
                        document_id=str(row[1]),
                        chunk_index=int(row[2]),
                        content=str(row[3]),
                        metadata=row[4] if isinstance(row[4], dict) else json.loads(row[4]),
                    )
                    results.append(SearchResult(chunk=chunk, score=float(row[5])))

                return results
        except Exception as ex:
            raise VectorStoreException(f"PgVectorStore full_text_search failed: {ex!s}", ex) from ex

    def hybrid_search(
        self,
        query_text: str,
        query_vector: list[float],
        top_k: int = 4,
        filter_metadata: dict[str, Any] | None = None,
        k: int = 60,
    ) -> list[SearchResult]:
        """
        Hybrid retrieval combining Dense Vector Search + Sparse Full-Text Search
        using Reciprocal Rank Fusion (RRF): RRF_score = sum(1 / (k + rank)).
        """
        dense_results = self.similarity_search(
            query_vector=query_vector,
            top_k=top_k * 2,
            filter_metadata=filter_metadata,
        )

        sparse_results = self.full_text_search(
            query_text=query_text,
            top_k=top_k * 2,
            filter_metadata=filter_metadata,
        )

        rrf_scores: dict[str, float] = {}
        chunk_map: dict[str, Chunk] = {}

        # Rank dense
        for rank, res in enumerate(dense_results, start=1):
            cid = res.chunk.id
            chunk_map[cid] = res.chunk
            rrf_scores[cid] = rrf_scores.get(cid, 0.0) + (1.0 / (k + rank))

        # Rank sparse
        for rank, res in enumerate(sparse_results, start=1):
            cid = res.chunk.id
            chunk_map[cid] = res.chunk
            rrf_scores[cid] = rrf_scores.get(cid, 0.0) + (1.0 / (k + rank))

        sorted_fused = sorted(rrf_scores.items(), key=lambda item: item[1], reverse=True)

        results: list[SearchResult] = []
        for cid, score in sorted_fused[:top_k]:
            results.append(SearchResult(chunk=chunk_map[cid], score=score))

        return results

    def delete(self, chunk_ids: list[str]) -> None:
        if not chunk_ids:
            return
        try:
            with self._get_connection() as conn, conn.cursor() as cur:
                cur.execute(
                    f"DELETE FROM {self._table} WHERE id = ANY(%s);",
                    (chunk_ids,),
                )
        except Exception as ex:
            raise VectorStoreException(f"PgVectorStore delete failed: {ex!s}", ex) from ex

    def count(self) -> int:
        try:
            with self._get_connection() as conn, conn.cursor() as cur:
                cur.execute(f"SELECT count(*) FROM {self._table};")
                row = cur.fetchone()
                return int(row[0]) if row else 0
        except Exception as ex:
            raise VectorStoreException(f"PgVectorStore count failed: {ex!s}", ex) from ex

    def clear(self) -> None:
        try:
            with self._get_connection() as conn, conn.cursor() as cur:
                cur.execute(f"TRUNCATE TABLE {self._table};")
        except Exception as ex:
            raise VectorStoreException(f"PgVectorStore clear failed: {ex!s}", ex) from ex
