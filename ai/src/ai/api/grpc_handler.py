import sys
from pathlib import Path
import grpc

# Add generated directory to sys.path so generated protobuf modules can import each other
proto_dir = Path(__file__).parent.parent / "generated"
sys.path.append(str(proto_dir))

import ai.generated.ai_service_pb2 as ai_service_pb2
import ai.generated.ai_service_pb2_grpc as ai_service_pb2_grpc

from ai.application.port_in.chat_use_case import ChatUseCase
from ai.domain.model.llm_message import LlmMessage
from ai.domain.model.llm_role import LlmRole
from ai.domain.exception.exceptions import (
    LlmTimeoutException,
    LlmSchemaValidationException,
)


class AiGrpcController(ai_service_pb2_grpc.AiServiceServicer):
    def __init__(self, chat_use_case: ChatUseCase):
        self.chat_use_case = chat_use_case

    def GenerateResponse(self, request, context):
        print("Received GenerateResponse RPC request via Clean Architecture API Layer")

        try:
            # Map request messages list to Domain models
            domain_messages = []
            for msg in request.messages:
                # Map role
                if msg.role == ai_service_pb2.SYSTEM:
                    role = LlmRole.SYSTEM
                elif msg.role == ai_service_pb2.ASSISTANT:
                    role = LlmRole.ASSISTANT
                else:
                    role = LlmRole.USER
                domain_messages.append(LlmMessage(role=role, content=msg.content))

            # Call use case
            result = self.chat_use_case.sendMessage(
                domain_messages, request.temperature
            )

            # Map confidence domain enum to gRPC confidence
            confidence_map = {
                "LOW": ai_service_pb2.LOW,
                "MEDIUM": ai_service_pb2.MEDIUM,
                "HIGH": ai_service_pb2.HIGH,
            }
            confidence = confidence_map.get(
                result.confidence.value, ai_service_pb2.MEDIUM
            )

            return ai_service_pb2.ChatResponse(
                answer=result.answer,
                confidence=confidence,
                prompt_tokens=result.prompt_tokens,
                completion_tokens=result.completion_tokens,
            )

        except LlmTimeoutException as ex:
            print(f"Timeout exception occurred: {str(ex)}")
            context.set_code(grpc.StatusCode.DEADLINE_EXCEEDED)
            context.set_details(str(ex))
            return ai_service_pb2.ChatResponse()

        except LlmSchemaValidationException as ex:
            print(f"Schema validation exception occurred: {str(ex)}")
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details(str(ex))
            return ai_service_pb2.ChatResponse()

        except Exception as ex:
            print(f"Unexpected error in API layer: {str(ex)}")
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(f"Internal gRPC server error: {str(ex)}")
            return ai_service_pb2.ChatResponse()
