package com.platform.app.iam.application.ports.inbound;

import java.util.List;
import java.util.UUID;

import com.platform.app.iam.application.dto.DepartmentResponseDto;

public interface GetDepartmentUseCase {
  DepartmentResponseDto getDepartmentById(UUID departmentId);

  List<DepartmentResponseDto> listDepartments();
}
