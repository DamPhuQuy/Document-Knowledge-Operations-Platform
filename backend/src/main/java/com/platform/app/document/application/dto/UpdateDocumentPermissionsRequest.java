package com.platform.app.document.application.dto;

import java.util.List;

import com.platform.app.document.domain.model.AccessLevel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
public class UpdateDocumentPermissionsRequest {

  @NotNull(message = "Access level must not be null")
  private AccessLevel accessLevel;

  @Valid
  private List<UserGrantDto> userGrants;

  @Valid
  private List<DepartmentGrantDto> departmentGrants;

  @Valid
  private List<RoleGrantDto> roleGrants;
}
