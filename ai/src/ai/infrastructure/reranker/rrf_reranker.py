from ai.application.port_out.reranker_port import RerankerPort
from ai.domain.model.search_result import SearchResult


class ReciprocalRankFusionReranker(RerankerPort):
    """
    RRF (Reciprocal Rank Fusion) reranker.
    Formula: score(d) = sum(1 / (k + rank_i(d)))
    """

    def __init__(self, k: int = 60) -> None:
        self.k = k

    def rerank(self, query: str, results: list[SearchResult], top_n: int = 3) -> list[SearchResult]:
        if not results:
            return []

        query_words = set(query.lower().split())
        scored_candidates: list[tuple[SearchResult, float]] = []

        for res in results:
            content_words = set(res.chunk.content.lower().split())
            intersection = query_words.intersection(content_words)
            lexical_ratio = len(intersection) / max(1, len(query_words))
            hybrid_score = (res.score * 0.65) + (lexical_ratio * 0.35)
            scored_candidates.append((res, hybrid_score))

        scored_candidates.sort(key=lambda x: x[1], reverse=True)
        return [
            SearchResult(chunk=res.chunk, score=round(score, 4))
            for res, score in scored_candidates[:top_n]
        ]

    @staticmethod
    def fuse_multiple_rankings(
        ranking_lists: list[list[SearchResult]], k: int = 60, top_n: int = 4
    ) -> list[SearchResult]:
        """Fuses multiple ranked lists using pure Reciprocal Rank Fusion."""
        rrf_scores: dict[str, float] = {}
        chunk_map: dict[str, SearchResult] = {}

        for ranked_list in ranking_lists:
            for rank, item in enumerate(ranked_list, start=1):
                cid = item.chunk.id
                chunk_map[cid] = item
                rrf_scores[cid] = rrf_scores.get(cid, 0.0) + (1.0 / (k + rank))

        sorted_items = sorted(rrf_scores.items(), key=lambda x: x[1], reverse=True)
        return [
            SearchResult(chunk=chunk_map[cid].chunk, score=score)
            for cid, score in sorted_items[:top_n]
        ]
