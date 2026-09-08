# Repository Agent Guidelines (RIPER-5 Framework)

<agent_guidelines version="4.0">

<overview>
  Entry-point configuration for AI agents in this subsystem. Establishes
  project-specific settings and wires the two universal layers:
  - **`.agents/`** — stateless agent-control rules (behavior, guardrails, conventions)
  - **`process/`** — task workflow, artifact chain, and durable knowledge base

  This file ONLY contains what is unique to this project: toolchain commands,
  architecture guardrail calibration, and path references.
  Framework versioning and update procedures are managed via [`instruction-version.json`](instruction-version.json).
</overview>

---

## 1. Agent Control Layer Reference

<agent_control_ref>
  The universal agent control rules live in [`.agents/`](.agents/README.md):

  | File | What it governs |
  |------|----------------|
  | [`instruction-version.json`](instruction-version.json) | Framework version manifest, compatibility, and update strategy |
  | [`.agents/behavior.md`](.agents/behavior.md) | Mode declaration, session startup, context priority |
  | [`.agents/guardrails.md`](.agents/guardrails.md) | Retry budget, escalation triggers, completion gate |
  | [`.agents/conventions/naming.md`](.agents/conventions/naming.md) | Naming and structural hygiene |

  Read these files at session start. They require no project-specific edits.
</agent_control_ref>

---

## 2. Core Pillars

<foundations>
  <!-- Pillar 1: Task & Specification -->
  <pillar id="task_spec" title="Task & Specification">
    <rule>Single Source of Truth: Active task file in [`process/features/active/{feature}/task.md`](process/features/) or [`process/general-plans/active/{task}/task.md`](process/general-plans/) (instantiated from [`process/_seeds/task-template.md.seed`](process/_seeds/task-template.md.seed)).</rule>
    <rule>Prompt & Slash Command Initialization: Users can request and run tasks directly via prompt or pseudo-slash command prefixes. The Agent automatically routes the folder and instantiates `task.md` from [`process/_seeds/task-template.md.seed`](process/_seeds/task-template.md.seed):
      - Commands `/feature`, `/big-task`, `/epic` or keywords `big task`, `feature`, `big changes`: routes to `process/features/active/{task-slug}/task.md` (PAIR mode).
      - Commands `/task`, `/small-task`, `/bug` or keywords `small task`, `general changes`, `small changes`: routes to `process/general-plans/active/{task-slug}/task.md` (PAIR mode).
      - Command `/hotfix`: routes to `process/general-plans/active/{task-slug}/task.md` and runs in DELEGATED mode.
      - Commands `/fast-track`, `/delegated`: executes autonomously through all phases.
      - Command `/update`: checks for a new framework version (via `instruction-version.json` and registry); updates instructions if a newer version exists, or outputs "nothing changed" if already on the latest version.
      The Agent creates the directory, hydrates `<goal>` and `<acceptance_criteria>` from the prompt, and initiates the RESEARCH phase immediately.</rule>
    <rule>Define changes via Goal, Current Behavior, Expected Behavior, Invariants, and `<out_of_scope>`.</rule>
    <rule>Acceptance Criteria (AC) must be unambiguous, verifiable markdown checkboxes (`- [ ]`).</rule>
    <rule>Every task has a master contract (`task.md`) and a full artifact chain: `research.md → decision.md → plan.md → state.md → review.md → handoff.md`.</rule>
  </pillar>

  <!-- Pillar 2: Context Navigation -->
  <pillar id="context" title="Context Navigation">
    <rule>Gather minimum sufficient context. No full-repo scanning or drive-by refactoring.</rule>
    <rule>Follow information priority defined in [`.agents/behavior.md`](.agents/behavior.md).</rule>
    <rule>Project context routes via [`process/context/all-context.md`](process/context/all-context.md).</rule>
    <information_priority>
      1. Task Spec & AC ([`process/features/**`](process/features/), [`process/general-plans/**`](process/general-plans/), [`process/context/all-context.md`](process/context/all-context.md))
      2. Domain Models & Ports (`src/ai/domain/`, `src/ai/application/port_*`)
      3. Relevant Test Suites (`tests/...`)
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
      Clean Architecture & Dependency Injection (`dependency-injector`) provide structural guidance, NOT an instruction to blindly over-engineer simple utilities. Never use `# type: ignore` without explicit justification.
    </architecture_guardrail>
  </pillar>
</foundations>

---

## 3. RIPER-5 Operating Protocol (Pillar 4: Loop)

