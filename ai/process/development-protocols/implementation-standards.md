# Implementation & Harness Standards

<implementation_standards version="1.0">

<description>
  Engineering quality, type safety, and testing conventions for the `ai` codebase.
</description>

## 1. Type Safety & Code Hygiene
<conventions>
  <rule id="strict_typing">
    All functions, methods, and class attributes must have explicit type annotations. Use union types (`int | None`) and avoid untyped `Any` unless interfacing with raw external payloads.
  </rule>
  <rule id="domain_purity">
    Code in `src/ai/domain/` must remain pure Python with zero external infrastructure dependencies.
  </rule>
  <rule id="immutability">
    Prefer immutable models (`@dataclass(frozen=True)` or Pydantic `BaseModel`) for domain events, value objects, and DTOs.
  </rule>
</conventions>

## 2. Test Architecture
<test_structure>
  - `tests/unit/`: Fast, isolated tests for domain models, chunking logic, and mock adapters.
  - `tests/integration/`: End-to-end flow tests with testcontainers or local test services (e.g. pgvector).
</test_structure>

</implementation_standards>
