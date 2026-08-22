# Repository Agent Guidelines

<agent_guidelines version="1.0">

## 1. Environment & Harness

<validation_commands>
uv run pytest
uv run mypy --strict .
uv run ruff check .
uv run ruff format --check .
</validation_commands>

<code_setup>The project uses aspects of programming by using dependency injection, follows controllers, services and repositories to use dependency injector library</code_setup>

<completion_gate>
Before marking any task as complete, execute all relevant validation commands.
A task is strictly incomplete if it introduces unresolved type, linting, formatting, or test failures.
</completion_gate>

---

## 2. Process & Workspace Protocol

<process_protocol>
<directory_structure>
Standard template for specifying new tasks.
Active and completed task specifications.
Architectural Decision Records defining system-wide invariants.
</directory_structure>

---

## 3. Context Navigation Flow

<context_navigation>

Gather minimum sufficient context to execute the smallest correct change. Avoid full-repo scanning.

<navigation_rules>
Prefer targeted searches (symbols, filenames, imports) over recursive directory scans.
Read corresponding tests before touching implementation code.
Inspect configuration and dependency manifests before assuming runtime capabilities.
Do not refactor unrelated code encountered during navigation.
</navigation_rules>

<context_boundary>
Halt context discovery once the target path, edge cases, invariants, and validation steps are established.
</context_boundary>
</context_navigation>

---

## 4. Operational Loop Protocol

<operational_loop>

Work in small, verifiable iterations: Plan → Implement → Validate → Sync State → Inspect → Stop.

```
<phase order="2" name="Implement">
  <action>Apply atomic edits adhering to strict typing (<code>mypy --strict</code>).</action>
  <action>Preserve existing public APIs and backward compatibility unless explicitly tasked to break them.</action>
  <action>Avoid speculative generalizations, dead code, and premature abstractions.</action>
</phase>

<phase order="3" name="Validate">
  <action>Run focused tests first: <code>uv run pytest path/to/test_file.py -k test_name</code>.</action>
  <action>Run static checks: <code>uv run mypy --strict .</code> and <code>uv run ruff check .</code>.</action>
</phase>

<phase order="4" name="Sync State">
  <action>Update checkboxes <code>[x]</code> and step statuses to <code>completed</code> in the active task spec file.</action>
</phase>

<phase order="5" name="Inspect">
  <action>Review <code>git diff</code> to ensure zero unintentional changes or formatting noise.</action>
  <action>Ensure changes satisfy acceptance criteria rather than merely silencing tool outputs.</action>
</phase>

```

<escalation_and_stop_conditions>
Same validation failure repeats more than 3 times consecutively (Retry Budget = 3).
Task requires modifying public APIs, DB schemas, or security policies not stated in Spec.
Fix requires modifying files outside defined task context boundaries or <out_of_scope>.
Stop immediately, log the blocker in process/tasks/[task-id].md under <open_decisions>, and request human guidance.
</escalation_and_stop_conditions>

<completion_protocol>
A task is complete only when:
All task acceptance criteria in process/tasks/[task-id].md are satisfied and marked done.
All relevant test suites pass via uv run pytest.
Strict type checking passes via uv run mypy --strict ..
Lint and format checks pass via uv run ruff check ..
The final diff contains zero extraneous modifications.
</completion_protocol>
</operational_loop>

</agent_guidelines>
