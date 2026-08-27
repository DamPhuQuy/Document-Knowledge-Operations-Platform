package com.platform.app.system.web;

import java.time.Instant;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Tag(name = "HealthCheckResponse", description = "Response object for system health check")
public class HealthCheckResponse {

    private String status;
    private String service;
    private String environment;
    private Instant timestamp;
    private long uptimeSeconds;
}
