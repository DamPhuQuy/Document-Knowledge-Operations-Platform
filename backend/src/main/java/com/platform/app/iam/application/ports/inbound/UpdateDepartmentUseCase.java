package com.platform.app.iam.application.ports.inbound;

import com.platform.app.iam.application.dto.DepartmentResponseDto;

public interface UpdateDepartmentUseCase {
    DepartmentResponseDto updateDepartment(UpdateDepartmentCommand command);
}
