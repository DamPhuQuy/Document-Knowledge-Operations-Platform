from openai import OpenAI

from ai.application.port_out.embedding_port import EmbeddingPort
from ai.domain.exception.exceptions import EmbeddingException
from ai.infrastructure.config.config import LlmConfig


class OpenAiEmbeddingAdapter(EmbeddingPort):
    """Generates embeddings using OpenAI API (text-embedding-3-small, text-embedding-3-large)."""

    def __init__(self, config: LlmConfig) -> None:
        self._config = config
        self._client = OpenAI(api_key=config.api_key, base_url=config.base_url)
        self._model = config.embedding_model
        self._dimension = config.embedding_dimensions

    @property
    def dimension(self) -> int:
        return self._dimension

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        if not texts:
            return []
        try:
            response = self._client.embeddings.create(
                model=self._model,
                input=texts,
            )
            sorted_data = sorted(response.data, key=lambda item: item.index)
            return [item.embedding for item in sorted_data]
        except Exception as ex:
            raise EmbeddingException(f"OpenAI embedding error: {ex!s}", ex) from ex

    def embed_query(self, text: str) -> list[float]:
        try:
            response = self._client.embeddings.create(
                model=self._model,
                input=[text],
            )
            return response.data[0].embedding
        except Exception as ex:
            raise EmbeddingException(f"OpenAI query embedding error: {ex!s}", ex) from ex
