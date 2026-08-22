from ai.domain.model.chunk import Chunk
from ai.domain.model.search_result import SearchResult
from ai.infrastructure.reranker.rrf_reranker import ReciprocalRankFusionReranker


def test_rrf_reranking() -> None:
    reranker = ReciprocalRankFusionReranker(k=60)

    c1 = Chunk(id="1", document_id="d1", content="Postgres connection pooling pgbouncer")
    c2 = Chunk(id="2", document_id="d2", content="Redis cache invalidation ttl")

    results = [
        SearchResult(chunk=c1, score=0.8),
        SearchResult(chunk=c2, score=0.9),
    ]

    reranked = reranker.rerank(query="pgbouncer postgres", results=results, top_n=2)
    assert len(reranked) == 2
    assert reranked[0].chunk.id == "1"


def test_fuse_multiple_rankings() -> None:
    c1 = Chunk(id="doc1", document_id="d1", content="content 1")
    c2 = Chunk(id="doc2", document_id="d2", content="content 2")
    c3 = Chunk(id="doc3", document_id="d3", content="content 3")

    list1 = [SearchResult(chunk=c1, score=1.0), SearchResult(chunk=c2, score=0.8)]
    list2 = [SearchResult(chunk=c2, score=1.0), SearchResult(chunk=c3, score=0.7)]

    fused = ReciprocalRankFusionReranker.fuse_multiple_rankings([list1, list2], k=60, top_n=3)
    assert len(fused) == 3
    assert fused[0].chunk.id == "doc2"
