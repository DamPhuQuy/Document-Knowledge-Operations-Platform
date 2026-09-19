package com.platform.app.document.application.dto;

import com.platform.app.document.domain.model.PermissionLevel;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentGrantDto {

    @NotNull(message = "Department ID must not be null")
    private UUID departmentId;

    @NotNull(message = "Permission level must not be null")
    private PermissionLevel permissionLevel;
}
