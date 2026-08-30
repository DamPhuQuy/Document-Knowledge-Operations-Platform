# Use Case Specifications: Conversational AI (`Conversational_AI`)
## Bounded Context 4

> **Source of Truth:** Complete Specification for Conversational AI & Citation Verification Use Cases (`UC-CHAT-01`, `UC-CHAT-02`, `UC-CHAT-03`).
> **Orchestrated by:** [`../MVP.md`](../MVP.md)

---

### Use Case Specification: `UC-CHAT-01`
- **Use Case Name:** Multi-Turn Conversational RAG with Grounded Citations
- **Stereotype:** Base Use Case
- **Actor(s):** Knowledge Worker / Auditor (primary), LLM API (`EXT-02`) (secondary)
- **Includes:** `UC-RAG-02` (Pre-filtered Hybrid Search), `UC-CHAT-03` (Token Usage & Confidence Tracking)
- **Extended By:** `UC-CHAT-02` (Evidence Citation Drill-Down) at Extension Point `User Clicks Citation Badge`
- **Summary Description:** Manages conversational sessions, maintaining multi-turn context and generating grounded, factual responses with inline citations using verified retrieved document chunks.
- **Priority:** Must Have
- **Status:** Complete Specification
- **Pre-Condition:**
  1. User has `read:documents` permission and an active conversation session.
- **Post-Condition(s):**
  1. User prompt and AI response stored in `conversation_messages`.
  2. Evidence citations linking chunks to the message stored in `message_citations`.
  3. Token usage and confidence level logged via `UC-CHAT-03`.
- **Basic Path:**
  1. User enters a query in active conversation window.
  2. Client sends request to `POST /api/v1/conversations/{id}/messages`.
  3. Backend loads user context and recent message history (last 3-5 turns).
  4. Backend invokes `UC-RAG-02` to execute Pre-filtered Hybrid Search.
  5. AI Service constructs Prompt Contract with System instructions, Context chunks, and formatting constraints.
  6. LLM Provider (`EXT-02`) generates grounded answer with inline citation tags (e.g., `[1]`, `[2]`).
  7. Backend persists message in `conversation_messages`.
  8. Backend inserts citation records into `message_citations`.
  9. Backend invokes `UC-CHAT-03` to record token metrics and confidence rating.
  10. Backend streams/returns complete answer and citation list to client.
- **Alternative Paths:**
  - 4a. Zero chunks retrieved: `UC-RAG-03` triggers abstention and returns refusal payload.
  - 6a. LLM provider error/timeout: Backend retries with fallback model or returns structured error message.
- **Business Rules:**
  - B1: Every factual claim in the response must correspond to an indexed citation.
  - B2: System prompt must explicitly instruct model not to use prior knowledge outside provided context.
- **Non-Functional Requirements:**
  - NF1: Time to First Token (TTFT) via Server-Sent Events (SSE) $< 800\text{ ms}$.

---

### Use Case Specification: `UC-CHAT-02`
- **Use Case Name:** Evidence Citation Drill-Down & Verification
- **Stereotype:** Extension Use Case
- **Actor(s):** Knowledge Worker / Compliance Auditor (primary), S3 Storage (`EXT-01`) (secondary)
- **Extends:** `UC-CHAT-01` (Multi-Turn Conversational RAG with Grounded Citations)
- **Extension Point:** `User Clicks Inline Citation Badge`
- **Condition:** Executed on-demand when a user clicks on an inline citation badge `[1]` in the chat UI to inspect evidence.
- **Includes:** None
- **Summary Description:** Allows users to inspect citation badges attached to AI answers, viewing verbatim text snippets, document title, page numbers, similarity score, and opening the original PDF page.
- **Priority:** Must Have
- **Status:** Complete Specification
- **Pre-Condition:**
  1. User is viewing an AI response containing citations.
  2. User has permission to read the cited document.
- **Post-Condition(s):**
  1. Source document snippet and PDF page preview are rendered to the user.
- **Basic Path:**
  1. User clicks on citation badge `[1]` next to an answer sentence.
  2. UI displays citation drawer showing document title, version number, page number, relevance score, and verbatim snippet.
  3. User clicks "View Source PDF".
  4. System verifies document ACL and generates temporary signed S3 URL from S3 Storage (`EXT-01`).
  5. UI opens PDF preview focused directly on the cited page.
- **Alternative Paths:**
  - 4a. User's permission was revoked after message generation: System denies access with HTTP 403 Forbidden.
- **Business Rules:**
  - B1: Citation snippet must match raw chunk text in `document_chunks.content`.
- **Non-Functional Requirements:**
  - NF1: Citation drawer load time $< 50\text{ ms}$.

---

### Use Case Specification: `UC-CHAT-03`
- **Use Case Name:** Token Usage & Confidence Tracking
- **Stereotype:** Included Use Case
- **Actor(s):** System Administrator (primary), AI Monitoring Subsystem (secondary)
- **Includes:** None
- **Extends / Extended By:** None (Included by `UC-CHAT-01`)
- **Summary Description:** Tracks input prompt tokens, completion tokens, execution latency, and assigns confidence ratings (`LOW`, `MEDIUM`, `HIGH`) to every AI interaction.
- **Priority:** Should Have
- **Status:** Complete Specification
- **Pre-Condition:**
  1. An AI message completion event is processed.
- **Post-Condition(s):**
  1. `conversation_messages` updated with `prompt_tokens`, `completion_tokens`, and `confidence`.
- **Basic Path:**
  1. AI Service parses token usage metadata from LLM provider response.
  2. AI Service calculates confidence rating based on top retrieval cosine similarity scores.
  3. AI Service returns token counts and confidence with response payload.
  4. Backend persists values into database columns.
- **Alternative Paths:**
  - 1a. Token usage metadata missing: System estimates token count using standard tokenizer.
- **Business Rules:**
  - B1: `confidence = HIGH` if top chunk similarity $> 0.82$.
  - B2: `confidence = MEDIUM` if top chunk similarity is between $0.65$ and $0.82$.
  - B3: `confidence = LOW` if top chunk similarity is between $0.50$ and $0.64$.
- **Non-Functional Requirements:**
  - NF1: Zero overhead added to conversational response delivery.
