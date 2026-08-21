from dependency_injector import containers, providers

from ai.api.ai_controller import AiController
from ai.application.service.chat_service import ChatService
from ai.infrastructure.client.openai_client_adapter import OpenAiClientAdapter
from ai.infrastructure.config.config import LlmConfig


class Container(containers.DeclarativeContainer):
    # Singleton configuration provider
    config = providers.Singleton(LlmConfig)

    # Singleton Outbound adapter (infrastructure client adapter)
    openai_adapter = providers.Singleton(OpenAiClientAdapter, config=config)

    # Singleton Application Service (use case implementation)
    chat_service = providers.Singleton(ChatService, llm_client_port=openai_adapter)

    # Singleton Inbound adapter (REST Controller)
    ai_controller = providers.Singleton(AiController, chat_use_case=chat_service)
