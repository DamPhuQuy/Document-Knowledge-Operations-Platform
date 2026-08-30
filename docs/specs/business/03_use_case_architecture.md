# 3. Use Case Modeling & Architectural Decomposition
## Enterprise Document Knowledge & Operations Platform

> **Source of Truth:** OMG UML 2.5 Modeling Standards, Macro Context Boundary, Master Decomposed Global Use Case Map, and Subsystem Architectural Diagrams.
> **Orchestrated by:** [`MVP.md`](MVP.md)

---

## 1. Standard UML 2.5 Use Case Conventions & Notation Reference

To ensure strict compliance with **OMG UML 2.5 Use Case Modeling Standards**, the diagrams in this specification adhere to the following semantic rules:

```text
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                   UML USE CASE RELATIONSHIP STANDARDS                                  │
├───────────────────┬──────────────────────────────────┬─────────────────────────────────────────────────┤
│ Relationship Type │ UML Visual Syntax                │ Exact Standard Semantics & Rules                │
├───────────────────┼──────────────────────────────────┼─────────────────────────────────────────────────┤
│ Association       │ Actor ──── UseCase               │ Solid line representing communication.          │
│                   │                                  │ Actors are outside the system boundary.         │
├───────────────────┼──────────────────────────────────┼─────────────────────────────────────────────────┤
│ «include»         │ BaseUseCase -. «include» .->     │ Mandatory execution. The Base Use Case ALWAYS   │
│                   │ IncludedUseCase                  │ calls the Included Use Case. Arrow points       │
│                   │                                  │ FROM Base TO Included (Base -> Included).       │
├───────────────────┼──────────────────────────────────┼─────────────────────────────────────────────────┤
│ «extend»          │ ExtensionUseCase -. «extend» .-> │ Conditional / optional execution. The Extension │
│                   │ BaseUseCase                      │ Use Case augments the Base Use Case under a     │
│                   │                                  │ specific Extension Point/Condition. Arrow points│
│                   │                                  │ FROM Extension TO Base (Extension -> Base).     │
├───────────────────┼──────────────────────────────────┼─────────────────────────────────────────────────┤
│ Generalization    │ SubActor ───|> BaseActor         │ Inheritance of roles or use cases. Solid line   │
│                   │ ChildUC ───|> ParentUC           │ with closed triangular arrow pointing to parent.│
└───────────────────┴──────────────────────────────────┴─────────────────────────────────────────────────┘
```

> [!IMPORTANT]
> **Strict UML Modeling Rules Applied:**
> 1. **Direction of `«include»`:** Points from the base use case to the included use case (`Base -. "«include»" .-> Included`).
> 2. **Direction of `«extend»`:** Points from the extending use case to the base use case (`Extension -. "«extend»" .-> Base`).
> 3. **No Non-Standard Stereotypes:** Deprecated custom stereotypes (such as `<<persists>>`, `<<calls>>`, `<<trigger>>`, `<<acts on>>`) are strictly replaced with standard UML Actor Associations or valid `«include»`/`«extend»` relationships.
> 4. **External Systems as Actors:** Third-party providers (`S3 Storage`, `LLM API`) are modeled as Secondary Actors outside the system boundary with solid association lines (`UseCase ──── SystemActor`).

---

## 2. High-Level Context Use Case Diagram (Macro Capabilities)

The context use case diagram captures the high-level system boundary, primary human actors, secondary external systems, and macro-level capabilities:

