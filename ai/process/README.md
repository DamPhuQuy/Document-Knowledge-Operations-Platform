# Process & Task Orchestration (RIPER-5)

<process_orchestration version="2.0" framework="RIPER-5">

<overview>
  Standard template and operational control center for managing engineering tasks and coordinating AI agent execution in the `ai` subsystem.
</overview>

## 1. Directory Structure
```text
ai/process/
├── README.md                    # Operational guide (this file)
├── _seeds/                      # Read-only archetype blueprints and templates
│   ├── _GUIDE.md                # Guide on using seeds
│   ├── task-template.md.seed    # Archetype for single tasks (SPEC, PLAN, REPORT)
│   ├── program-template.md.seed # Archetype for multi-phase programs (Umbrella PLAN)
│   └── context-group.md.seed    # Archetype for domain context routers
├── context/                     # Durable knowledge base & context routers
│   ├── all-context.md           # Root context router
│   └── planning/all-planning.md # Planning calibration & vertical slicing rules
├── development-protocols/       # System rules & execution harness
│   ├── all-development-protocols.md
│   ├── orchestration.md         # Subagent delegation rules
│   └── implementation-standards.md # Typing, linting, and testing standards
├── features/                    # Domain features (≥5 files / ≥3 phases)
│   ├── active/                  # Active task folders: {task_slug}_{dd-mm-yy}/
│   ├── completed/               # Archived task folders
│   └── backlog/                 # Backlog notes: {note_slug}_NOTE_{dd-mm-yy}.md
└── general-plans/               # Cross-cutting & standalone tasks
    ├── active/
    ├── completed/
    └── backlog/
```

## 2. The RIPER-5 Operational Flow
<operational_phases>
  <phase order="1" name="Research">
    Ingest task spec and follow `<information_priority>` (Spec $\rightarrow$ Tests $\rightarrow$ Domain $\rightarrow$ Config $\rightarrow$ Target code).
  </phase>

  <phase order="2" name="Innovate & Plan">
    Decompose into vertical slices; populate the execution plan.
  </phase>

  <phase order="3" name="Produce">
    Apply minimal atomic edits with strict typing (`mypy --strict`).
  </phase>

  <phase order="4" name="Evaluate">
    Run `pytest`, `mypy --strict .`, `ruff check .`, `ruff format --check .`.
  </phase>

  <phase order="5" name="Reconcile & Review">
    Inspect `git diff`, update checkboxes (`- [x]`), verify all Acceptance Criteria.
  </phase>
</operational_phases>

</process_orchestration>
