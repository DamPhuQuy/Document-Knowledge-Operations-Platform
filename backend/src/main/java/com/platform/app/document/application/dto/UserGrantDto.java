package com.platform.app.document.application.dto;

import java.util.UUID;

import com.platform.app.document.domain.model.PermissionLevel;

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
public class UserGrantDto {

  @NotNull(message = "User ID must not be null")
  private UUID userId;

  @NotNull(message = "Permission level must not be null")
  private PermissionLevel permissionLevel;
}
