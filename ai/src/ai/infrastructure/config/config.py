import os
from dotenv import load_dotenv

load_dotenv()


class LlmConfig:
    def __init__(self) -> None:
        self.__api_key: str = os.getenv("LLM_API_KEY", "your-api-key")
        self.__base_url: str = os.getenv("LLM_BASE_URL", "https://api.openai.com/v1")
        self.__model: str = os.getenv("LLM_MODEL", "gpt-4o-mini")
        self.__default_temperature: float = float(os.getenv("LLM_TEMPERATURE", "0.2"))
        self.__http_port: int = int(os.getenv("AI_HTTP_PORT", "8000"))

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