```mermaid
flowchart LR
    %% Primary Human Actors (Left)
    subgraph Human_Actors ["Primary Human Stakeholders"]
        Admin(["System Administrator"]):::actor
        Manager(["Department Manager"]):::actor
        Staff(["Knowledge Worker / Staff"]):::actor
        Auditor(["Compliance Auditor"]):::actor
        Customer(["External Client"]):::actor
    end

    %% System Boundary & Macro Use Cases
    subgraph System_Boundary ["Document Knowledge & Operations Platform Boundary"]
        direction TB
        M_IAM(["UC-MACRO-01: Authenticate & Manage Identity"]):::macro
        M_DOC(["UC-MACRO-02: Manage Document Lifecycle & Storage"]):::macro
        M_RAG(["UC-MACRO-03: Ingest Knowledge & Index Vectors"]):::macro
        M_CHAT(["UC-MACRO-04: Grounded Conversational AI & Citations"]):::macro
        M_WF(["UC-MACRO-05: Automate Document Processing Workflows"]):::macro
        M_HITL(["UC-MACRO-06: Govern Operational Approvals (HITL)"]):::macro
        M_AUDIT(["UC-MACRO-07: Track Security Audit Trail & Notifications"]):::macro
    end

    %% Secondary Supporting System Actors (Right)
    subgraph Supporting_Systems ["Secondary External Systems"]
        S3[("S3 Object Storage\n(EXT-01)")]:::system
        LLM[("Embedding & LLM API\n(EXT-02)")]:::system
    end

    %% Actor to Macro Associations
    Admin --- M_IAM
    Admin --- M_WF
    Admin --- M_HITL
    Admin --- M_AUDIT

    Manager --- M_DOC
    Manager --- M_CHAT
    Manager --- M_HITL
    Manager --- M_WF

    Staff --- M_IAM
    Staff --- M_DOC
    Staff --- M_CHAT
    Staff --- M_HITL

    Auditor --- M_CHAT
    Auditor --- M_AUDIT

    Customer --- M_IAM
    Customer --- M_DOC
    Customer --- M_CHAT

    %% Secondary Actor Associations
    M_DOC --- S3
    M_RAG --- LLM
    M_CHAT --- LLM

    %% High-level Include Dependencies
    M_CHAT -. "«include»" .-> M_RAG
    M_WF -. "«include»" .-> M_RAG
    M_DOC -. "«include»" .-> M_AUDIT
    M_HITL -. "«include»" .-> M_AUDIT

    classDef actor fill:#f8f9fa,stroke:#495057,stroke-width:2px,color:#212529;
    classDef macro fill:#e7f5ff,stroke:#1c7ed6,stroke-width:2px,color:#1864ab;
    classDef system fill:#f3f0ff,stroke:#7950f2,stroke-width:2px,color:#5f3dc4;
```

---

## 3. Master Decomposed Use Case Map (Global Architecture)

The macro capabilities are decomposed into **20 concrete functional use cases** organized across **7 Bounded Contexts**, featuring fully standardized `«include»` and `«extend»` relationships with explicit condition points:

