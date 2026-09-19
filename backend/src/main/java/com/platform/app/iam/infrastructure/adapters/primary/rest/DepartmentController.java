package com.platform.app.iam.infrastructure.adapters.primary.rest;

import com.platform.app.iam.application.dto.DepartmentResponseDto;
import com.platform.app.iam.application.ports.inbound.CreateDepartmentCommand;
import com.platform.app.iam.application.ports.inbound.CreateDepartmentUseCase;
import com.platform.app.iam.application.ports.inbound.GetDepartmentUseCase;
import com.platform.app.iam.application.ports.inbound.UpdateDepartmentCommand;
import com.platform.app.iam.application.ports.inbound.UpdateDepartmentUseCase;
import com.platform.app.iam.infrastructure.adapters.primary.rest.dto.request.CreateDepartmentRequest;
import com.platform.app.iam.infrastructure.adapters.primary.rest.dto.request.UpdateDepartmentRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Slf4j
public class DepartmentController {

    private final CreateDepartmentUseCase createDepartmentUseCase;
    private final UpdateDepartmentUseCase updateDepartmentUseCase;
    private final GetDepartmentUseCase getDepartmentUseCase;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentResponseDto> createDepartment(
        @Valid @RequestBody CreateDepartmentRequest request,
        Authentication authentication
    ) {
        UUID operatorUserId = extractOperatorId(authentication);
        log.info(
            "REST POST /api/v1/departments requested with code [{}] by operator [{}]",
            request.code(),
            operatorUserId
        );
        CreateDepartmentCommand command = new CreateDepartmentCommand(
            request.code(),
            request.name(),
            request.description(),
            operatorUserId
        );

        DepartmentResponseDto created =
            createDepartmentUseCase.createDepartment(command);
        log.debug(
            "REST POST /api/v1/departments created department [{}] with id [{}]",
            created.code(),
            created.id()
        );
        URI location = URI.create("/api/v1/departments/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentResponseDto> updateDepartment(
        @PathVariable("id") UUID id,
        @Valid @RequestBody UpdateDepartmentRequest request,
        Authentication authentication
    ) {
        UUID operatorUserId = extractOperatorId(authentication);
        log.info(
            "REST PUT /api/v1/departments/{} requested with code [{}] by operator [{}]",
            id,
            request.code(),
            operatorUserId
        );
        UpdateDepartmentCommand command = new UpdateDepartmentCommand(
            id,
            request.code(),
            request.name(),
            request.description(),
            operatorUserId
        );

        DepartmentResponseDto updated =
            updateDepartmentUseCase.updateDepartment(command);
        log.debug("REST PUT /api/v1/departments/{} updated successfully", id);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentResponseDto> getDepartmentById(
        @PathVariable("id") UUID id
    ) {
        log.debug("REST GET /api/v1/departments/{}", id);
        DepartmentResponseDto department =
            getDepartmentUseCase.getDepartmentById(id);
        return ResponseEntity.ok(department);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DepartmentResponseDto>> listDepartments() {
        log.debug("REST GET /api/v1/departments list requested");
        List<DepartmentResponseDto> departments =
            getDepartmentUseCase.listDepartments();
        return ResponseEntity.ok(departments);
    }

    private UUID extractOperatorId(Authentication authentication) {
        if (authentication != null && authentication.getName() != null) {
            try {
                return UUID.fromString(authentication.getName());
            } catch (IllegalArgumentException _) {
                // Principal is not a UUID
            }
        }
        return null;
    }
}
