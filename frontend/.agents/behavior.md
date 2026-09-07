# Agent Behavior Protocol

<behavior_protocol version="1.0">

<description>
  Universal rules governing HOW the agent declares its operating mode,
  reloads state at session start, and navigates context. Project-agnostic.
</description>

---

## 1. Mode Declaration

Every response that advances a task MUST open with a mode declaration on the
first line. The declared mode must match the current phase recorded in `task.md`.

```
[MODE: RESEARCH]   — read-only, no source changes
[MODE: INNOVATE]   — generating options, no source changes
[MODE: PLAN]       — writing plan artifacts, no source changes
[MODE: EXECUTE]    — making scoped source changes
[MODE: REVIEW]     — reviewing, no code fixes
```

Omit the mode declaration only for pure conversational exchanges that do not
advance a task (e.g., answering a factual question, clarifying scope).

In continuous autonomous execution (DELEGATED / Fast-Track), the agent opens with the starting phase's mode. When transitioning across phases within a single turn, emit an explicit transition marker:
`>>> [PHASE TRANSITION: <OLD_PHASE> -> <NEW_PHASE>]` and proceed under the new mode immediately without pausing.

---

## 2. Working Modes & Phase Transition Protocol

<working_modes_protocol>
  Execution posture is determined by `<working_mode>` in `task.md` or user prompt instruction:

  ### PAIR Mode (Default — Step-by-Step Collaboration):
  - The agent works on one phase at a time.
  - Upon completing a phase, it updates the corresponding artifact and **HALTS** to let the human engineer inspect, discuss, and sign off the Gate (G1, G2, G3).
  - Waits for user prompt (e.g., "Start next phase") before updating `<current_phase>` in `task.md` and continuing.

  ### DELEGATED Mode (Autonomous / Fast-Track / Skip Permissions):
  - **Activation:** `<working_mode>DELEGATED</working_mode>` in `task.md` OR explicit prompt directive ("fast-track", "skip permissions", "run automatically", "auto-advance", "autonomous").
  - **Core Rule:** **DO NOT HALT AFTER EACH PHASE TO WAIT FOR USER PROMPT "NEXT".**
  - **Continuous Transition Workflow:**
    1. When the current phase meets its exit criteria/checklist, the agent checks `- [x]`.
    2. Populates auto-approval into the artifact: `approved_by: [AUTO: DELEGATED]` with timestamp and technical rationale (in INNOVATE: adopts the optimal Recommendation; in PLAN: locks the scope contract).
    3. Immediately updates `<current_phase>` in `task.md` to the next phase (`RESEARCH` → `INNOVATE` → `PLAN` → `EXECUTE` → `REVIEW`).
    4. Instantiates the next phase seed artifact (Copy-On-Demand) and **CONTINUES EXECUTION IMMEDIATELY** within the same session/turn.
  - **Sole Stop Conditions in DELEGATED:**
    - Task is 100% COMPLETE (Gate 3 PASS, housekeeping cleaned, `handoff.md` generated, moved to `completed/`).
    - OR a true Escalation Trigger is tripped (retry budget exhausted after 3 attempts, destructive command, or unresolvable invariant conflict).

  ### MANUAL & DIAGNOSE-ONLY Modes:
  - `MANUAL`: Human leads command-by-command; agent provides scoped assistance.
  - `DIAGNOSE-ONLY`: Runs Research & Review for root-cause audit without mutating source code.
</working_modes_protocol>

---

## 3. Session Startup Protocol

Before continuing any in-progress task, reload persistent state in this order:

<startup_sequence>
  1. Read `task.md` — confirm current phase and open gates.
  2. Read `research.md` if Research phase is complete.
  3. Read `decision.md` if Innovate phase is complete — confirm approved decisions.
  4. Read `plan.md` — confirm current slice index and scope contract.
  5. Read `state.md` — confirm completed slices, failure memory, retry budget, next action.
  6. Re-read any source files that changed since last context load.
</startup_sequence>

**Do NOT rely on conversation memory alone.** Always verify against the
file-based artifacts listed above.

---

## 4. Context Navigation Rules

<context_rules>
  <rule id="minimum_context">
    Gather minimum sufficient context only. Never scan the full repository or
    perform drive-by refactoring outside the active task scope.
  </rule>

  <rule id="information_priority">
    Load context in this priority order:
    1. Task spec and acceptance criteria (`task.md`)
    2. Research and decision artifacts (`research.md`, `decision.md`)
    3. Relevant test suites
    4. Domain models and port interfaces
    5. Configuration and dependency injection setup
    6. Concrete infrastructure implementations
  </rule>

  <rule id="no_stale_context">
    Re-read relevant files after any repository change. Never act on stale
    in-memory snapshots.
  </rule>
</context_rules>

</behavior_protocol>
