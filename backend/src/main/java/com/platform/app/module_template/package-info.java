/**
 * <h1>Business Capability: Support Ticket Management (Core Domain Blueprint)</h1>
 *
 * <h2>1. Role & Responsibility</h2>
 * <p>
 * Bounded Context responsible for the full customer support ticket lifecycle:
 * ticket creation, agent assignment, state machine transitions, conversation threading,
 * customer/agent messaging, resolution, and closing.
 * </p>
 * <p>
 * This capability serves as the <b>Architectural Archetype</b> for complex core domains,
 * illustrating the selective application of <b>Tactical Domain-Driven Design (DDD)</b>
 * structured with Hexagonal / Ports and Adapters architecture.
 * </p>
 *
 * <h2>2. Architectural Layers</h2>
 * <ul>
 *   <li>{@link com.platform.app.module_template.domain}: The central domain core (Aggregate Roots, Entities, Value Objects, Domain Events, Repository Interfaces).</li>
 *   <li>{@link com.platform.app.module_template.application}: Use Cases and workflow orchestration (Application Services, Command/Query Handlers, DTOs).</li>
 *   <li>{@link com.platform.app.module_template.infrastructure}: Outbound Adapters (Spring Data JPA persistence, External Gateways, Message Publishers).</li>
 *   <li>{@link com.platform.app.module_template.api}: Inbound Adapters (REST Controllers, API Request/Response models, OpenAPI documentation).</li>
 * </ul>
 *
 * <h2>3. Flow of Dependencies</h2>
 * <pre>
 *   [ api ] ───────────────┐
 *                          ▼
 *   [ infrastructure ] ─► [ application ] ─► [ domain ] (Pure Core - Zero outer dependencies)
 * </pre>
 */
package com.platform.app.module_template;
