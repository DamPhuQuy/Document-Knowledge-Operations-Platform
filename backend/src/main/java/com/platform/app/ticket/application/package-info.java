/**
 * <h1>Application Layer - Use Cases & Workflow Orchestration</h1>
 *
 * <h2>1. Role & Responsibility</h2>
 * <p>
 * The Application Layer acts as the orchestrator for domain use cases. Its responsibilities include:
 * </p>
 * <ul>
 *   <li>Receiving commands and queries from the API inbound adapters.</li>
 *   <li>Managing transaction boundaries ({@code @Transactional}).</li>
 *   <li>Enforcing contextual authorization rules based on user role and ownership.</li>
 *   <li>Loading Aggregate Roots from repositories, executing domain methods, and saving updated state.</li>
 *   <li>Mapping domain entities into safe, decoupled response DTOs.</li>
 * </ul>
 *
 * <h2>2. Dependency Rules</h2>
 * <ul>
 *   <li><b>Allowed:</b> Depends strictly on {@code domain} and {@code shared}.</li>
 *   <li><b>Strictly Forbidden:</b> Must NOT depend on concrete persistence classes in {@code infrastructure} or web transport classes in {@code api}.</li>
 * </ul>
 *
 * <h2>3. Expected Components</h2>
 * <ul>
 *   <li>
 *     <b>Application Services / Use Case Handlers (e.g., {@link com.platform.app.ticket.application.service.TicketApplicationService}):</b><br>
 *     Coordinates domain aggregate operations across transaction boundaries.
 *   </li>
 *   <li>
 *     <b>Commands & Queries (e.g., {@link com.platform.app.ticket.application.dto.CreateTicketCommand}, {@link com.platform.app.ticket.application.dto.AssignTicketCommand}, {@link com.platform.app.ticket.application.dto.UpdateTicketStatusCommand}):</b><br>
 *     Input DTOs carrying validated use case parameters.
 *   </li>
 *   <li>
 *     <b>Response DTOs (e.g., {@link com.platform.app.ticket.application.dto.TicketResponse}, {@link com.platform.app.ticket.application.dto.TicketSummaryResponse}, {@link com.platform.app.ticket.application.dto.TicketMessageResponse}):</b><br>
 *     Output representations decoupled from internal domain model structure.
 *   </li>
 * </ul>
 */
package com.platform.app.ticket.application;
