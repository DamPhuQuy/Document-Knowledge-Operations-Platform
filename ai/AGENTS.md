# Repository Agent Guidelines (RIPER-5 Framework)

<agent_guidelines version="2.0">

<overview>
  Master operating instructions for AI agents in the `ai` subsystem. Establishes the **4 Core Pillars** and the **RIPER-5 Operational Loop**, integrated with the [`process/`](process/README.md) task framework.
</overview>

---

## 1. Core Foundations

<foundations>
  <!-- Pillar 1: Task & Specification -->
  <pillar id="task_spec" title="Task & Specification">
    <rule>Single Source of Truth: Active task file in [`process/features/{feature}/active/`](process/features/) or [`process/general-plans/active/`](process/general-plans/) (instantiated from [`process/_seeds/task-template.md.seed`](process/_seeds/task-template.md.seed)).</rule>
    <rule>Define changes via Goal, Current Behavior, Expected Behavior, Invariants, and `<out_of_scope>`.</rule>
    <rule>Acceptance Criteria (AC) must be unambiguous, verifiable markdown checkboxes (`- [ ]`).</rule>
  </pillar>

  <!-- Pillar 2: Context Navigation -->
  <pillar id="context" title="Context Navigation">
    <rule>Gather minimum sufficient context. No full-repo scanning or drive-by refactoring.</rule>
    <information_priority>
      1. Task Spec & AC ([`process/features/**/_SPEC*.md`](process/features/), [`process/general-plans/**/_PLAN*.md`](process/general-plans/), [`process/context/all-context.md`](process/context/all-context.md))
      2. Relevant Test Suites (`tests/...`)
      3. Domain Models & Ports (`src/ai/domain/`, `src/ai/application/port_*`)
      4. Configuration & DI (`src/ai/infrastructure/config/`, `pyproject.toml`)
      5. Concrete Implementations (`src/ai/infrastructure/`, `src/ai/application/service/`, `src/ai/api/`)
    </information_priority>
  </pillar>

  <!-- Pillar 3: Engineering Harness & Guardrails -->
  <pillar id="harness" title="Engineering Harness & Guardrails">
    <validation_commands>
      uv run pytest
      uv run mypy --strict .
      uv run ruff check .
      uv run ruff format --check .
    </validation_commands>
    <architecture_guardrail>
      Clean Architecture & DI (`dependency-injector`) provide structural guidance, NOT an instruction to blindly over-engineer simple utilities.
    </architecture_guardrail>
  </pillar>
</foundations>

---

## 2. Operational Loop (Pillar 4: RIPER-5 Protocol)

<operational_loop>
  <phase order="1" name="Research (R)">
    Ingest active task spec, follow `<information_priority>`, establish domain invariants and boundaries.
  </phase>

  <phase order="2" name="Innovate & Plan (I)">
    Decompose into vertical slices (per [`process/context/planning/all-planning.md`](process/context/planning/all-planning.md)), define step-by-step plan and test cases.
  </phase>

  <phase order="3" name="Produce (P)">
    Implement scoped atomic edits with strict typing (`mypy --strict .`). Preserve public APIs and backward compatibility.
  </phase>

  <phase order="4" name="Evaluate (E)">
    Execute focused test (`uv run pytest path/to/test.py -k name`) $\rightarrow$ full suite (`uv run pytest`) $\rightarrow$ type check (`uv run mypy --strict .`) $\rightarrow$ lint & format (`uv run ruff check . && uv run ruff format --check .`).
  </phase>

  <phase order="5" name="Reconcile & Review (R)">
    Inspect `git diff` for zero noise. Safely toggle verified checkboxes (`- [x]`). Move completed task to `completed/`.
  </phase>

  <retry_budget max_attempts="3">
    Maximum 3 consecutive attempts per distinct failure symptom. Never bypass errors with `# type: ignore` or linter suppression. If exhausted, halt and log into `<open_decisions>`.
  </retry_budget>

  <escalation_and_stop_conditions>
    Halt immediately and request human guidance if:
    1. Retry budget is exhausted on a recurring failure.
    2. Modification requires undeclared changes to public APIs, DB schemas, or security policies.
    3. Modification requires touching files outside defined boundaries or in `<out_of_scope>`.
    *Exception:* Do not stop if the change was explicitly authorized by the user in the prompt/spec, or is a mandatory accompanying test/import update.
  </escalation_and_stop_conditions>

  <completion_gate>
    A task is COMPLETE only when:
    1. All Acceptance Criteria in the active task spec are verified (`- [x]`).
    2. All 4 validation commands execute with zero errors/warnings.
    3. The final `git diff` contains zero extraneous or unreviewed modifications.
  </completion_gate>
</operational_loop>

---

## 3. Workspace Protocols

<workspace_rules>
  <rule id="env">Execute all commands using `uv run <command>` inside `.venv`.</rule>
  <rule id="sync">Only toggle `- [x]` after Phase 4 (Evaluate) and Phase 5 (Reconcile) pass. Never overwrite human-written specs.</rule>
  <rule id="isolation">Keep edits within the `ai/` subsystem unless explicit cross-system coordination is requested.</rule>
  <rule id="subagents">Subagent delegation must adhere to [`process/development-protocols/orchestration.md`](process/development-protocols/orchestration.md).</rule>
</workspace_rules>

</agent_guidelines>
