from dependency_injector import containers, providers
from ai.infrastructure.config.config import LlmConfig
from ai.infrastructure.client.openai_client_adapter import OpenAiClientAdapter
from ai.application.service.chat_service import ChatService

class Container(containers.DeclarativeContainer):
    # Configure modules where dependencies should be injected
    wiring_config = containers.WiringConfiguration(modules=["ai.api.ai_controller"])

    # Singleton configuration provider
    config = providers.Singleton(LlmConfig)

    # Singleton Outbound adapter (infrastructure client adapter)
    openai_adapter = providers.Singleton(
        OpenAiClientAdapter,
        config=config
    )

    # Singleton Application Service (use case implementation)
    chat_service = providers.Singleton(
        ChatService,
        llm_client_port=openai_adapter
    )