```mermaid
flowchart TB
    %% Human Actors
    Admin(["System Admin"]):::actor
    Manager(["Department Manager"]):::actor
    Staff(["Knowledge Worker"]):::actor
    Auditor(["Compliance Auditor"]):::actor

    %% System Actors
    Worker(["AI Ingestion Worker"]):::sysactor
    Engine(["Hybrid RAG Engine"]):::sysactor
    Orchestrator(["Workflow Orchestrator"]):::sysactor
    AuditSub(["Audit Subsystem"]):::sysactor

    %% Bounded Context 1: IAM
    subgraph BC_IAM ["1. Identity & Access Management (IAM)"]
        UC_IAM_01(["UC-IAM-01: User Login & JWT Session"])
        UC_IAM_02(["UC-IAM-02: Multi-Role Assignment"])
        UC_IAM_03(["UC-IAM-03: Department Scoping"])
    end

    %% Bounded Context 2: Document Management
    subgraph BC_DOC ["2. Document Management (DMS)"]
        UC_DOC_01(["UC-DOC-01: Document Upload & S3 Storage"])
        UC_DOC_02(["UC-DOC-02: Manage Document Versioning"])
        UC_DOC_03(["UC-DOC-03: Configure ACL Matrix"])
        UC_DOC_04(["UC-DOC-04: Document Soft Deletion"])
    end

    %% Bounded Context 3: AI Knowledge & RAG
    subgraph BC_RAG ["3. AI Knowledge & Hybrid RAG"]
        UC_RAG_01(["UC-RAG-01: Ingestion & Vector Indexing"])
        UC_RAG_02(["UC-RAG-02: Pre-filtered Hybrid Search"])
        UC_RAG_03(["UC-RAG-03: Anti-Hallucination Safe Abstention"])
    end

    %% Bounded Context 4: Conversational AI
    subgraph BC_CHAT ["4. Conversational AI"]
        UC_CHAT_01(["UC-CHAT-01: Multi-Turn Conversational Q&A"])
        UC_CHAT_02(["UC-CHAT-02: Citation Drill-Down & Verification"])
        UC_CHAT_03(["UC-CHAT-03: Token Usage & Confidence Tracking"])
    end

    %% Bounded Context 5: Workflow Automation
    subgraph BC_WF ["5. Workflow Automation"]
        UC_WF_01(["UC-WF-01: Trigger-Based Pipeline Execution"])
        UC_WF_02(["UC-WF-02: Workflow Execution Monitoring"])
    end

    %% Bounded Context 6: Operations & HITL
    subgraph BC_HITL ["6. Operations & Human-In-The-Loop"]
        UC_HITL_01(["UC-HITL-01: 2-Phase Action Request & Preview"])
        UC_HITL_02(["UC-HITL-02: Manager Approval & Idempotent Commit"])
        UC_HITL_03(["UC-HITL-03: Operational Exception Task Handling"])
    end

    %% Bounded Context 7: Audit & Notifications
    subgraph BC_AUDIT ["7. Audit Trail & Notifications"]
        UC_AUDIT_01(["UC-AUDIT-01: Immutable Audit Trail Logging"])
        UC_AUDIT_02(["UC-AUDIT-02: Real-time In-App Notifications"])
    end

    %% Actor to Use Case Associations
    Admin --- UC_IAM_02
    Admin --- UC_IAM_03
    Admin --- UC_WF_02
    Admin --- UC_AUDIT_01
    Admin --- UC_HITL_02

    Staff --- UC_IAM_01
    Staff --- UC_DOC_01
    Staff --- UC_DOC_03
    Staff --- UC_CHAT_01
    Staff --- UC_HITL_01
    Staff --- UC_HITL_03

    Manager --- UC_DOC_03
    Manager --- UC_DOC_04
    Manager --- UC_HITL_02
    Manager --- UC_HITL_03
    Manager --- UC_WF_02

    Auditor --- UC_CHAT_02
    Auditor --- UC_AUDIT_01

    Worker --- UC_RAG_01
    Engine --- UC_RAG_02
    Orchestrator --- UC_WF_01
    AuditSub --- UC_AUDIT_01

    %% Standard UML Include Relationships (Base -> Included)
    UC_CHAT_01 -. "«include»" .-> UC_RAG_02
    UC_CHAT_01 -. "«include»" .-> UC_CHAT_03
    UC_WF_01 -. "«include»" .-> UC_RAG_01
    UC_IAM_01 -. "«include»" .-> UC_AUDIT_01
    UC_DOC_01 -. "«include»" .-> UC_AUDIT_01
    UC_DOC_03 -. "«include»" .-> UC_AUDIT_01
    UC_DOC_04 -. "«include»" .-> UC_AUDIT_01
    UC_HITL_01 -. "«include»" .-> UC_AUDIT_02
    UC_HITL_02 -. "«include»" .-> UC_AUDIT_01
    UC_HITL_02 -. "«include»" .-> UC_AUDIT_02

    %% Standard UML Extend Relationships (Extension -> Base)
    UC_DOC_02 -. "«extend»\n(On Existing Document)" .-> UC_DOC_01
    UC_RAG_03 -. "«extend»\n(On 0 Chunks Found)" .-> UC_RAG_02
    UC_CHAT_02 -. "«extend»\n(On Citation Click)" .-> UC_CHAT_01
    UC_HITL_01 -. "«extend»\n(On Sensitive Action)" .-> UC_WF_01
    UC_HITL_03 -. "«extend»\n(On Pipeline Failure)" .-> UC_WF_01

    classDef actor fill:#f8f9fa,stroke:#495057,stroke-width:2px,color:#212529;
    classDef sysactor fill:#f1f3f5,stroke:#868e96,stroke-width:2px,stroke-dasharray: 5 5,color:#343a40;
```

