# 3. Use Case Modeling & Architectural Decomposition
## Enterprise Document Knowledge & Operations Platform
### Ultra-Lean Infrastructure-Focused MVP Specification

> **Source of Truth:** OMG UML 2.5 Modeling Standards, Macro Context Boundary, Master Decomposed Global Use Case Map, and Active Subsystem Architectural Diagrams.
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

---

## 2. High-Level Context Use Case Diagram (Ultra-Lean MVP)

The context use case diagram captures the high-level system boundary, primary human actors, secondary external systems, and macro-level capabilities for the **Ultra-Lean MVP**:

```mermaid
flowchart LR
    %% Primary Human Actors (Left)
    subgraph Human_Actors ["Primary Human Stakeholders"]
        Admin(["System & DevOps Admin"]):::actor
        Manager(["Department Manager"]):::actor
        Staff(["Knowledge Worker / Staff"]):::actor
        Auditor(["Compliance Auditor"]):::actor
    end

    %% System Boundary & Macro Use Cases
    subgraph System_Boundary ["Ultra-Lean MVP System Boundary"]
        direction TB
        M_IAM(["UC-MACRO-01: Authenticate & Manage Identity\n[BC 1: IAM_Organization]"]):::macro
        M_DOC(["UC-MACRO-02: Manage Document Lifecycle & Storage\n[BC 2: Document_Management]"]):::macro
        M_AUDIT(["UC-MACRO-03: Track Security Audit Trail & Diagnostics\n[BC 3: Audit_System]"]):::macro
    end

    %% Secondary Supporting System Actors (Right)
    subgraph Supporting_Systems ["Secondary Cloud Systems"]
        AWS_CLOUD[("AWS Cloud & Edge Platform\nEC2 / VPC / Cloudflare\n(EXT-03)")]:::cloudSystem
        S3[("AWS S3 Object Storage\n(EXT-01)")]:::system
    end

    %% Actor to Macro Associations
    Admin --- M_IAM
    Admin --- M_AUDIT
    Manager --- M_DOC
    Staff --- M_IAM
    Staff --- M_DOC
    Auditor --- M_AUDIT

    %% Secondary Actor Associations
    System_Boundary --- AWS_CLOUD
    M_DOC --- S3

    %% Include Dependencies
    M_IAM -. "«include»" .-> M_AUDIT
    M_DOC -. "«include»" .-> M_AUDIT

    classDef actor fill:#f8f9fa,stroke:#495057,stroke-width:2px,color:#212529;
    classDef macro fill:#e7f5ff,stroke:#1c7ed6,stroke-width:2px,color:#1864ab;
    classDef system fill:#fff4e6,stroke:#fd7e14,stroke-width:2px,color:#d9480f;
    classDef cloudSystem fill:#e6fcf5,stroke:#0ca678,stroke-width:2px,color:#099268;
```

> **Note on Deferred Modules:** Advanced macro capabilities (`UC-MACRO-04: Conversational AI`, `UC-MACRO-05: Workflow Automation`, `UC-MACRO-06: Operations HITL`) have been relocated to [`deferred/`](deferred/) for post-MVP execution.

---

## 3. Master Decomposed Use Case Map (Ultra-Lean MVP)

The macro capabilities are decomposed into **8 active functional use cases** across **3 Core Bounded Contexts**:

```mermaid
flowchart TB
    %% Human Actors
    Admin(["System Admin"]):::actor
    Manager(["Department Manager"]):::actor
    Staff(["Knowledge Worker"]):::actor
    Auditor(["Compliance Auditor"]):::actor

    %% System Actors
    AuditSub(["Audit Subsystem (SYS-04)"]):::sysactor
    S3[("AWS S3 Storage (EXT-01)")]:::sysactor

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

    %% Bounded Context 3: Audit System
    subgraph BC_AUDIT ["3. Audit Trail Subsystem"]
        UC_AUDIT_01(["UC-AUDIT-01: Immutable Audit Trail Logging"])
    end

    %% Actor to Use Case Associations
    Admin --- UC_IAM_02
    Admin --- UC_IAM_03
    Admin --- UC_AUDIT_01

    Staff --- UC_IAM_01
    Staff --- UC_DOC_01
    Staff --- UC_DOC_03

    Manager --- UC_DOC_03
    Manager --- UC_DOC_04

    Auditor --- UC_AUDIT_01

    AuditSub --- UC_AUDIT_01
    UC_DOC_01 --- S3
    UC_DOC_02 --- S3

    %% Standard UML Include Relationships (Base -> Included)
    UC_IAM_01 -. "«include»" .-> UC_AUDIT_01
    UC_IAM_02 -. "«include»" .-> UC_AUDIT_01
    UC_IAM_03 -. "«include»" .-> UC_AUDIT_01
    UC_DOC_01 -. "«include»" .-> UC_AUDIT_01
    UC_DOC_03 -. "«include»" .-> UC_AUDIT_01
    UC_DOC_04 -. "«include»" .-> UC_AUDIT_01

    %% Standard UML Extend Relationships (Extension -> Base)
    UC_DOC_02 -. "«extend»\n(On Existing Document)" .-> UC_DOC_01

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
    S3[("AWS S3 Storage\n(EXT-01)")]:::system

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

### Diagram 3: Audit Trail Subsystem (`Audit_System`)

```mermaid
flowchart LR
    %% Primary Actors
    Auditor(["Compliance Auditor"]):::actor
    Admin(["System Administrator"]):::actor

    %% Secondary System Actors
    AuditSub(["Audit Subsystem (SYS-04)"]):::sysactor

    %% System Boundary
    subgraph BC_AUDIT ["Audit Subsystem Boundary"]
        UC_AUDIT_01(["UC-AUDIT-01: Immutable Audit Trail Logging"])
    end

    %% Actor Associations
    Auditor --- UC_AUDIT_01
    Admin --- UC_AUDIT_01
    AuditSub --- UC_AUDIT_01

    classDef actor fill:#f8f9fa,stroke:#495057,stroke-width:2px,color:#212529;
    classDef sysactor fill:#f1f3f5,stroke:#868e96,stroke-width:2px,stroke-dasharray: 5 5,color:#343a40;
```
