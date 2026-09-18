package com.platform.app.document.application.ports.outbound;

import java.util.List;
import java.util.UUID;

import com.platform.app.document.domain.model.DocumentDepartmentAccess;
import com.platform.app.document.domain.model.DocumentRoleAccess;
import com.platform.app.document.domain.model.DocumentUserAccess;
import com.platform.app.document.domain.model.PermissionLevel;

public interface DocumentAclRepositoryPort {

  void replacePermissions(
      UUID documentId,
      List<DocumentUserAccess> userGrants,
      List<DocumentDepartmentAccess> departmentGrants,
      List<DocumentRoleAccess> roleGrants);

  List<DocumentUserAccess> findUserGrants(UUID documentId);

  List<DocumentDepartmentAccess> findDepartmentGrants(UUID documentId);

  List<DocumentRoleAccess> findRoleGrants(UUID documentId);

  boolean hasUserPermission(UUID documentId, UUID userId, PermissionLevel requiredLevel);
}
