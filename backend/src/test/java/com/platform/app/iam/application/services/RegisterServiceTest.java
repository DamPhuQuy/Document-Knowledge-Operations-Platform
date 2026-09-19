package com.platform.app.iam.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.platform.app.iam.application.dto.UserProfileDto;
import com.platform.app.iam.application.dto.UserRegisteredOtpEvent;
import com.platform.app.iam.application.ports.inbound.RegisterCommand;
import com.platform.app.iam.application.ports.outbound.OtpRepositoryPort;
import com.platform.app.iam.application.ports.outbound.RoleRepositoryPort;
import com.platform.app.iam.application.ports.outbound.UserRepositoryPort;
import com.platform.app.iam.domain.exception.EmailAlreadyExistsException;
import com.platform.app.iam.domain.model.Role;
import com.platform.app.iam.domain.model.User;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private RoleRepositoryPort roleRepositoryPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpRepositoryPort otpRepositoryPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Captor
    private ArgumentCaptor<UserRegisteredOtpEvent> eventCaptor;

    @Captor
    private ArgumentCaptor<String> otpCaptor;

    private RegisterService registerService;

    @BeforeEach
    void setUp() {
        registerService = new RegisterService(
            userRepositoryPort,
            roleRepositoryPort,
            passwordEncoder,
            otpRepositoryPort,
            eventPublisher
        );
    }

    @Test
    @DisplayName("Should successfully register user with default ROLE_STAFF role")
    void shouldRegisterUserSuccessfullyWithDefaultRoleStaff() {
        // Arrange
        RegisterCommand command = RegisterCommand.builder()
            .email("NEW.USER@Platform.COM")
            .password("RawPassword#123")
            .firstName("John")
            .lastName("Doe")
            .build();

        Role staffRole = Role.builder()
            .id(UUID.randomUUID())
            .code("ROLE_STAFF")
            .name("Staff")
            .permissions(Set.of())
            .build();

        when(userRepositoryPort.findByEmail("new.user@platform.com")).thenReturn(Optional.empty());
        when(roleRepositoryPort.findByCode("ROLE_STAFF")).thenReturn(Optional.of(staffRole));
        when(passwordEncoder.encode("RawPassword#123")).thenReturn("hashed_password_123");
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserProfileDto result = registerService.execute(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("new.user@platform.com");
        assertThat(result.fullName()).isEqualTo("John Doe");
        assertThat(result.roles()).containsExactly("ROLE_STAFF");
        assertThat(result.departmentId()).isNull();
        assertThat(result.isInternal()).isTrue();

        verify(userRepositoryPort).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("new.user@platform.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("hashed_password_123");
        assertThat(savedUser.isEnabled()).isFalse(); // Pending OTP verification
        assertThat(savedUser.isInternal()).isTrue();

        verify(otpRepositoryPort).saveOtp(eq("new.user@platform.com"), otpCaptor.capture(), eq(Duration.ofMinutes(5)));
        String generatedOtp = otpCaptor.getValue();
        assertThat(generatedOtp).matches("^\\d{6}$");

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        UserRegisteredOtpEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.email()).isEqualTo("new.user@platform.com");
        assertThat(publishedEvent.otp()).isEqualTo(generatedOtp);
        assertThat(publishedEvent.fullName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should fallback to STAFF role if ROLE_STAFF not found")
    void shouldFallbackToStaffRoleWhenRoleStaffNotFound() {
        // Arrange
        RegisterCommand command = RegisterCommand.builder()
            .email("staff@platform.com")
            .password("Password#123")
            .firstName("Alice")
            .lastName(null)
            .build();

        Role fallbackRole = Role.builder()
            .id(UUID.randomUUID())
            .code("STAFF")
            .name("Staff")
            .permissions(Set.of())
            .build();

        when(userRepositoryPort.findByEmail("staff@platform.com")).thenReturn(Optional.empty());
        when(roleRepositoryPort.findByCode("ROLE_STAFF")).thenReturn(Optional.empty());
        when(roleRepositoryPort.findByCode("STAFF")).thenReturn(Optional.of(fallbackRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserProfileDto result = registerService.execute(command);

        // Assert
        assertThat(result.fullName()).isEqualTo("Alice");
        assertThat(result.roles()).containsExactly("STAFF");
    }

    @Test
    @DisplayName("Should allow registration with empty roles if no staff role exists")
    void shouldAllowRegistrationWithEmptyRolesWhenNoRoleExists() {
        // Arrange
        RegisterCommand command = RegisterCommand.builder()
            .email("norole@platform.com")
            .password("Password#123")
            .firstName("Bob")
            .lastName("Smith")
            .build();

        when(userRepositoryPort.findByEmail("norole@platform.com")).thenReturn(Optional.empty());
        when(roleRepositoryPort.findByCode("ROLE_STAFF")).thenReturn(Optional.empty());
        when(roleRepositoryPort.findByCode("STAFF")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserProfileDto result = registerService.execute(command);

        // Assert
        assertThat(result.roles()).isEmpty();
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when email already registered")
    void shouldThrowEmailAlreadyExistsExceptionWhenEmailExists() {
        // Arrange
        RegisterCommand command = RegisterCommand.builder()
            .email("existing@platform.com")
            .password("Password#123")
            .firstName("Existing")
            .lastName("User")
            .build();

        User existingUser = User.builder()
            .id(UUID.randomUUID())
            .email("existing@platform.com")
            .passwordHash("hash")
            .fullName("Existing User")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(userRepositoryPort.findByEmail("existing@platform.com")).thenReturn(Optional.of(existingUser));

        // Act & Assert
        assertThatThrownBy(() -> registerService.execute(command))
            .isInstanceOf(EmailAlreadyExistsException.class)
            .hasMessageContaining("Email already registered: existing@platform.com");

        verify(userRepositoryPort, never()).save(any());
        verify(otpRepositoryPort, never()).saveOtp(any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Should throw NullPointerException when command is null")
    void shouldThrowNullPointerExceptionWhenCommandIsNull() {
        assertThatThrownBy(() -> registerService.execute(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("RegisterCommand must not be null");
    }
}
