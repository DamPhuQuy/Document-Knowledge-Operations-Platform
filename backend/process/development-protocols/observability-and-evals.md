# Evaluation, Benchmarking & Cost Observability Protocol

<observability_and_evals_protocol version="1.0" framework="RIPER-5">

<description>
  Technical standard for monitoring token consumption, model compute budgets,
  execution latency, and quantitative benchmark evaluation frameworks (Evals / Pass@k).
</description>

---

## 1. Cost & Token Observability Governance

<token_cost_governance>
  Every agent session operates under finite token budgets and computational resource limits.

  ### Mandatory Tracked Metrics:
  - **Total Tool Calls:** Number of tool invocations within a task. Raise caution if a task exceeds 50 tool calls without slice completion.
  - **Estimated Input / Output Tokens:** Monitor token consumption to proactively prevent context window bloat.
  - **First-Pass Acceptance Rate:** Track whether slices pass verification on the first attempt or require auto-healing loops.
  - **Wall-Clock Latency:** Total execution time from requirement hydration to Gate 3 sign-off.

  ### Anti-Waste Guardrails:
  1. Never redundantly read unchanged files within the same active session.
  2. Enforce LSP `documentSymbols` or bounded range reading (`offset`/`limit`) for files exceeding 200 lines (with a hard ceiling at 350 lines for any single unconstrained read).
  3. When context consumption hits 60% of window capacity, initiate State Compaction into `state.md` and crystallize verified evidence.
</token_cost_governance>

---

## 2. Git-Atomic Commits & Sandboxing Policy

<atomic_commits_and_sandboxing>
  ### Git-Atomic Commit Convention:
  Upon each vertical slice passing its automated `<verifier>` command with exit code 0, the agent MUST execute an atomic git commit:
  ```bash
  git commit -m "<type>(<task-id>/slice-<index>): <short summary> [verifier: <cmd> (exit: 0)]"
  ```
  *(Example: `git commit -m "feat(CHG-001/slice-01): implement domain entity [verifier: npm test tests/unit.test.ts (exit: 0)]"`)*

  ### Git Worktree Sandboxing Policy:
  - When a subagent executes exploratory research spikes or risky experimental builds, isolate the workspace:
    ```bash
    git worktree add ../scratch-sandbox-<task-id> -b sandbox/<task-id>
    ```
  - After verification is completed and evidence is recorded into `state.md`, cleanly tear down the sandbox:
    ```bash
    git worktree remove ../scratch-sandbox-<task-id> --force
    git branch -D sandbox/<task-id>
    ```
</atomic_commits_and_sandboxing>

---

## 3. Benchmark & Quantitative Evaluation (Evals Framework)

<evals_framework>
  For performance optimization, architectural refactoring, or algorithmic upgrades:

  1. **Baseline Measurement:** Execute benchmark suite before source modifications and record in the `baseline` row of `results.tsv`.
  2. **Post-Innovation Measurement:** Execute identical benchmark in equivalent environment conditions (same CPU/memory, background processes closed).
  3. **Quantitative Gate Criteria:**
     - Reject regressions exceeding 2% throughput degradation or 5% p99 latency increase unless explicitly justified and approved at Gate 1.
</evals_framework>

---

## 4. Deterministic Governance Evals & Evidence Hygiene

<governance_evals>
  Use `process/evals/eval-case.json` as an offline, deterministic fixture. A case records input as data, expected policy verdict, expected side effects, and evidence references; it must never cause a command or MCP call to execute.
  Record only references, hashes, policy verdicts, and verifier exit codes in task artifacts. Never persist secrets, credentials, or raw private tool output.
</governance_evals>

</observability_and_evals_protocol>
