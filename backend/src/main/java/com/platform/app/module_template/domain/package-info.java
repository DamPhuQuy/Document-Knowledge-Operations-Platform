/**
 * <h1>Domain Layer - Core Business Logic & Invariants</h1>
 *
 * <h2>1. Role & Responsibility</h2>
 * <p>
 * The Domain Layer is the heart of a Bounded Context. It encapsulates all domain models,
 * business rules, aggregate consistency boundaries, state machine invariants, and domain events.
 * </p>
 * <p>
 * This layer models <b>how the business domain operates</b> completely independent of database technologies,
 * network transport protocols (HTTP/REST/gRPC), or web frameworks.
 * </p>
 *
 * <h2>2. Dependency Rules</h2>
 * <ul>
 *   <li><b>Strict Invariant:</b> The Domain Layer must NEVER depend on outer layers ({@code application}, {@code infrastructure}, or {@code api}).</li>
 *   <li>Framework-free: Must not import Spring MVC, Spring Web, or transport-specific annotations.</li>
 *   <li>All external interactions (databases, third-party systems) are modeled strictly as <b>Domain Repository Interfaces (Ports)</b> declared in this package.</li>
 * </ul>
 *
 * <h2>3. Expected Components</h2>
 * <ul>
 *   <li>
 *     <b>Aggregate Roots:</b><br>
 *     The primary entry entities guarding internal consistency, enforcing state transitions,
 *     managing child entities, and registering domain events.
 *   </li>
 *   <li>
 *     <b>Entities:</b><br>
 *     Internal entities possessing unique identities within the aggregate boundary.
 *   </li>
 *   <li>
 *     <b>Value Objects:</b><br>
 *     Immutable types without separate identity, encapsulating domain attributes, validation, and transition logic.
 *   </li>
 *   <li>
 *     <b>Domain Events:</b><br>
 *     Immutable event records signaling significant state changes or business occurrences within the domain.
 *   </li>
 *   <li>
 *     <b>Domain Repository Interfaces:</b><br>
 *     Outbound port contracts defining persistence operations, decoupled from concrete storage technologies.
 *   </li>
 * </ul>
 */
package com.platform.app.module_template.domain;
