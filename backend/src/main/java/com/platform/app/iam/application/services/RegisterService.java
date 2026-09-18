package com.platform.app.iam.application.services;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.platform.app.iam.application.dto.UserProfileDto;
import com.platform.app.iam.application.ports.inbound.RegisterCommand;
import com.platform.app.iam.application.ports.inbound.RegisterUseCase;
import com.platform.app.iam.application.ports.outbound.RoleRepositoryPort;
import com.platform.app.iam.application.ports.outbound.UserRepositoryPort;
import com.platform.app.iam.domain.exception.EmailAlreadyExistsException;
import com.platform.app.iam.domain.model.Role;
import com.platform.app.iam.domain.model.User;
import com.platform.app.shared.util.IdGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegisterService implements RegisterUseCase {

  private final UserRepositoryPort userRepositoryPort;
  private final RoleRepositoryPort roleRepositoryPort;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public UserProfileDto execute(RegisterCommand command) {
    Objects.requireNonNull(command, "RegisterCommand must not be null");
    String email = command.email().trim().toLowerCase();

    if (userRepositoryPort.findByEmail(email).isPresent()) {
      log.warn("Registration attempt rejected: email [{}] already exists", email);
      throw new EmailAlreadyExistsException("Email already registered: " + email);
    }

    Role defaultRole =
        roleRepositoryPort
            .findByCode("ROLE_STAFF")
            .or(() -> roleRepositoryPort.findByCode("STAFF"))
            .orElse(null);
    Set<Role> roles = defaultRole != null ? Set.of(defaultRole) : Set.of();

    String passwordHash = passwordEncoder.encode(command.password());

    User user =
        User.builder()
            .id(IdGenerator.nextId())
            .email(email)
            .passwordHash(passwordHash)
            .fullName(command.getFullName())
            .departmentId(null)
            .enabled(true)
            .internal(true)
            .roles(roles)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    User savedUser = userRepositoryPort.save(user);
    log.info("User registered successfully with ID [{}] and email [{}]", savedUser.getId(), savedUser.getEmail());

    return UserProfileDto.builder()
        .id(savedUser.getId())
        .email(savedUser.getEmail())
        .fullName(savedUser.getFullName())
        .departmentId(savedUser.getDepartmentId())
        .isInternal(savedUser.isInternal())
        .roles(savedUser.getRoleCodes())
        .permissions(savedUser.getAllPermissionCodes())
        .build();
  }
}
