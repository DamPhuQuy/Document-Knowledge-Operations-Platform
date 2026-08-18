from google.protobuf.internal import containers as _containers
from google.protobuf.internal import enum_type_wrapper as _enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from collections.abc import Iterable as _Iterable, Mapping as _Mapping
from typing import ClassVar as _ClassVar, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class LlmRole(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    SYSTEM: _ClassVar[LlmRole]
    USER: _ClassVar[LlmRole]
    ASSISTANT: _ClassVar[LlmRole]

class Confidence(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    LOW: _ClassVar[Confidence]
    MEDIUM: _ClassVar[Confidence]
    HIGH: _ClassVar[Confidence]

SYSTEM: LlmRole
USER: LlmRole
ASSISTANT: LlmRole
LOW: Confidence
MEDIUM: Confidence
HIGH: Confidence

class LlmMessage(_message.Message):
    __slots__ = ("role", "content")
    ROLE_FIELD_NUMBER: _ClassVar[int]
    CONTENT_FIELD_NUMBER: _ClassVar[int]
    role: LlmRole
    content: str
    def __init__(
        self, role: _Optional[_Union[LlmRole, str]] = ..., content: _Optional[str] = ...
    ) -> None: ...

class ChatRequest(_message.Message):
    __slots__ = ("messages", "temperature")
    MESSAGES_FIELD_NUMBER: _ClassVar[int]
    TEMPERATURE_FIELD_NUMBER: _ClassVar[int]
    messages: _containers.RepeatedCompositeFieldContainer[LlmMessage]
    temperature: float
    def __init__(
        self,
        messages: _Optional[_Iterable[_Union[LlmMessage, _Mapping]]] = ...,
        temperature: _Optional[float] = ...,
    ) -> None: ...

class ChatResponse(_message.Message):
    __slots__ = ("answer", "confidence", "prompt_tokens", "completion_tokens")
    ANSWER_FIELD_NUMBER: _ClassVar[int]
    CONFIDENCE_FIELD_NUMBER: _ClassVar[int]
    PROMPT_TOKENS_FIELD_NUMBER: _ClassVar[int]
    COMPLETION_TOKENS_FIELD_NUMBER: _ClassVar[int]
    answer: str
    confidence: Confidence
    prompt_tokens: int
    completion_tokens: int
    def __init__(
        self,
        answer: _Optional[str] = ...,
        confidence: _Optional[_Union[Confidence, str]] = ...,
        prompt_tokens: _Optional[int] = ...,
        completion_tokens: _Optional[int] = ...,
    ) -> None: ...
