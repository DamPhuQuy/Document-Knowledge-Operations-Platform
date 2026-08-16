package com.platform.app.system.web;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthCheckResponse {

    private String status;
    private String service;
    private String environment;
    private Instant timestamp;
    private long uptimeSeconds;
}
