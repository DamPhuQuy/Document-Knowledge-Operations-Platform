import os
from pathlib import Path
import uvicorn
from fastapi import FastAPI

from ai.infrastructure.config.container import Container
from ai.api.ai_controller import router

# 1. Initialize Dependency Injection Container
container = Container()

# Create FastAPI App
app = FastAPI(
    title="Clean Architecture AI Service",
    description="REST API for Document Knowledge Operations Platform",
    version="1.0.0"
)

# Attach container instance to app state
app.state.container = container

# Include routers
app.include_router(router)

def serve():
    config = container.config()
    print(f"Starting Clean Architecture Python AI REST server on port {config.http_port}...")
    uvicorn.run(app, host="0.0.0.0", port=config.http_port)

if __name__ == "__main__":
    serve()
