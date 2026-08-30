# Implementation & Harness Standards

<implementation_standards version="1.0">

<description>
  Engineering quality, type safety, and testing conventions for this codebase.
</description>

## 1. Type Safety & Code Hygiene
<conventions>
  <rule id="strict_typing">
    All functions, methods, and class attributes must have explicit type annotations. Use modern union types (`int | None` or `string | null`) and avoid untyped `Any`/`any` unless interfacing with raw external payloads.
  </rule>
  <rule id="domain_purity">
    Code in domain layers must remain pure with zero external infrastructure dependencies.
  </rule>
  <rule id="immutability">
    Prefer immutable models for domain events, value objects, and DTOs.
  </rule>
</conventions>

## 2. Test Architecture
<test_structure>
  - `tests/unit/`: Fast, isolated tests for domain models, core logic, and mock adapters.
  - `tests/integration/`: End-to-end flow tests with test services or local databases.
</test_structure>

</implementation_standards>
