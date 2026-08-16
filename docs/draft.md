Đúng. Với roadmap hiện tại, mình sẽ **không học tuần tự từng section nữa**. File `.typ` nên đóng vai trò **knowledge map để tra cứu**, còn đường học chính nên đổi thành **một project duy nhất phát triển theo vertical slice**.

## Project mình đề xuất: **AI Knowledge & Action Assistant**

Một AI assistant cho một tổ chức nhỏ, có thể:

> hỏi tài liệu → retrieve evidence → trả lời có citation → nhớ conversation → gọi tool → thực hiện workflow → xin approval → đánh giá → trace/debug.

Nó đủ nhỏ để một người làm được nhưng đủ rộng để chạm gần như toàn bộ phần quan trọng của AI Application Engineering.

Kiến trúc cuối cùng sẽ gần như:

```text
Client
  │
  ▼
FastAPI
  │
  ├── Auth / Tenant
  │
  ├── Conversation State
  │
  ▼
AI Application Service
  │
  ├── Context Builder
  ├── Prompt
  ├── LLM
  ├── RAG ─────────── PostgreSQL + pgvector
  ├── Tools
  ├── Agent Loop
  ├── Memory
  └── Guardrails
          │
          ▼
     Approval / Action

Cross-cutting:
Eval + Tests + Tracing + Security + Cost/Latency
```

Điểm quan trọng là **không build toàn bộ architecture ngay từ đầu**.

---

# Cách học mới: 8 vertical slices

## Slice 1 — LLM API thật sự

Build:

```http
POST /chat
```

```text
user → FastAPI → LLM → response
```

Không framework agent. Không RAG. Không vector DB.

Trong lúc build mới học:

- LLM mental model
- token / tokenizer
- context window
- system/user/assistant message
- hallucination
- inference
- temperature
- max output tokens
- provider SDK
- async/await
- timeout
- retry
- API key
- Pydantic
- structured output

Ví dụ response:

```python
class AssistantResponse(BaseModel):
    answer: str
    confidence: Literal["low", "medium", "high"]
```

### Bạn học vì gặp vấn đề

Ví dụ:

> Vì sao response không deterministic?

→ temperature / sampling.

> Vì sao request quá dài?

→ token / context window.

> Vì sao JSON lúc đúng lúc sai?

→ structured output / schema.

Đây mới là project-based learning.

---

# Slice 2 — Prompt như software contract

Bây giờ assistant phải làm một task rõ ràng.

Ví dụ:

> AI assistant hỗ trợ nhân viên tìm hiểu tài liệu nội bộ.

Thêm:

```text
Task
Input
Context
Constraints
Output schema
Abstention rule
```

Học:

- prompt contract
- instruction hierarchy
- delimiter
- few-shot
- negative example
- decomposition
- prompt versioning
- prompt injection
- regression test

Không ngồi học `"zero-shot vs few-shot"` riêng.

Bạn thử prompt → nó fail → tìm hiểu concept → sửa → viết test.

---

# Slice 3 — RAG

Đây sẽ là phần lớn đầu tiên của project.

Cho khoảng:

```text
20–30 Markdown/PDF/TXT documents
```

Pipeline:

```text
document
   ↓
parse
   ↓
chunk
   ↓
embedding
   ↓
pgvector
```

Query:

```text
question
   ↓
embedding
   ↓
retrieve
   ↓
context assembly
   ↓
LLM
   ↓
answer + citations
```

Lúc này tự nhiên phải học:

### Embedding

- embedding model
- vector
- cosine similarity
- dot product
- dimensions

### Retrieval

- top-k
- lexical search
- dense search
- hybrid retrieval
- metadata filtering

### RAG

- parsing
- chunking
- chunk overlap
- metadata
- document identity
- freshness
- citation
- grounding
- answerability
- abstention

### Security

- document prompt injection
- tenant isolation
- ACL

Đây là nơi rất nhiều keyword trong roadmap **tự nhiên kết nối với nhau**.

---

# Slice 4 — Evaluation

Đừng đợi project xong mới học eval.

Tạo khoảng:

```text
20–30 questions
```

gồm:

```text
normal questions
no-answer questions
ambiguous questions
wrong-document questions
prompt-injection questions
ACL questions
```

Pipeline:

```text
test case
   ↓
retrieval
   ↓
generation
   ↓
evaluation
```

Lúc này mới học:

