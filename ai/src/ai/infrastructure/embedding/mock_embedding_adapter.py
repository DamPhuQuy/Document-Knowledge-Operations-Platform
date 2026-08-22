import hashlib
import math

from ai.application.port_out.embedding_port import EmbeddingPort


class MockEmbeddingAdapter(EmbeddingPort):
    """
    Deterministic in-memory embedding adapter for offline tests, local development,
    and fast cookbook experiments without needing an API key.
    Generates normalized pseudo-semantic hash vectors.
    """

    def __init__(self, dimension: int = 1536) -> None:
        self._dim = dimension

    @property
    def dimension(self) -> int:
        return self._dim

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        return [self._embed_single(t) for t in texts]

    def embed_query(self, text: str) -> list[float]:
        return self._embed_single(text)

    def _embed_single(self, text: str) -> list[float]:
        vec = [0.0] * self._dim
        words = text.lower().split()

        for word in words:
            h = int(hashlib.md5(word.encode("utf-8")).hexdigest(), 16)
            for i in range(min(5, self._dim)):
                idx = (h + i * 31) % self._dim
                val = (((h >> (i * 8)) & 0xFF) / 128.0) - 1.0
                vec[idx] += val

        norm = math.sqrt(sum(v * v for v in vec))
        if norm > 0:
            vec = [v / norm for v in vec]
        else:
            vec[0] = 1.0
        return vec
