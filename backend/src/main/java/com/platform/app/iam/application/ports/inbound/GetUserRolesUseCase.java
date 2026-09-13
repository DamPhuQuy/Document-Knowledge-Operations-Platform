package com.platform.app.iam.application.ports.inbound;

import java.util.UUID;

import com.platform.app.iam.application.dto.UserRolesResponseDto;

public interface GetUserRolesUseCase {
  UserRolesResponseDto getUserRoles(UUID userId);
}
