package com.platform.app.iam.application.ports.inbound;

import com.platform.app.iam.application.dto.UserRolesResponseDto;
import java.util.UUID;

public interface GetUserRolesUseCase {
    UserRolesResponseDto getUserRoles(UUID userId);
}