- golden set
- regression evaluation
- Recall@k
- citation correctness
- groundedness
- exact match
- semantic evaluation
- LLM-as-judge
- latency
- token usage
- cost/task

Sau đó mỗi thay đổi:

```text
prompt
chunking
embedding model
retrieval
model
```

đều phải chạy eval.

**Đây là một trong những tư duy quan trọng nhất của AI Engineering.**

---

# Slice 5 — Conversation + Context Engineering

Thêm:

```text
POST /conversations/{id}/messages
```

Vấn đề xuất hiện:

```text
history ngày càng dài
        ↓
context window
        ↓
latency/cost tăng
```

Bây giờ học:

- conversation history
- session state
- context budget
- sliding window
- compaction
- summarization
- context priority
- state vs memory vs knowledge

Context builder:

```text
system instructions
        +
user/session information
        +
conversation history
        +
retrieved evidence
        +
tool results
        ↓
     context
```

Bạn sẽ hiểu **Context Engineering** sâu hơn nhiều so với học định nghĩa riêng.

---

# Slice 6 — Tool Calling

Cho assistant thêm capability thật.

Ban đầu chỉ read-only:

```text
search_documents()
get_document()
calculator()
get_current_ticket()
```

Model chỉ:

```text
decide tool
     ↓
generate arguments
```

Application mới thực thi:

```text
validate
authorize
execute
return result
```

Học:

- tool schema
- JSON Schema
- tool selection
- argument validation
- tool result
- timeout
- error handling
- tool output injection
- least privilege
- idempotency

Architecture quan trọng:

```text
LLM ≠ tool executor
```

Model chỉ **đề xuất action**.

Application vẫn kiểm soát quyền.

---

# Slice 7 — Từ AI Application → Basic Agent

Lúc này mới agentize.

Không LangGraph trước.

Tự viết:

```python
while state.steps < MAX_STEPS:
    observation = observe(state)
    decision = await decide(observation)

    if decision.type == "answer":
        return finish(decision)

    result = await execute_tool(decision.tool)
    state = update_state(state, result)
```

Mental model:

```text
Observe
   ↓
Decide
   ↓
Act
   ↓
Verify
   ↓
Stop / Continue
```

Lúc này học:

- agent
- state
- agent loop
- stop condition
- max steps
- max tool calls
- deadline
- cost budget
- retry
- replan
- escalation
- progress invariant
- loop detection

Đây là lý do mình rất đồng ý với hướng mới của bạn:

**agent không còn là một chapter lý thuyết.**

Bạn sẽ thấy:

> À, agent thực chất là application-controlled loop quanh model + state + tools.

---

# Slice 8 — Mutation + Human Approval

Thêm tool nguy hiểm hơn, nhưng vẫn chỉ simulated/internal:

```text
create_ticket()
update_ticket()
close_ticket()
```

Không cho agent chạy trực tiếp.

Flow:

```text
Agent
  ↓
prepare action
  ↓
preview
  ↓
WAITING_APPROVAL
  ↓
human approve
  ↓
revalidate
  ↓
commit
```

Từ đây học cực kỳ tự nhiên:

- human-in-the-loop
- approval
- risk tiers
- dry-run
- prepare → approve → commit
- idempotency
- audit
- state persistence
- stale approval
- authorization
- compensation

Đây đã là một **AI Agent system tương đối nghiêm túc**.

---

# Slice 9 — Productionization

Mình coi đây là phần cuối project chứ không phải một môn riêng.

Thêm từng thứ khi project bắt đầu có failure:

```text
Streaming
Tracing
Rate limiting
Retry/backoff
Cancellation
Concurrency limit
Provider adapter
Model fallback
Redis cache
Async ingestion
```

Theo dõi:

```text
request
├─ model span
├─ retrieval span
├─ database span
└─ tool span
```

Metrics:

```text
p50 / p95 latency
TTFT
token/request
cost/task
retrieval Recall@k
tool error rate
groundedness
task success
```

Học:

- OpenTelemetry
- trace/span
- correlation ID
- model version
- prompt version
- index version
- failure taxonomy
- rollback
- circuit breaker
- budgets

---

# Cuối project bạn sẽ chạm được

Từ roadmap của bạn:

