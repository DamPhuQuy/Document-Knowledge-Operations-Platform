package com.platform.app.iam.infrastructure.adapters.secondary.persistence.repository;

import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.UserJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataUserRepository
    extends JpaRepository<UserJpaEntity, UUID>
{
    @Query(
        "SELECT DISTINCT u FROM UserJpaEntity u " +
            "LEFT JOIN FETCH u.roles r " +
            "LEFT JOIN FETCH r.permissions p " +
            "WHERE LOWER(u.email) = LOWER(:email)"
    )
    Optional<UserJpaEntity> findByEmailIgnoreCaseWithRolesAndPermissions(
        @Param("email") String email
    );

    @Query(
        "SELECT DISTINCT u FROM UserJpaEntity u " +
            "LEFT JOIN FETCH u.roles r " +
            "LEFT JOIN FETCH r.permissions p " +
            "WHERE u.id = :id"
    )
    Optional<UserJpaEntity> findByIdWithRolesAndPermissions(
        @Param("id") UUID id
    );
}