---

## 4. Subsystem Use Case Diagrams by Bounded Context

---

### Diagram 1: Identity & Access Management (`IAM_Organization`)

```mermaid
flowchart LR
    %% Actors
    User(["System User"]):::actor
    Admin(["System Administrator"]):::actor
    AuditSub(["Audit Subsystem"]):::sysactor

    %% System Boundary
    subgraph BC_IAM ["Identity & Access Management Subsystem"]
        UC_IAM_01(["UC-IAM-01: User Login & JWT Session Management"])
        UC_IAM_02(["UC-IAM-02: Multi-Role Assignment"])
        UC_IAM_03(["UC-IAM-03: Department Setup & Internal Scoping"])
        UC_AUDIT_01_EXT(["UC-AUDIT-01: Immutable Audit Logging"]):::external
    end

    %% Associations
    User --- UC_IAM_01
    Admin --- UC_IAM_02
    Admin --- UC_IAM_03
    AuditSub --- UC_AUDIT_01_EXT

    %% Standard Includes
    UC_IAM_01 -. "«include»" .-> UC_AUDIT_01_EXT
    UC_IAM_02 -. "«include»" .-> UC_AUDIT_01_EXT
    UC_IAM_03 -. "«include»" .-> UC_AUDIT_01_EXT

    classDef actor fill:#f8f9fa,stroke:#495057,stroke-width:2px,color:#212529;
    classDef sysactor fill:#f1f3f5,stroke:#868e96,stroke-width:2px,stroke-dasharray: 5 5,color:#343a40;
    classDef external fill:#fff3bf,stroke:#fab005,stroke-width:2px,stroke-dasharray: 3 3,color:#d9480f;
```

---

### Diagram 2: Document Management & Access Control (`Document_Management`)

```mermaid
flowchart LR
    %% Primary Actors
    Staff(["Knowledge Worker"]):::actor
    Manager(["Department Manager"]):::actor

    %% Secondary System Actors
    S3[("S3 Object Storage\n(EXT-01)")]:::system
    %% System Boundary
    subgraph BC_DOC ["Document Management Subsystem"]
        UC_DOC_01(["UC-DOC-01: Document Upload & S3 Storage"])
        UC_DOC_02(["UC-DOC-02: Manage Document Versioning"])
        UC_DOC_03(["UC-DOC-03: Configure ACL Matrix"])
        UC_DOC_04(["UC-DOC-04: Document Soft Deletion"])
        UC_AUDIT_01_EXT(["UC-AUDIT-01: Immutable Audit Logging"]):::external
    end

    %% Actor Associations
    Staff --- UC_DOC_01
    Staff --- UC_DOC_03
    Manager --- UC_DOC_03
    Manager --- UC_DOC_04

    %% Secondary Actor Associations
    UC_DOC_01 --- S3
    UC_DOC_02 --- S3

    %% Standard UML Extend (Extension -> Base)
    UC_DOC_02 -. "«extend»\n(Extension Point:\nExisting Document Revision)" .-> UC_DOC_01

    %% Standard UML Include (Base -> Included)
    UC_DOC_01 -. "«include»" .-> UC_AUDIT_01_EXT
    UC_DOC_03 -. "«include»" .-> UC_AUDIT_01_EXT
    UC_DOC_04 -. "«include»" .-> UC_AUDIT_01_EXT

    classDef actor fill:#f8f9fa,stroke:#495057,stroke-width:2px,color:#212529;
    classDef system fill:#f3f0ff,stroke:#7950f2,stroke-width:2px,color:#5f3dc4;
    classDef external fill:#fff3bf,stroke:#fab005,stroke-width:2px,stroke-dasharray: 3 3,color:#d9480f;
```

---

### Diagram 3: AI Knowledge & Hybrid Retrieval (`AI_Knowledge_RAG`)

