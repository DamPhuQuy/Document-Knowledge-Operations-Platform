/**
 * <h1>Shared Security & JWT Infrastructure</h1>
 *
 * <h2>1. Role & Responsibility</h2>
 * <p>
 * Provides stateless authentication and authorization infrastructure using JSON Web Tokens (JWT)
 * conforming to modern JJWT 0.12 specifications.
 * </p>
 *
 * <h2>2. Expected Components</h2>
 * <ul>
 *   <li>{@link com.platform.app.shared.security.JwtTokenProvider}: Generates, parses, extracts claims, and verifies HMAC-signed JWT access and refresh tokens.</li>
 *   <li>{@link com.platform.app.shared.security.JwtAuthenticationFilter}: Intercepts incoming HTTP requests, extracts the Bearer token from the Authorization header, and populates the {@link org.springframework.security.core.context.SecurityContext}.</li>
 *   <li>{@link com.platform.app.shared.security.UserPrincipal}: Immutable {@link org.springframework.security.core.userdetails.UserDetails} implementation representing the authenticated caller.</li>
 *   <li>{@link com.platform.app.shared.security.RestAuthenticationEntryPoint}: Formats unauthenticated 401 Unauthorized responses into standard JSON {@link com.platform.app.shared.dto.ApiResponse}.</li>
 *   <li>{@link com.platform.app.shared.security.RestAccessDeniedHandler}: Formats forbidden 403 Access Denied responses into standard JSON {@link com.platform.app.shared.dto.ApiResponse}.</li>
 * </ul>
 */
package com.platform.app.shared.security;
