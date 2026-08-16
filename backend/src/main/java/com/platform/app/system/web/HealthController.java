package com.platform.app.system.web;

import com.platform.app.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "System", description = "System health and diagnostic endpoints")
public class HealthController {

    @Value("${spring.application.name:customer-support-platform}")
    private String applicationName;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @GetMapping
    @Operation(summary = "System Health Check", description = "Returns service health status and runtime information")
    public ResponseEntity<ApiResponse<HealthCheckResponse>> checkHealth() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();

        HealthCheckResponse health = HealthCheckResponse.builder()
                .status("UP")
                .service(applicationName)
                .environment(activeProfile)
                .timestamp(Instant.now())
                .uptimeSeconds(uptimeMs / 1000)
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Service is running smoothly", health));
    }
}
