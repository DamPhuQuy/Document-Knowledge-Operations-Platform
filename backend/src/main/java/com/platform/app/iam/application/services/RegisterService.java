package com.platform.app.iam.application.services;

import com.platform.app.iam.application.dto.UserProfileDto;
import com.platform.app.iam.application.dto.UserRegisteredOtpEvent;
import com.platform.app.iam.application.ports.inbound.RegisterCommand;
import com.platform.app.iam.application.ports.inbound.RegisterUseCase;
import com.platform.app.iam.application.ports.outbound.OtpRepositoryPort;
import com.platform.app.iam.application.ports.outbound.RoleRepositoryPort;
import com.platform.app.iam.application.ports.outbound.UserRepositoryPort;
import com.platform.app.iam.domain.exception.EmailAlreadyExistsException;
import com.platform.app.iam.domain.model.Role;
import com.platform.app.iam.domain.model.User;
import com.platform.app.shared.util.IdGenerator;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegisterService implements RegisterUseCase {

    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private final SecureRandom secureRandom = new SecureRandom();

    private final UserRepositoryPort userRepositoryPort;
    private final RoleRepositoryPort roleRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final OtpRepositoryPort otpRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public UserProfileDto execute(RegisterCommand command) {
        Objects.requireNonNull(command, "RegisterCommand must not be null");
        String email = command.email().trim().toLowerCase();

        if (userRepositoryPort.findByEmail(email).isPresent()) {
            log.warn(
                "Registration attempt rejected: email [{}] already exists",
                email
            );
            throw new EmailAlreadyExistsException(
                "Email already registered: " + email
            );
        }

        Role defaultRole = roleRepositoryPort
            .findByCode("ROLE_STAFF")
            .or(() -> roleRepositoryPort.findByCode("STAFF"))
            .orElse(null);
        Set<Role> roles = defaultRole != null ? Set.of(defaultRole) : Set.of();

        String passwordHash = passwordEncoder.encode(command.password());

        User user = User.builder()
            .id(IdGenerator.nextId())
            .email(email)
            .passwordHash(passwordHash)
            .fullName(command.getFullName())
            .departmentId(null)
            .enabled(false)
            .internal(true)
            .roles(roles)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        User savedUser = userRepositoryPort.save(user);
        log.info(
            "User registered pending OTP verification with ID [{}] and email [{}]",
            savedUser.getId(),
            savedUser.getEmail()
        );

        // Generate secure 6-digit OTP and store in Redis with 5 min TTL
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        otpRepositoryPort.saveOtp(email, otp, OTP_TTL);

        // Trigger async email dispatch
        eventPublisher.publishEvent(
            UserRegisteredOtpEvent.builder()
                .email(email)
                .otp(otp)
                .fullName(savedUser.getFullName())
                .build()
        );

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
