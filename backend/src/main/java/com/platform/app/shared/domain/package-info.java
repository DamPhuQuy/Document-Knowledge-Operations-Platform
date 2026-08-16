/**
 * <h1>Shared Domain Kernel</h1>
 *
 * <h2>1. Role & Responsibility</h2>
 * <p>
 * Provides reusable domain contracts and base classes for entity lifecycle and domain event publication.
 * </p>
 *
 * <h2>2. Dependency Rules</h2>
 * <ul>
 *   <li>Depends only on Jakarta Persistence and Spring Data domain event annotations.</li>
 *   <li>Contains no capability-specific business rules.</li>
 * </ul>
 *
 * <h2>3. Expected Components</h2>
 * <ul>
 *   <li>{@link com.platform.app.shared.domain.BaseEntity}: Mapped superclass providing auto-increment ID and auditing timestamps (createdAt, updatedAt).</li>
 *   <li>{@link com.platform.app.shared.domain.AggregateRoot}: Base class for aggregate roots supporting internal event accumulation and publishing via {@link org.springframework.data.domain.DomainEvents}.</li>
 *   <li>{@link com.platform.app.shared.domain.DomainEvent}: Common marker contract for all domain events across the platform.</li>
 * </ul>
 */
package com.platform.app.shared.domain;
