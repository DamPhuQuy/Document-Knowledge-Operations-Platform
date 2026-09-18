package com.platform.app.document.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.platform.app.document.domain.model.AccessLevel;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocumentPermissionsResponseDto {

  private final UUID documentId;
  private final AccessLevel accessLevel;
  private final List<UserGrantDto> userGrants;
  private final List<DepartmentGrantDto> departmentGrants;
  private final List<RoleGrantDto> roleGrants;
  private final Instant updatedAt;
}
