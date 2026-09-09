package com.platform.app.iam.application.ports.outbound;

import com.platform.app.iam.domain.model.User;
import java.util.Optional;

public interface UserRepositoryPort {
  Optional<User> findByEmail(String email);
}
