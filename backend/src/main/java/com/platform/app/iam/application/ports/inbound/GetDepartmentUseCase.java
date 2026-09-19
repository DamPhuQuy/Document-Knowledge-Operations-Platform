package com.platform.app.iam.application.ports.inbound;

import com.platform.app.iam.application.dto.DepartmentResponseDto;
import java.util.List;
import java.util.UUID;

public interface GetDepartmentUseCase {
    DepartmentResponseDto getDepartmentById(UUID departmentId);

    List<DepartmentResponseDto> listDepartments();
}
