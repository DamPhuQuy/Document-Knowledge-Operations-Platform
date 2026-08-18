import os
from dataclasses import dataclass
from dotenv import load_dotenv

load_dotenv()

@dataclass(frozen=True)
class LlmConfig:
    api_key: str = os.getenv("LLM_API_KEY", "your-api-key")
    base_url: str = os.getenv("LLM_BASE_URL", "https://api.openai.com/v1")
    model: str = os.getenv("LLM_MODEL", "gpt-4o-mini")
    default_temperature: float = float(os.getenv("LLM_TEMPERATURE", "0.2"))
    grpc_port: str = os.getenv("AI_GRPC_PORT", "50051")
