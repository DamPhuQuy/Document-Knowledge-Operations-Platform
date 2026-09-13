package com.platform.app.iam.application.ports.outbound;

import java.util.Optional;
import java.util.UUID;

import com.platform.app.iam.domain.model.User;

public interface UserRepositoryPort {
  Optional<User> findByEmail(String email);
  Optional<User> findById(UUID id);
  User save(User user);
}
