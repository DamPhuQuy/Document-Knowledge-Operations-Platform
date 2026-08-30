# Seeds & Archetypes Scaffolding Guide

<seeds_guide version="1.0">

<scope>
  `_seeds/` is a read-only scaffolding directory containing blueprint templates.
  Copy and instantiate these seeds when defining new tasks, programs, or context routers.
</scope>

## 1. Blueprint Catalog
<catalog>
  <seed type="task" path="task-template.md.seed">
    Standard single-task template covering `_SPEC`, `_PLAN`, `_REPORT`, and verifiable Acceptance Criteria.
  </seed>
  
  <seed type="program" path="program-template.md.seed">
    Multi-phase program blueprint with umbrella planning, phase breakdown, and blast radius registries.
  </seed>

  <seed type="context" path="context-group.md.seed">
    Template for creating domain-specific context routers (`all-{group}.md`).
  </seed>
</catalog>

## 2. Instantiation Command
```bash
cp process/_seeds/task-template.md.seed process/features/your-feature/active/your-task_PLAN_$(date +%d-%m-%y).md
```

</seeds_guide>
