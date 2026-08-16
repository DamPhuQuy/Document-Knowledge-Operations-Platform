/**
 * <h1>Shared Configuration Infrastructure</h1>
 *
 * <h2>1. Role & Responsibility</h2>
 * <p>
 * Houses application-wide Spring {@code @Configuration} beans, including security filter chains,
 * CORS policy sources, OpenAPI Swagger UI documentation, JSON serialization, and JPA auditing.
 * </p>
 *
 * <h2>2. Expected Components</h2>
 * <ul>
 *   <li>{@link com.platform.app.shared.config.SecurityConfig}: Configures Spring Security stateless session management, CSRF protection, URL authorization whitelists, and JWT filter registration.</li>
 *   <li>{@link com.platform.app.shared.config.CorsConfig}: Defines global Cross-Origin Resource Sharing (CORS) source configurations.</li>
 *   <li>{@link com.platform.app.shared.config.OpenApiConfig}: Configures Swagger/OpenAPI documentation with Bearer JWT SecurityScheme.</li>
 *   <li>{@link com.platform.app.shared.config.JacksonConfig}: Customizes {@link com.fasterxml.jackson.databind.ObjectMapper} with JSR-310 JavaTimeModule and ISO-8601 timestamps.</li>
 *   <li>{@link com.platform.app.shared.config.JpaAuditingConfig}: Enables Spring Data JPA auditing for automatic timestamp management.</li>
 *   <li>{@link com.platform.app.shared.config.properties.AppProperties}: Strongly typed configuration properties mapped from environment variables.</li>
 * </ul>
 */
package com.platform.app.shared.config;
