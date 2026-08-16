/**
 * <h1>API / Transport Layer - Inbound Adapters</h1>
 *
 * <h2>1. Role & Responsibility</h2>
 * <p>
 * The API Layer provides inbound driving adapters receiving incoming client requests via HTTP REST,
 * WebSockets, GraphQL, or message queues.
 * </p>
 * <p>
 * Responsibilities include:
 * </p>
 * <ul>
 *   <li>Mapping HTTP requests (endpoints, query parameters, path variables, request bodies).</li>
 *   <li>Enforcing input validation via Bean Validation (JSR-380 {@code @Valid}).</li>
 *   <li>Invoking corresponding application use case services.</li>
 *   <li>Wrapping results into standard {@link com.platform.app.shared.dto.ApiResponse} payloads with proper HTTP status codes.</li>
 *   <li>Publishing OpenAPI / Swagger documentation metadata.</li>
 * </ul>
 *
 * <h2>2. Dependency Rules</h2>
 * <ul>
 *   <li><b>Allowed:</b> Depends on {@code application}, {@code domain}, and {@code shared}.</li>
 *   <li><b>Strictly Forbidden:</b> Must NEVER interact directly with the {@code infrastructure} layer (e.g., no direct queries or JPA repository injection in controllers).</li>
 * </ul>
 *
 * <h2>3. Expected Components</h2>
 * <ul>
 *   <li>
 *     <b>REST Controllers (e.g., {@link com.platform.app.ticket.api.TicketController}):</b><br>
 *     Handles HTTP routing, parameter extraction, and use case invocation.
 *   </li>
 *   <li>
 *     <b>Web Request Payloads & Custom Formatters (if protocol-specific):</b><br>
 *     Structures specific to HTTP transport serialization.
 *   </li>
 * </ul>
 */
package com.platform.app.ticket.api;
