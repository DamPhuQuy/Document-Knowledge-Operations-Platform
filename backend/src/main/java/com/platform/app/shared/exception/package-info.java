/**
 * <h1>Shared Exception & Global Error Handling</h1>
 *
 * <h2>1. Role & Responsibility</h2>
 * <p>
 * Implements a centralized exception hierarchy and global REST exception handler translating runtime,
 * validation, domain rule, and security exceptions into standardized {@link com.platform.app.shared.dto.ApiResponse} formats.
 * </p>
 *
 * <h2>2. Expected Components</h2>
 * <ul>
 *   <li>{@link com.platform.app.shared.exception.ErrorCode}: Canonical catalog of business error codes mapped to HTTP status codes.</li>
 *   <li>{@link com.platform.app.shared.exception.AppException}: Base unchecked exception for all application-level errors.</li>
 *   <li>{@link com.platform.app.shared.exception.DomainRuleViolationException}: Thrown when a business invariant or aggregate rule is violated (HTTP 422 Unprocessable Entity).</li>
 *   <li>{@link com.platform.app.shared.exception.ResourceNotFoundException}: Thrown when an entity or resource is not found (HTTP 404 Not Found).</li>
 *   <li>{@link com.platform.app.shared.exception.BadRequestException}: Thrown on invalid request arguments (HTTP 400 Bad Request).</li>
 *   <li>{@link com.platform.app.shared.exception.UnauthorizedException}: Thrown on authentication failures (HTTP 401 Unauthorized).</li>
 *   <li>{@link com.platform.app.shared.exception.ForbiddenException}: Thrown on authorization/permission failures (HTTP 403 Forbidden).</li>
 *   <li>{@link com.platform.app.shared.exception.GlobalExceptionHandler}: {@code @RestControllerAdvice} intercepting all application exceptions.</li>
 * </ul>
 */
package com.platform.app.shared.exception;