```mermaid
flowchart LR
    %% System Actors
    Worker(["AI Ingestion Worker\n(SYS-01)"]):::sysactor
    Engine(["Hybrid RAG Engine\n(SYS-02)"]):::sysactor

    %% External Service Actor
    LLM[("Embedding & LLM API\n(EXT-02)")]:::system

    %% System Boundary
    subgraph BC_RAG ["AI Knowledge & RAG Subsystem"]
        UC_RAG_01(["UC-RAG-01: Ingestion & Vector Indexing"])
        UC_RAG_02(["UC-RAG-02: Pre-filtered Hybrid RRF Search"])
        UC_RAG_03(["UC-RAG-03: Anti-Hallucination Safe Abstention"])
    end

    %% Actor Associations
    Worker --- UC_RAG_01
    Engine --- UC_RAG_02

    %% External Associations
    UC_RAG_01 --- LLM
    UC_RAG_02 --- LLM

    %% Standard UML Extend (Extension -> Base)
    UC_RAG_03 -. "«extend»\n(Extension Point:\n0 Chunks Found OR Sim < 0.50)" .-> UC_RAG_02

    classDef sysactor fill:#f1f3f5,stroke:#868e96,stroke-width:2px,stroke-dasharray: 5 5,color:#343a40;
    classDef system fill:#f3f0ff,stroke:#7950f2,stroke-width:2px,color:#5f3dc4;
```

---

### Diagram 4: Conversational AI & Citations (`Conversational_AI`)

```mermaid
flowchart LR
    %% Primary Human Actors
    Staff(["Knowledge Worker"]):::actor
    Auditor(["Compliance Auditor"]):::actor

    %% Secondary System Actor
    LLM[("Embedding & LLM API\n(EXT-02)")]:::system

    %% System Boundary
    subgraph BC_CHAT ["Conversational AI Subsystem"]
        UC_CHAT_01(["UC-CHAT-01: Multi-Turn Conversational Q&A"])
        UC_CHAT_02(["UC-CHAT-02: Evidence Citation Drill-Down"])
        UC_CHAT_03(["UC-CHAT-03: Token Usage & Confidence Tracking"])
        UC_RAG_02_EXT(["UC-RAG-02: Pre-filtered Hybrid Search"]):::external
    end

    %% Actor Associations
    Staff --- UC_CHAT_01
    Auditor --- UC_CHAT_01
    Auditor --- UC_CHAT_02
    UC_CHAT_01 --- LLM

    %% Standard UML Include (Base -> Included)
    UC_CHAT_01 -. "«include»" .-> UC_RAG_02_EXT
    UC_CHAT_01 -. "«include»" .-> UC_CHAT_03

    %% Standard UML Extend (Extension -> Base)
    UC_CHAT_02 -. "«extend»\n(Extension Point:\nUser Clicks Citation Badge)" .-> UC_CHAT_01

    classDef actor fill:#f8f9fa,stroke:#495057,stroke-width:2px,color:#212529;
    classDef system fill:#f3f0ff,stroke:#7950f2,stroke-width:2px,color:#5f3dc4;
    classDef external fill:#fff3bf,stroke:#fab005,stroke-width:2px,stroke-dasharray: 3 3,color:#d9480f;
```

---

### Diagram 5: Workflow Automation (`Workflow_Automation`)

