/**
 * <h1>Domain Layer - Core Business Logic & Invariants</h1>
 *
 * <h2>1. Role & Responsibility</h2>
 * <p>
 * The Domain Layer is the heart of the Bounded Context. It encapsulates all domain models,
 * business rules, aggregate consistency boundaries, state machine invariants, and domain events.
 * </p>
 * <p>
 * This layer models <b>how the business operates</b> completely independent of database technologies,
 * network transport protocols (HTTP/REST/gRPC), or web frameworks.
 * </p>
 *
 * <h2>2. Dependency Rules</h2>
 * <ul>
 *   <li><b>Strict Invariant:</b> The Domain Layer must NEVER depend on outer layers ({@code application}, {@code infrastructure}, or {@code api}).</li>
 *   <li>Framework-free: Must not import Spring MVC, Spring Web, or transport-specific annotations.</li>
 *   <li>All external interactions (databases, third-party systems) are modeled strictly as <b>Domain Repository Interfaces (Ports)</b> declared here.</li>
 * </ul>
 *
 * <h2>3. Expected Components</h2>
 * <ul>
 *   <li>
 *     <b>Aggregate Roots (e.g., {@link com.platform.app.ticket.domain.model.Ticket}):</b><br>
 *     The root entity guarding internal consistency, enforcing state transitions,
 *     managing child entities, and registering domain events.
 *   </li>
 *   <li>
 *     <b>Entities (e.g., {@link com.platform.app.ticket.domain.model.TicketMessage}):</b><br>
 *     Internal entities possessing unique identity within the aggregate boundary.
 *   </li>
 *   <li>
 *     <b>Value Objects (e.g., {@link com.platform.app.ticket.domain.model.TicketStatus}, {@link com.platform.app.ticket.domain.model.TicketPriority}, {@link com.platform.app.ticket.domain.model.TicketCategory}):</b><br>
 *     Immutable types without identity, encapsulating domain rules and state machine transition logic ({@code canTransitionTo}).
 *   </li>
 *   <li>
 *     <b>Domain Events (e.g., {@link com.platform.app.ticket.domain.event.TicketCreatedEvent}, {@link com.platform.app.ticket.domain.event.TicketStatusChangedEvent}, {@link com.platform.app.ticket.domain.event.TicketAssignedEvent}):</b><br>
 *     Immutable records signaling significant state changes within the domain.
 *   </li>
 *   <li>
 *     <b>Domain Repository Interfaces (e.g., {@link com.platform.app.ticket.domain.repository.TicketRepository}):</b><br>
 *     Port contracts defining persistence needs, decoupled from specific storage implementations.
 *   </li>
 * </ul>
 */
package com.platform.app.ticket.domain;
