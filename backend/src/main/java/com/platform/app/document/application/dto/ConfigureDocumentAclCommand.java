package com.platform.app.document.application.dto;

import com.platform.app.document.domain.model.AccessLevel;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConfigureDocumentAclCommand {

    private final UUID documentId;
    private final UUID currentUserId;
    private final boolean isAdmin;
    private final boolean hasManagePermissions;
    private final AccessLevel accessLevel;
    private final List<UserGrantDto> userGrants;
    private final List<DepartmentGrantDto> departmentGrants;
    private final List<RoleGrantDto> roleGrants;
}
