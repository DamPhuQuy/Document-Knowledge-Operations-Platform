import uvicorn
from fastapi import FastAPI

from ai.api.ai_controller import router as chat_router
from ai.api.rag_controller import router as rag_router
from ai.infrastructure.config.container import Container

# 1. Initialize Dependency Injection Container
container: Container = Container()

# 2. Create FastAPI App
app: FastAPI = FastAPI(
    title="Clean Architecture AI & RAG Platform",
    description="REST API for Document Knowledge Operations Platform",
    version="1.0.0",
)

# Attach container instance to app state
app.state.container = container

# Include routers
app.include_router(chat_router)
app.include_router(rag_router)


def serve() -> None:
    config = container.config()
    print(f"Starting Clean Architecture Python AI REST server on port {config.http_port}...")
    uvicorn.run(app, host="0.0.0.0", port=config.http_port)


if __name__ == "__main__":
    serve()
