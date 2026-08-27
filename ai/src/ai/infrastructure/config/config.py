import os

from dotenv import load_dotenv

load_dotenv()


class LlmConfig:
    def __init__(self) -> None:
        # LLM Settings
        self.__api_key: str = os.getenv("LLM_API_KEY", "your-api-key")
        self.__base_url: str = os.getenv("LLM_BASE_URL", "https://api.openai.com/v1")
        self.__model: str = os.getenv("LLM_MODEL", "gpt-4o-mini")
        self.__default_temperature: float = float(os.getenv("LLM_TEMPERATURE", "0.2"))
        self.__http_port: int = int(os.getenv("AI_HTTP_PORT", "8000"))

        # Embedding Settings
        self.__embedding_model: str = os.getenv("EMBEDDING_MODEL", "text-embedding-3-small")
        self.__embedding_dimensions: int = int(os.getenv("EMBEDDING_DIMENSIONS", "1536"))

        # PostgreSQL & pgvector Settings
        self.__postgres_host: str = os.getenv("POSTGRES_HOST", "localhost")
        self.__postgres_port: int = int(os.getenv("POSTGRES_PORT", "5432"))
        self.__postgres_db: str = os.getenv("POSTGRES_DB", "doc_knowledge_db")
        self.__postgres_user: str = os.getenv("POSTGRES_USER", "postgres")
        self.__postgres_password: str = os.getenv("POSTGRES_PASSWORD", "postgres")
        self.__postgres_table: str = os.getenv("POSTGRES_RAG_TABLE", "document_chunks")

        # RAG Defaults
        self.__default_chunk_size: int = int(os.getenv("DEFAULT_CHUNK_SIZE", "500"))
        self.__default_chunk_overlap: int = int(os.getenv("DEFAULT_CHUNK_OVERLAP", "50"))
        self.__rag_top_k: int = int(os.getenv("RAG_TOP_K", "4"))
        self.__chroma_persist_dir: str = os.getenv("CHROMA_PERSIST_DIR", "./data/chroma")

    @property
    def api_key(self) -> str:
        return self.__api_key

    @property
    def base_url(self) -> str:
        return self.__base_url

    @property
    def model(self) -> str:
        return self.__model

    @property
    def default_temperature(self) -> float:
        return self.__default_temperature

    @property
    def http_port(self) -> int:
        return self.__http_port

    @property
    def embedding_model(self) -> str:
        return self.__embedding_model

    @property
    def embedding_dimensions(self) -> int:
        return self.__embedding_dimensions

    @property
    def postgres_host(self) -> str:
        return self.__postgres_host

    @property
    def postgres_port(self) -> int:
        return self.__postgres_port

    @property
    def postgres_db(self) -> str:
        return self.__postgres_db

    @property
    def postgres_user(self) -> str:
        return self.__postgres_user

    @property
    def postgres_password(self) -> str:
        return self.__postgres_password

    @property
    def postgres_table(self) -> str:
        return self.__postgres_table

    @property
    def postgres_dsn(self) -> str:
        return (
            f"postgresql://{self.__postgres_user}:{self.__postgres_password}"
            f"@{self.__postgres_host}:{self.__postgres_port}/{self.__postgres_db}"
        )

    @property
    def default_chunk_size(self) -> int:
        return self.__default_chunk_size

    @property
    def default_chunk_overlap(self) -> int:
        return self.__default_chunk_overlap

    @property
    def rag_top_k(self) -> int:
        return self.__rag_top_k

    @property
    def chroma_persist_dir(self) -> str:
        return self.__chroma_persist_dir
