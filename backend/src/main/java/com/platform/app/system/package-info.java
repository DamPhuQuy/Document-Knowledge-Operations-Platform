/**
 * <h1>Business Capability: System Diagnostics & Health</h1>
 *
 * <h2>1. Role & Responsibility</h2>
 * <p>
 * Provides operational diagnostic endpoints, liveness checks, service uptime,
 * and active runtime profile information for orchestration engines (Kubernetes, Docker, Load Balancers).
 * </p>
 *
 * <h2>2. Architectural Style: Lean & Pragmatic Capability</h2>
 * <p>
 * As this capability has no complex domain rules or state mutations, it is intentionally implemented
 * using a thin, pragmatic style (REST Controller &rarr; DTO) without DDD layering overhead.
 * </p>
 *
 * <h2>3. Expected Components</h2>
 * <ul>
 *   <li>{@link com.platform.app.system.web.HealthController}: Exposes {@code GET /api/v1/health}.</li>
 *   <li>{@link com.platform.app.system.web.HealthCheckResponse}: Lightweight DTO containing status, timestamp, uptime, and service name.</li>
 * </ul>
 */
package com.platform.app.system;
