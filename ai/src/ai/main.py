import os
import sys
from concurrent import futures
from pathlib import Path
import grpc

# Ensure the generated pb2 modules can be found
proto_dir = Path(__file__).parent / "generated"
sys.path.append(str(proto_dir))

import ai.generated.ai_service_pb2_grpc as ai_service_pb2_grpc

from ai.infrastructure.config.config import LlmConfig
from ai.infrastructure.client.openai_client_adapter import OpenAiClientAdapter
from ai.application.service.chat_service import ChatService
from ai.api.grpc_handler import AiGrpcController

def serve():
    # 1. Initialize Configuration (Infrastructure Config)
    config = LlmConfig()
    
    # 2. Instantiate Outbound Adapter (Infrastructure Client)
    openai_adapter = OpenAiClientAdapter(config)
    
    # 3. Instantiate Application Service (Orchestrator)
    chat_service = ChatService(openai_adapter)
    
    # 4. Instantiate Inbound Adapter (API Layer)
    grpc_servicer = AiGrpcController(chat_service)
    
    # 5. Bootstrap and Start gRPC Server
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    ai_service_pb2_grpc.add_AiServiceServicer_to_server(grpc_servicer, server)
    
    server.add_insecure_port(f"[::]:{config.grpc_port}")
    print(f"Starting Clean Architecture Python AI gRPC server on port {config.grpc_port}...")
    server.start()
    server.wait_for_termination()

if __name__ == "__main__":
    serve()
