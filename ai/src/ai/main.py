import uvicorn
from fastapi import FastAPI

from ai.infrastructure.config.container import Container

# 1. Initialize Dependency Injection Container
container: Container = Container()

# 2. Create FastAPI App
app: FastAPI = FastAPI(
    title="Clean Architecture AI Service",
    description="REST API for Document Knowledge Operations Platform",
    version="1.0.0",
)

# Attach container instance to app state
app.state.container = container

# Include routers from Controller instances in Container
app.include_router(container.ai_controller().router)


def serve():
    config = container.config()
    print(
        f"Starting Clean Architecture Python AI REST server on port {config.http_port}..."
    )
    uvicorn.run(app, host="127.0.0.1", port=config.http_port)


if __name__ == "__main__":
    serve()
