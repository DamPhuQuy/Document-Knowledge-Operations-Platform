package com.platform.app.iam.infrastructure.adapters.secondary.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.RoleJpaEntity;

public interface SpringDataRoleRepository extends JpaRepository<RoleJpaEntity, UUID> {

  @Query(
      "SELECT DISTINCT r FROM RoleJpaEntity r "
          + "LEFT JOIN FETCH r.permissions p "
          + "WHERE r.id IN :ids")
  Set<RoleJpaEntity> findByIdInWithPermissions(@Param("ids") Collection<UUID> ids);

  @Query(
      "SELECT DISTINCT r FROM RoleJpaEntity r "
          + "LEFT JOIN FETCH r.permissions p "
          + "WHERE UPPER(r.code) = UPPER(:code)")
  Optional<RoleJpaEntity> findByCodeIgnoreCaseWithPermissions(@Param("code") String code);

  @Query(
      "SELECT DISTINCT r FROM RoleJpaEntity r "
          + "LEFT JOIN FETCH r.permissions p")
  List<RoleJpaEntity> findAllWithPermissions();
}
