/**
 * <h1>Infrastructure Layer - Outbound Adapters & Technical Implementations</h1>
 *
 * <h2>1. Role & Responsibility</h2>
 * <p>
 * The Infrastructure Layer implements the secondary/outbound ports defined by the domain layer.
 * It handles the technical details of communicating with relational databases (PostgreSQL via Spring Data JPA),
 * message brokers, external notification APIs, and file storage systems.
 * </p>
 *
 * <h2>2. Dependency Rules & Inversion of Control</h2>
 * <ul>
 *   <li><b>Dependency Inversion Principle (DIP):</b> Implements domain repository interfaces declared in {@code domain.repository}, isolating domain business logic from database and SQL frameworks.</li>
 *   <li><b>Allowed:</b> Depends on {@code domain} and {@code application}.</li>
 *   <li><b>Strictly Forbidden:</b> Outer components must not reverse-couple domain or application layers to concrete infrastructure classes.</li>
 * </ul>
 *
 * <h2>3. Expected Components</h2>
 * <ul>
 *   <li>
 *     <b>Spring Data JPA Repositories (e.g., {@link com.platform.app.ticket.infrastructure.persistence.SpringDataJpaTicketRepository}):</b><br>
 *     Framework interface extending {@link org.springframework.data.jpa.repository.JpaRepository}.
 *   </li>
 *   <li>
 *     <b>Repository Implementations (e.g., {@link com.platform.app.ticket.infrastructure.persistence.TicketRepositoryImpl}):</b><br>
 *     Adapter class implementing {@link com.platform.app.ticket.domain.repository.TicketRepository} and delegating to Spring Data JPA.
 *   </li>
 *   <li>
 *     <b>External Gateways & Data Mappers:</b><br>
 *     Adapters communicating with remote third-party systems or converting database schemas to domain models.
 *   </li>
 * </ul>
 */
package com.platform.app.ticket.infrastructure;
