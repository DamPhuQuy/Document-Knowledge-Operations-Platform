package com.platform.app.iam.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.platform.app.iam.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
