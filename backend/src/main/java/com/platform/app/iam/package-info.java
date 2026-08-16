/**
 * <h1>Business Capability: Identity & Access Management (IAM)</h1>
 *
 * <h2>1. Role & Responsibility</h2>
 * <p>
 * Manages user accounts, authentication credentials, authorization roles ({@code ADMIN}, {@code AGENT}, {@code CUSTOMER}),
 * password hashing (BCrypt), user registration, user login, and JWT access/refresh token issuance.
 * </p>
 *
 * <h2>2. Architectural Style: Pragmatic Layered Capability</h2>
 * <p>
 * Standard identity workflows are predominantly CRUD and password hashing backed by Spring Security.
 * To avoid unnecessary boilerplate, IAM is organized in a clean, pragmatic 3-layer architecture:
 * <b>Web Controller &rarr; Application Service &rarr; Spring Data Repository</b>.
 * </p>
 *
 * <h2>3. Dependency Rules</h2>
 * <ul>
 *   <li><b>Allowed:</b> Depends on {@code shared} for base entities, response DTOs, security filters, and exception handling.</li>
 *   <li><b>Strictly Forbidden:</b> Must NOT depend on {@code ticket} or any other specific business capability.</li>
 * </ul>
 *
 * <h2>4. Expected Components</h2>
 * <ul>
 *   <li>{@code iam.domain}: {@link com.platform.app.iam.domain.User}, {@link com.platform.app.iam.domain.Role}</li>
 *   <li>{@code iam.repository}: {@link com.platform.app.iam.repository.UserRepository}</li>
 *   <li>{@code iam.service}: {@link com.platform.app.iam.service.IamService}, {@link com.platform.app.iam.service.CustomUserDetailsService}</li>
 *   <li>{@code iam.web}: {@link com.platform.app.iam.web.AuthController}, DTOs (LoginRequest, RegisterRequest, AuthResponse, UserResponse)</li>
 * </ul>
 */
package com.platform.app.iam;
