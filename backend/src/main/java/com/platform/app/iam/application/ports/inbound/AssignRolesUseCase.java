package com.platform.app.iam.application.ports.inbound;

import com.platform.app.iam.application.dto.UserRolesResponseDto;

public interface AssignRolesUseCase {
    UserRolesResponseDto assignRoles(AssignRolesCommand command);
}
