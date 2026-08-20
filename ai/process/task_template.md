# Task: [Brief Task Name]

<task_spec version="1.0">

## 1. Specification

### Goal
[Describe the target outcome, not implementation details.]

### Current Behavior
[How the system currently behaves.]

### Expected Behavior
[Expected behavior once the task is complete.]

### Invariants & Business Rules
- [Invariant 1 — conditions that must remain true before and after changes.]

### Out of Scope
<out_of_scope>
- [Explicitly excluded scope, unrelated refactoring, or undeclared API/schema changes.]
</out_of_scope>

### Acceptance Criteria
- [ ] [Criterion 1 — verifiable behavior with expected result]
- [ ] [Criterion 2 — test case or edge case handling]

---

## 2. Context Boundaries

### Relevant Files & Symbols
- `[path/to/file]` — [Related class, function, or module]

### References & Dependencies
- `[docs/... | API contract | DB schema | env config]`

---

## 3. Execution Plan & Verification

### Plan
- [ ] Step 1: [Define/modify contracts, schemas, or interfaces]
- [ ] Step 2: [Implement core domain/application logic]
- [ ] Step 3: [Add or update tests]

### Verification
```bash
# Focused test
uv run pytest path/to/test_file.py -k test_name

# Full suite verification
uv run pytest
uv run ruff check .
uv run mypy --strict .
```

---

## 4. Open Decisions & Stop Conditions

### Open Decisions
<open_decisions>
- [ ] [Item requiring human clarification or decision]
</open_decisions>

### Stop Conditions
Stop and request human input if:
- Changes require undeclared modifications to public APIs or database schemas.
- Implementation conflicts with existing domain invariants.
- Solution requires touching components in `<out_of_scope>`.
- Consecutive validation failures exceed retry budget (3 attempts).
- Expected behavior cannot be determined from available code, tests, or specs.

</task_spec>
