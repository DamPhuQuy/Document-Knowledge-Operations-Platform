/**
 * <h1>Shared Kernel & Global Infrastructure</h1>
 *
 * <h2>1. Role & Responsibility</h2>
 * <p>
 * The {@code shared} package serves as the system's <b>Shared Kernel</b> in DDD terminology.
 * It houses cross-cutting concerns, base classes, common DTOs, global exception handling,
 * and application-wide Spring configurations consumed across all business capabilities.
 * </p>
 *
 * <h2>2. Dependency Rules</h2>
 * <ul>
 *   <li><b>Allowed:</b> Depends on standard libraries, Spring Framework, Jackson, JJWT, and Jakarta Persistence.</li>
 *   <li><b>Strictly Forbidden:</b> Must NEVER depend on any concrete business capability (such as {@code iam}, {@code ticket}, etc.).</li>
 * </ul>
 *
 * <h2>3. Expected Components</h2>
 * <ul>
 *   <li>{@code shared.domain}: {@link com.platform.app.shared.domain.BaseEntity}, {@link com.platform.app.shared.domain.AggregateRoot}, {@link com.platform.app.shared.domain.DomainEvent}</li>
 *   <li>{@code shared.dto}: {@link com.platform.app.shared.dto.ApiResponse}, {@link com.platform.app.shared.dto.PageResponse}</li>
 *   <li>{@code shared.exception}: {@link com.platform.app.shared.exception.AppException}, {@link com.platform.app.shared.exception.GlobalExceptionHandler}, {@link com.platform.app.shared.exception.ErrorCode}</li>
 *   <li>{@code shared.config}: Global Spring configurations (Security, CORS, OpenAPI Swagger, Jackson, JPA Auditing)</li>
 *   <li>{@code shared.security}: JWT token provider, security filters, user details, and HTTP authentication handlers</li>
 * </ul>
 */
package com.platform.app.shared;
