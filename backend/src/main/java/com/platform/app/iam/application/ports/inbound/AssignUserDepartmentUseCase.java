package com.platform.app.iam.application.ports.inbound;

import com.platform.app.iam.application.dto.UserDepartmentResponseDto;

public interface AssignUserDepartmentUseCase {
  UserDepartmentResponseDto assignUserDepartment(AssignUserDepartmentCommand command);
}