```text
LLM Foundations                 ✓
Prompt Engineering              ✓
Structured Output               ✓

Embeddings                      ✓
Search                          ✓
RAG                             ✓
Grounding                       ✓

Conversation State              ✓
Context Engineering             ✓
Memory fundamentals             ✓

Tool Calling                    ✓
Tool Engineering                ✓

Agent Loop                      ✓
Agent State                     ✓
Stop Conditions                 ✓
Retry / Replan                  ✓

Human-in-the-loop               ✓
Guardrails                      ✓
Permissions                     ✓

Evaluation                      ✓
Observability                   ✓
Security                        ✓

FastAPI                         ✓
PostgreSQL                      ✓
pgvector                        ✓
Redis                           ✓
asyncio                         ✓
Pydantic                        ✓
pytest                          ✓
OpenTelemetry                   ✓
```

Đây gần như toàn bộ **critical path của AI Application / Basic Agent Engineering** trong file của bạn.

---

## Những thứ mình chủ động KHÔNG đưa vào project ban đầu

Đây là phần rất quan trọng.

Không ép học:

```text
Transformer mathematics
Q/K/V mathematics
backprop mathematics

fine-tuning
LoRA
quantization
model serving

LangChain
LangGraph

MCP

multi-agent

advanced graph orchestration
durable distributed agents

advanced vector index tuning
HNSW internals

Kafka / distributed queues

complex MLOps
```

Không phải vì chúng không quan trọng.

Mà vì:

> **chưa có problem → chưa có reason để học.**

Ví dụ một ngày project cần pause agent vài giờ rồi resume:

→ checkpoint/durable graph trở nên cần thiết.

Khi đó học LangGraph.

Có 20 external tools từ nhiều service:

→ MCP bắt đầu có ý nghĩa.

Model không đáp ứng domain-specific behavior dù RAG/prompt đã tối ưu:

→ investigate fine-tuning.

Đó chính xác là **on-demand learning**.

---

# Repository mình khuyên dùng

Không over-engineer Clean Architecture ngay.

```text
ai-assistant/
│
├── app/
│   ├── api/
│   │   ├── chat.py
│   │   └── documents.py
│   │
│   ├── llm/
│   │   ├── client.py
│   │   ├── prompts.py
│   │   └── schemas.py
│   │
│   ├── rag/
│   │   ├── ingestion.py
│   │   ├── chunking.py
│   │   ├── embeddings.py
│   │   └── retrieval.py
│   │
│   ├── tools/
│   ├── agent/
│   ├── memory/
│   ├── evals/
│   ├── observability/
│   └── security/
│
├── tests/
├── evals/
├── documents/
│
├── pyproject.toml
└── docker-compose.yml
```

Để architecture **tiến hóa cùng kiến thức**, thay vì thiết kế một hệ thống khổng lồ trước khi hiểu vấn đề.

---

# Quy tắc học từ giờ

Mình sẽ đổi hoàn toàn thứ tự trong roadmap thành:

```text
Build
 ↓
Encounter problem
 ↓
Identify concept
 ↓
Learn minimum theory
 ↓
Implement
 ↓
Break it intentionally
 ↓
Test / Eval / Trace
 ↓
Continue project
```

thay vì:

```text
Learn concept A
Learn concept B
Learn concept C
...
hope someday they connect
```

Và với mỗi keyword chỉ cần trả lời 5 câu trong chính file của bạn:

```text
1. Nó giải quyết problem nào?
2. Nó nằm ở đâu trong flow?
3. Code/API nào đại diện cho nó?
4. Nó fail thế nào?
5. Tôi chứng minh nó hoạt động bằng test/eval/trace nào?
```

Đây mới nên là **cách sử dụng `AIEngineeringKeywordLearner.typ` từ bây giờ**.

### Một thay đổi nữa mình khuyên

Đừng đặt target kiểu:

> “Hoàn thành section RAG.”

Hãy đặt target:

> “Assistant trả lời được từ knowledge base, citation đúng, không answer khi evidence không đủ, và không leak document tenant khác.”

Knowledge trở thành **phương tiện để đạt engineering outcome**, không phải checkbox.

Với phạm vi hiện tại, mình sẽ coi **Slice 1 → Slice 8 là project chính**, còn Slice 9 là production-hardening. Sau khi hoàn thành nó, bạn đã có nền đủ mạnh để quyết định xem bước tiếp theo thực sự cần **advanced RAG, LangGraph, MCP, multi-agent hay model adaptation**, thay vì học chúng chỉ vì chúng nằm trên roadmap.
