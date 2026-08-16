/**
 * <h1>Shared DTOs & Response Envelopes</h1>
 *
 * <h2>1. Role & Responsibility</h2>
 * <p>
 * Defines unified REST API payload contracts and response envelopes used consistently across all HTTP endpoints.
 * </p>
 *
 * <h2>2. Expected Components</h2>
 * <ul>
 *   <li>{@link com.platform.app.shared.dto.ApiResponse}: Standard generic API response wrapper encapsulating success status, message, payload data, timestamp, and optional validation errors.</li>
 *   <li>{@link com.platform.app.shared.dto.PageResponse}: Standard pagination wrapper providing items, page number, page size, total elements, total pages, and last page indicator.</li>
 * </ul>
 */
package com.platform.app.shared.dto;
