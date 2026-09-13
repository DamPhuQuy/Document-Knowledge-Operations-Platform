# LSP & MCP Integration Protocol (Language Server Protocol & Model Context Protocol)

<mcp_lsp_protocol version="1.0" framework="RIPER-5">

<description>
  Technical standard for leveraging Language Server Protocol (LSP) for static code analysis
  and Model Context Protocol (MCP) for secure peripheral tool integration with deterministic fallback.
</description>

---

## 1. Language Server Protocol (LSP) Standards for Coding Agents

<lsp_standards>
  Agents MUST prioritize abstract syntax tree (AST) static analysis capabilities via LSP over raw text grep when navigating codebase symbols:

  ### Standard LSP Actions & Use Cases:
  | LSP Operation | Intended Purpose | Anti-Pattern Replaced |
  |---|---|---|
  | `documentSymbols` | Retrieve symbol map of exported classes, functions, and interfaces | Ingesting full content of large files just to inspect layout |
  | `goToDefinition` | Jump directly to declaration of a function, type, or interface | Grepping symbol name and guessing among duplicate identifiers |
  | `findReferences` | Locate all callers and usages across repo (Impact Radius Analysis) | Brittle string matching that catches false positives or comments |
  | `diagnostics` | Read instant compiler errors, type warnings, and linter issues on save | Running full heavy test suite merely to catch syntax/type errors |
  | `hover` | Inspect function signature, parameter types, and docstrings | Reading definition file to check argument order |

  ### "AST-First" Navigation Rules:
  1. When exploring file structure: Call `documentSymbols` first. Only read full function bodies when implementation changes are required.
  2. When changing function signatures or deleting methods: You MUST run `findReferences` to populate `<impacted_files>` during Research/Plan.
  3. After editing files in Execute: Immediately inspect LSP `diagnostics` to resolve local type issues before running the slice verifier.
</lsp_standards>

---

## 2. Model Context Protocol (MCP) Governance & Security

<mcp_governance>
  When MCP servers are active in the environment, agents must strictly observe permission boundaries:

  ### Tool Classification:
  - **Class 1: Read-Only MCP — Permitted across ALL phases (Research, Plan, Execute, Review):**
    - *Database MCP (Query/Schema):* Inspect tables, columns, foreign keys, constraints. NEVER run mutating DDL/DML.
    - *Git MCP:* Read commit logs, branch status, structured diffs.
    - *Browser/DevTools MCP:* Inspect live DOM, accessibility tree, and console error logs.
    - *Issue Tracker MCP:* Ingest ticket descriptions and acceptance criteria.
  - **Class 2: Mutating MCP — STRICTLY RESTRICTED to EXECUTE Phase:**
    - Writing data, mutating external state. Must be explicitly vetted against `<scope_contract>`.

  ### Safe Operational Rules:
  - Never pass raw secrets, API keys, or cloud credentials into MCP tool arguments.
  - Large payloads returned by MCP queries must be bounded with strict limits (`LIMIT` / pagination) to avoid saturating context window.
</mcp_governance>

---

## 3. Deterministic Fallback Engine

<fallback_engine>
  An agent must NEVER halt or error out simply because an LSP server or MCP connection is unavailable. Always apply prioritized fallbacks:

  ```text
  [LSP / MCP Unavailable]
         │
         ├──► Fallback for Code Navigation:
         │      1. Use ast-grep (if available in environment) for structural AST queries.
         │      2. Use targeted regex grep_search (e.g., `(function|class|def)\s+Name`).
         │      3. Perform targeted line-range reads (offset/limit), avoiding bulk file reads.
         │
         └──► Fallback for Data & Infrastructure Inspection:
                1. Inspect static migration files (`migrations/`, `.sql`, schema files).
                2. Inspect environment samples (`.env.example`, `docker-compose.yml`).
                3. Run deterministic CLI test commands (`npm test`, `pytest`, `cargo test`).
  ```
</fallback_engine>

</mcp_lsp_protocol>
