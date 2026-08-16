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
 *   <li>Enforcing contextual authorization rules based on user roles and ownership.</li>
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
 *     <b>Application Services / Use Case Handlers:</b><br>
 *     Coordinates domain operations, executes use cases, and manages transactional state.
 *   </li>
 *   <li>
 *     <b>Commands & Queries:</b><br>
 *     Input DTOs carrying validated use case parameters and command intent.
 *   </li>
 *   <li>
 *     <b>Response DTOs:</b><br>
 *     Output representations decoupled from internal domain model structures.
 *   </li>
 * </ul>
 */
package com.platform.app.module_template.application;
