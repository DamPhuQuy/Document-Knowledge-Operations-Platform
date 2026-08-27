# AI Document & Knowledge Operations Platform

> An AI-assisted platform for managing documents, retrieving organizational knowledge, and executing controlled knowledge workflows.

**Project Topic:** Infrastructure Deployment for an AI Document & Knowledge Operations Platform

## Overview

Organizations maintain knowledge across PDFs, policies, reports, specifications, notes, and internal documentation. Finding and utilizing this information requires searching multiple sources, comparing versions, understanding context, and converting findings into verified actions.

This platform provides an AI-native knowledge layer on top of document management:

```text
Documents -> Ingestion -> Retrieval / RAG -> Context -> LLM / Agent -> Tools / Approval
```

The core objective is **knowledge operations**: retrieving, reasoning, transforming, and acting on organizational knowledge while enforcing permissions, verifiable citations, auditability, and human-in-the-loop control.

## Core Capabilities

- Identity, multi-tenant workspace isolation, and RBAC document permissions.
- Document upload, metadata tracking, versioning, and object storage.
- Asynchronous parsing, chunking, embedding, and vector indexing.
- Permission-aware hybrid RAG with source citations.
- Conversational context, structured LLM responses, and tool calling.
- Operational tasks, human approvals, audit logging, and AI evaluation.

## Architecture

```text
                         Web Client
                            |
                            v
                     Spring Boot API
                +-----------+-----------+
                |           |           |
                v           v           v
          PostgreSQL       S3       Python AI Service
          + pgvector        |           |
                            v           +-- RAG / Context
                           SQS          +-- Agent / Tools
                            |           +-- Evaluation
                            v                |
                     Ingestion Worker        v
                    Parse / Chunk /       Bedrock
                    Embed / Index
```

- **Spring Boot API**: Handles core business logic, identity, access control, workspaces, document metadata, tasks, approvals, audit trails, and client APIs.
- **Python AI Service**: Handles AI pipelines including chunking, embeddings, dense/sparse retrieval, reranking, context assembly, RAG, tool calling, and evaluation.
- **PostgreSQL + pgvector**: Unified relational store and vector database.
- **AWS S3 / SQS**: Object storage for documents and message queue for asynchronous ingestion.

## Repository Structure

```text
Document-Knowledge-Operations-Platform/
├── backend/              # Spring Boot application service
├── ai/                   # Python AI & RAG microservice
├── frontend/             # React + TypeScript web client
├── docs/                 # Architecture, database schemas, and documentation
├── docker-compose.yaml
├── .env.example          # Local / default environment template
├── .env.dev.example      # Development environment template
├── .env.prod.example     # Production environment template
└── README.md
```

## Technology Stack

| Area | Technology |
|---|---|
| Frontend | React, TypeScript, Vite |
| Backend | Java 25, Spring Boot 4, Spring Security, Spring Data JPA, Liquibase |
| AI Service | Python 3.11+, FastAPI, uv, pgvector, ChromaDB |
| Database | PostgreSQL 16+, pgvector |
| Infrastructure | Docker, Docker Compose, AWS (S3, SQS, Bedrock) |

## Engineering Principles

- Enforce authorization checks before retrieved document context reaches the model.
- Treat ingested document content as untrusted input.
- Route agent tools through backend authorization and validation; prevent direct, unrestricted database or cloud access.
- Keep business domain boundaries explicit between deterministic services and AI pipelines.
- Use measurable evaluation metrics (groundedness, relevance, faithfulness) rather than subjective output assessment.

## Non-Goals

The platform is not intended as a generic chatbot, a full enterprise content management (ECM) suite, an unrestricted autonomous agent runner, or a foundation model training platform.