```mermaid
flowchart LR
    %% Actors
    Orchestrator(["Workflow Orchestrator\n(SYS-03)"]):::sysactor
    Manager(["Department Manager"]):::actor
    Admin(["System Administrator"]):::actor

    %% System Boundary
    subgraph BC_WF ["Workflow Automation Subsystem"]
        UC_WF_01(["UC-WF-01: Trigger-Based Pipeline Execution"])
        UC_WF_02(["UC-WF-02: Workflow Execution Monitoring"])
        UC_RAG_01_EXT(["UC-RAG-01: Ingestion & Vector Indexing"]):::external
        UC_HITL_01_EXT(["UC-HITL-01: 2-Phase Action Request"]):::external
        UC_HITL_03_EXT(["UC-HITL-03: Exception Task Handling"]):::external
    end

    %% Actor Associations
    Orchestrator --- UC_WF_01
    Manager --- UC_WF_02
    Admin --- UC_WF_02

    %% Standard UML Include (Base -> Included)
    UC_WF_01 -. "«include»" .-> UC_RAG_01_EXT

    %% Standard UML Extend (Extension -> Base)
    UC_HITL_01_EXT -. "«extend»\n(Extension Point:\nSensitive Action Triggered)" .-> UC_WF_01
    UC_HITL_03_EXT -. "«extend»\n(Extension Point:\nStep Execution Failure)" .-> UC_WF_01

    classDef actor fill:#f8f9fa,stroke:#495057,stroke-width:2px,color:#212529;
    classDef sysactor fill:#f1f3f5,stroke:#868e96,stroke-width:2px,stroke-dasharray: 5 5,color:#343a40;
    classDef external fill:#fff3bf,stroke:#fab005,stroke-width:2px,stroke-dasharray: 3 3,color:#d9480f;
```

---

### Diagram 6: Operations & Human-In-The-Loop (`Operations_HITL`)

```mermaid
flowchart LR
    %% Primary Actors
    Staff(["Knowledge Worker"]):::actor
    Manager(["Department Manager"]):::actor
    Admin(["System Administrator"]):::actor

    %% System Boundary
    subgraph BC_HITL ["Operations & HITL Subsystem"]
        UC_HITL_01(["UC-HITL-01: 2-Phase Action Request & Preview"])
        UC_HITL_02(["UC-HITL-02: Manager Approval & Idempotent Commit"])
        UC_HITL_03(["UC-HITL-03: Operational Exception Task Handling"])
        UC_AUDIT_01_EXT(["UC-AUDIT-01: Immutable Audit Logging"]):::external
        UC_AUDIT_02_EXT(["UC-AUDIT-02: Real-time In-App Notifications"]):::external
    end

    %% Actor Associations
    Staff --- UC_HITL_01
    Staff --- UC_HITL_03
    Manager --- UC_HITL_02
    Manager --- UC_HITL_03
    Admin --- UC_HITL_02

    %% Standard UML Include Relationships
    UC_HITL_01 -. "«include»" .-> UC_AUDIT_02_EXT
    UC_HITL_02 -. "«include»" .-> UC_AUDIT_01_EXT
    UC_HITL_02 -. "«include»" .-> UC_AUDIT_02_EXT
    UC_HITL_03 -. "«include»" .-> UC_AUDIT_01_EXT

    classDef actor fill:#f8f9fa,stroke:#495057,stroke-width:2px,color:#212529;
    classDef external fill:#fff3bf,stroke:#fab005,stroke-width:2px,stroke-dasharray: 3 3,color:#d9480f;
```

---

### Diagram 7: Audit Subsystem & Notifications (`Audit_System`)

```mermaid
flowchart LR
    %% Actors
    Auditor(["Compliance Auditor"]):::actor
    Admin(["System Administrator"]):::actor
    User(["System User"]):::actor
    AuditSub(["Audit Subsystem\n(SYS-04)"]):::sysactor
    NotifSub(["Notification Subsystem\n(SYS-05)"]):::sysactor

    %% System Boundary
    subgraph BC_AUDIT ["Audit Trail & Notification Subsystem"]
        UC_AUDIT_01(["UC-AUDIT-01: Immutable Audit Trail Logging"])
        UC_AUDIT_02(["UC-AUDIT-02: Real-time In-App Notifications"])
    end

    %% Actor Associations
    AuditSub --- UC_AUDIT_01
    Auditor --- UC_AUDIT_01
    Admin --- UC_AUDIT_01
    NotifSub --- UC_AUDIT_02
    User --- UC_AUDIT_02

    classDef actor fill:#f8f9fa,stroke:#495057,stroke-width:2px,color:#212529;
    classDef sysactor fill:#f1f3f5,stroke:#868e96,stroke-width:2px,stroke-dasharray: 5 5,color:#343a40;
```
