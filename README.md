# AI Document & Knowledge Operations Platform

> An AI-assisted platform for managing documents, retrieving organizational knowledge, and executing controlled knowledge workflows.

**Project Topic:** Infrastructure Deployment for an AI Document & Knowledge Operations Platform  
**Vietnamese:** Triển khai hạ tầng cho nền tảng quản lý tài liệu và vận hành tri thức tích hợp AI

## Overview

Organizations often keep knowledge across PDFs, policies, reports, specifications, notes, and internal documentation. Finding useful information may require searching multiple sources, comparing versions, understanding context, and converting findings into actions.

This project adds an AI-native knowledge layer on top of document management:

```text
Documents → Ingestion → Retrieval / RAG → Context → LLM / Agent → Tools / Approval
```

The goal is not to build a generic chatbot or a full enterprise content-management system. The focus is **knowledge operations**: retrieve, reason, transform, and act on organizational knowledge while preserving permissions, citations, auditability, and human control.

## Core Capabilities

- Workspace, identity, RBAC, and document permissions.
- Document upload, metadata, versioning, and object storage.
- Async parsing, chunking, embedding, and indexing.
- Permission-aware RAG with source attribution.
- Conversation context, structured output, and tool calling.
- Tasks, human approval, audit logs, and AI evaluation.

## Architecture

```text
                         Web Client
                            │
                            ▼
                     Spring Boot API
                ┌───────────┼───────────┐
                ▼           ▼           ▼
          PostgreSQL       S3       Python AI Service
           + pgvector       │           │
                            ▼           ├── RAG / Context
                           SQS          ├── Agent / Tools
                            │           └── Evaluation
                            ▼                │
                     Ingestion Worker       ▼
                    Parse / Chunk /      Bedrock
                    Embed / Index
```

**Spring Boot** owns deterministic application logic: identity, authorization, workspaces, documents, tasks, approvals, audit, transactions, and public APIs.

**Python AI Service** owns AI workflows: embeddings, retrieval, reranking, context assembly, prompts, RAG, tool calling, agent state, and evaluation.

The backend starts as a **modular monolith**. Document ingestion is asynchronous, and PostgreSQL + pgvector is the initial vector store to keep operational complexity low.

## Main Modules

```text
Identity & Access · Workspace · Document Management · Knowledge
AI / Agent · Task & Approval · Audit
```

AI functionality is developed incrementally:

```text
Model API → RAG → Evaluation → Context Engineering → Tool Calling → Agent → Human Approval
```

LangChain and LangGraph are introduced only when their abstractions solve an observed problem. The initial architecture uses a **single agent** with explicit tools and authorization boundaries.

## Technology Stack

| Area | Technology |
|---|---|
| Frontend | React, TypeScript, Vite |
| Backend | Java, Spring Boot, Spring Security, Spring Data JPA |
| AI Service | Python, FastAPI, LangChain, LangGraph |
| Data | PostgreSQL, pgvector |
| AWS | ECS/Fargate, ECR, RDS, S3, SQS, Bedrock, IAM, VPC, CloudWatch, Secrets Manager |
| Tooling | Docker, Terraform, GitHub Actions |

OpenSearch, EventBridge, Lambda, and Bedrock Knowledge Bases are optional experiments rather than MVP requirements.

## Repository Structure

```text
ai-knowledge-platform/
├── backend/          # Spring Boot application
├── ai-service/       # RAG and agent runtime
├── frontend/         # Web client
├── infrastructure/   # Terraform / deployment
├── docs/             # Architecture, ADRs, API and AI notes
├── scripts/
├── docker-compose.yml
├── .env.example
└── README.md
```

Detailed architecture and design decisions belong in `docs/architecture/` and `docs/adr/`, not in the root README.

## MVP

The MVP should demonstrate:

1. Authentication, workspace isolation, and authorization.
2. Document upload/versioning with S3 storage.
3. Async ingestion: parse → chunk → embed → index.
4. Permission-aware vector retrieval and RAG with citations.
5. Conversation context and structured LLM responses.
6. Agent tool calling with at least one approved write action.
7. AWS deployment with basic observability.
8. Basic RAG and agent evaluation.

## Engineering Principles

- Keep the business domain understandable; put complexity into AI, backend architecture, and infrastructure.
- Add technology only when a concrete engineering requirement justifies it.
- Enforce authorization before retrieved content reaches the model.
- Treat document content as untrusted data.
- Route agent tools through application authorization and validation; never expose unrestricted DB or AWS access.
- Prefer measurable AI experiments over subjective output quality.

## Non-Goals

The initial project does not target full enterprise ECM, Google Drive/SharePoint replacement, microservices, Kubernetes, Kafka, multi-agent orchestration, unrestricted autonomous agents, foundation-model training, complex ML pipelines, or a full OCR/collaborative-editing platform.

## Project Philosophy

This repository is a **project-based learning system**:

```text
problem → baseline → limitation → concept → technique → measurement → trade-off
```

Technology is introduced after the engineering problem becomes visible, not simply because the technology exists.
