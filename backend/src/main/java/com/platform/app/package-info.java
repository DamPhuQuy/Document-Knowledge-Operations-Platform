/**
 * <h1>Document and Knowledge Operations Platform - Backend Architecture Blueprint</h1>
 *
 * <h2>Architectural Principles</h2>
 * <p>
 * This application is architected following two fundamental design principles:
 * </p>
 * <ol>
 *   <li>
 *     <b>Package by Business Capability (Top-Level):</b><br>
 *     Top-level packages represent autonomous bounded contexts or business capabilities
 *     (e.g., {@code iam}, {@code ticket}, {@code system}, {@code shared}) rather than technical horizontal slices.
 *     This maximizes cohesion within capabilities and minimizes coupling across boundaries.
 *   </li>
 *   <li>
 *     <b>Selective Tactical DDD (Domain Complexity Driven):</b><br>
 *     Tactical Domain-Driven Design patterns (Aggregates, Value Objects, Domain Events, Domain Repositories)
 *     are applied selectively where domain complexity warrants them (e.g., {@code ticket}).
 *     Simple CRUD, authentication, or infrastructure capabilities (e.g., {@code iam}, {@code system})
 *     remain lean and pragmatic (Controller &rarr; Service &rarr; Repository) to avoid premature over-engineering.
 *   </li>
 * </ol>
 *
 * <h2>Standard Capability Package Layout (For Complex DDD Capabilities)</h2>
 * <pre>
 * com.platform.app.[capability]/
 *   +-- domain/          (Enterprise & Core Domain Logic - Pure Java, zero framework coupling)
 *   |     +-- model/     (Aggregate Roots, Entities, Value Objects)
 *   |     +-- event/     (Domain Events)
 *   |     +-- repository/(Domain Repository Interfaces / Outbound Ports)
 *   +-- application/     (Application Use Cases & Transaction Orchestration)
 *   |     +-- service/   (Application Services / Use Case Handlers)
 *   |     +-- dto/       (Command, Query, and Response DTOs)
 *   +-- infrastructure/  (Outbound Adapters: Persistence, Message Brokers, External Gateways)
 *   |     +-- persistence/ (Spring Data JPA Repositories, Database Adapters)
 *   +-- api/             (Inbound Adapters: REST, GraphQL, WebSocket, or gRPC Controllers)
 * </pre>
 *
 * <h2>Dependency Inversion & Flow of Dependencies</h2>
 * <ul>
 *   <li><b>domain</b> &larr; depended on by <b>application</b>, <b>infrastructure</b>, and <b>api</b>. Must NEVER depend on outer layers.</li>
 *   <li><b>application</b> &larr; depended on by <b>api</b> and <b>infrastructure</b>. Depends only on <b>domain</b> and <b>shared</b>.</li>
 *   <li><b>infrastructure</b> &rarr; implements domain repository interfaces. Depends on <b>domain</b> and <b>application</b>.</li>
 *   <li><b>api</b> &rarr; triggers application use cases. Depends on <b>application</b>, <b>domain</b>, and <b>shared</b>.</li>
 * </ul>
 */
package com.platform.app;
