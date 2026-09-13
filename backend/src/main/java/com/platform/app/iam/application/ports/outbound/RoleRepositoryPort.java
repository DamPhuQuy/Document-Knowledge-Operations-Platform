package com.platform.app.iam.application.ports.outbound;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.platform.app.iam.domain.model.Role;

public interface RoleRepositoryPort {
  Set<Role> findByIds(Set<UUID> ids);
  Optional<Role> findByCode(String code);
  List<Role> findAll();
}