<riper5_protocol>

  <!-- ─────────────────── WORKING MODES ─────────────────── -->
  <working_modes>
    <mode id="PAIR" default="true">Step-by-step collaboration. Halts after each phase for human engineer review and gate sign-off.</mode>
    <mode id="DELEGATED" alias="fast-track,autonomous,skip-permissions">Autonomous delegation. When task.md has working_mode=DELEGATED or the user specifies fast-track / skip permissions, the agent auto-certifies qualifying gates ([AUTO: DELEGATED]), advances task.md current_phase, and executes continuously without pausing for "next phase" prompts.</mode>
  </working_modes>

  <!-- ─────────────────── PHASE CONSTRAINTS ─────────────────── -->

  <phase name="RESEARCH" order="1">
    <constraint>READ-ONLY. No source-code modifications.</constraint>
    <constraint>No implementation decisions.</constraint>
    <constraint>No architectural choices.</constraint>
    <output>Produce/update `research.md`. Classify evidence as Confirmed / Observed / Hypothesized.</output>
    <gate id="G0">All Research Exit Criteria in `research.md` checked before advancing. In DELEGATED / Fast-Track, the agent auto-certifies and advances to INNOVATE immediately.</gate>
  </phase>

  <phase name="INNOVATE" order="2">
    <constraint>READ-ONLY. No source-code modifications.</constraint>
    <constraint>Present 2–3 viable options with trade-off matrix.</constraint>
    <constraint>PAIR mode: Leave `<engineer_decision>` blank for the human engineer to review and complete.</constraint>
    <constraint>DELEGATED / Fast-Track mode: Automatically adopt the optimal Recommendation, record rationale with `[AUTO: DELEGATED]`, sign Gate 1, and immediately advance to PLAN.</constraint>
    <output>Produce `decision.md`.</output>
    <gate id="G1">Gate 1 in `decision.md` signed by engineer (PAIR) or auto-certified by agent with `[AUTO: DELEGATED]` (DELEGATED/Fast-Track) before advancing to Plan.</gate>
  </phase>

  <phase name="PLAN" order="3">
    <constraint>Plan artifacts only. No source-code changes.</constraint>
    <constraint>Every slice must have a defined verifier, expected evidence, and rollback point.</constraint>
    <constraint>Scope contract (allowed / forbidden files) must be explicit.</constraint>
    <output>Produce `plan.md`. Populate Verification Matrix headers.</output>
    <gate id="G2">Gate 2 in `plan.md` signed by engineer (PAIR) or auto-certified by agent with `[AUTO: DELEGATED]` (DELEGATED/Fast-Track) before advancing to Execute.</gate>
  </phase>

  <phase name="EXECUTE" order="4">
    <constraint>Read/write only within the scope approved in `plan.md`.</constraint>
    <constraint>One slice at a time. Run verifier after each slice. Inspect diff after each slice.</constraint>
    <constraint>No unrelated refactoring. No changes to forbidden files.</constraint>
    <constraint>Update `state.md` after every slice.</constraint>
    <output>Source code + tests + updated `state.md` with verification evidence. When all slices pass, advance directly to REVIEW.</output>
  </phase>

  <phase name="REVIEW" order="5">
    <constraint>READ-ONLY. May run verification commands.</constraint>
    <constraint>No code fixes during review. Log findings in `review.md` instead.</constraint>
    <constraint>Cover: behavior, architecture, data, security, regression.</constraint>
    <output>Produce `review.md` with findings classified by category/severity/type and Gate 3 checklist.</output>
    <gate id="G3">Gate 3 in `review.md` must PASS before handoff. In DELEGATED mode, the agent performs full audit, creates `handoff.md`, moves task to `completed/`, and reports completion.</gate>
  </phase>

  <!-- Behavior rules (mode declaration, retry, escalation, completion gate) →
       see .agents/behavior.md and .agents/guardrails.md -->

</riper5_protocol>

---

## 4. Workspace Protocols

<workspace_rules>
  <rule id="env">Execute all commands inside the designated runtime virtual environment / harness.</rule>
  <rule id="sync">Only toggle `- [x]` after the relevant verifier passes. Never overwrite human-written specs.</rule>
  <rule id="isolation">Keep edits within this subsystem unless explicit cross-system coordination is requested.</rule>
  <rule id="subagents">Subagent delegation must adhere to [`process/development-protocols/orchestration.md`](process/development-protocols/orchestration.md).</rule>
  <rule id="no_stale_context">Re-read relevant files after the repository changes. Do not rely on stale conversation context.</rule>
  <rule id="command_safety">Never run destructive commands (force push, hard reset, unconstrained rm -rf, DDL drops, secret inspection). See .agents/guardrails.md.</rule>
  <rule id="housekeeping">Remove all debug logs, scratch artifacts, and verify git diff cleanliness before Gate G3.</rule>
</workspace_rules>

</agent_guidelines>
