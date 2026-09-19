package com.platform.app.iam.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.platform.app.iam.application.dto.UserRolesResponseDto;
import com.platform.app.iam.application.dto.UserRolesUpdatedEvent;
import com.platform.app.iam.application.ports.inbound.AssignRolesCommand;
import com.platform.app.iam.application.ports.outbound.RoleRepositoryPort;
import com.platform.app.iam.application.ports.outbound.UserRepositoryPort;
import com.platform.app.iam.domain.exception.EmptyRolesException;
import com.platform.app.iam.domain.exception.SelfRoleRevocationException;
import com.platform.app.iam.domain.model.Permission;
import com.platform.app.iam.domain.model.Role;
import com.platform.app.iam.domain.model.User;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AssignRolesServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private RoleRepositoryPort roleRepositoryPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AssignRolesService assignRolesService;

    @BeforeEach
    void setUp() {
        assignRolesService = new AssignRolesService(
            userRepositoryPort,
            roleRepositoryPort,
            eventPublisher
        );
    }

    @Test
    @DisplayName("Should retrieve user roles and effective permissions")
    void shouldGetUserRoles() {
        UUID userId = UUID.randomUUID();
        Permission p1 = Permission.builder()
            .id(UUID.randomUUID())
            .code("read:users")
            .name("Read Users")
            .module("IAM")
            .build();
        Role role = Role.builder()
            .id(UUID.randomUUID())
            .code("ROLE_STAFF")
            .name("Staff")
            .permissions(Set.of(p1))
            .build();
        User user = User.builder()
            .id(userId)
            .email("staff@platform.com")
            .passwordHash("hash")
            .fullName("Staff User")
            .roles(Set.of(role))
            .enabled(true)
            .internal(true)
            .build();

        when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));

        UserRolesResponseDto response = assignRolesService.getUserRoles(userId);

        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertEquals("staff@platform.com", response.email());
        assertEquals(1, response.roles().size());
        assertTrue(response.permissions().contains("READ:USERS"));
    }

    @Test
    @DisplayName(
        "Should throw IllegalArgumentException when user not found on getUserRoles"
    )
    void shouldThrowWhenUserNotFoundOnGet() {
        UUID userId = UUID.randomUUID();
        when(userRepositoryPort.findById(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            assignRolesService.getUserRoles(userId)
        );
    }

    @Test
    @DisplayName("Should assign roles successfully and publish audit event")
    void shouldAssignRolesSuccessfully() {
        UUID userId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();
        UUID oldRoleId = UUID.randomUUID();
        UUID newRoleId = UUID.randomUUID();

        Role oldRole = Role.builder()
            .id(oldRoleId)
            .code("ROLE_STAFF")
            .name("Staff")
            .permissions(Set.of())
            .build();
        Role newRole = Role.builder()
            .id(newRoleId)
            .code("ROLE_MANAGER")
            .name("Manager")
            .permissions(Set.of())
            .build();

        User user = User.builder()
            .id(userId)
            .email("user@platform.com")
            .passwordHash("hash")
            .fullName("Platform User")
            .roles(Set.of(oldRole))
            .enabled(true)
            .internal(true)
            .build();

        when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepositoryPort.findByIds(Set.of(newRoleId))).thenReturn(
            Set.of(newRole)
        );
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation ->
            invocation.getArgument(0)
        );

        AssignRolesCommand command = new AssignRolesCommand(
            userId,
            Set.of(newRoleId),
            operatorId
        );

        UserRolesResponseDto response = assignRolesService.assignRoles(command);

        assertNotNull(response);
        assertEquals(1, response.roles().size());
        assertEquals("ROLE_MANAGER", response.roles().iterator().next().code());

        ArgumentCaptor<UserRolesUpdatedEvent> eventCaptor =
            ArgumentCaptor.forClass(UserRolesUpdatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        UserRolesUpdatedEvent publishedEvent = eventCaptor.getValue();
        assertEquals(userId, publishedEvent.targetUserId());
        assertEquals(operatorId, publishedEvent.operatorUserId());
        assertTrue(publishedEvent.oldRoleCodes().contains("ROLE_STAFF"));
        assertTrue(publishedEvent.newRoleCodes().contains("ROLE_MANAGER"));
    }

    @Test
    @DisplayName(
        "Should throw IllegalArgumentException when target role does not exist"
    )
    void shouldThrowWhenRoleNotFound() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        User user = User.builder()
            .id(userId)
            .email("user@platform.com")
            .passwordHash("hash")
            .fullName("Platform User")
            .roles(Set.of())
            .enabled(true)
            .internal(true)
            .build();

        when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepositoryPort.findByIds(Set.of(roleId))).thenReturn(Set.of()); // Role not found

        AssignRolesCommand command = new AssignRolesCommand(
            userId,
            Set.of(roleId),
            UUID.randomUUID()
        );

        assertThrows(IllegalArgumentException.class, () ->
            assignRolesService.assignRoles(command)
        );
    }

    @Test
    @DisplayName("Should throw EmptyRolesException when empty roles passed")
    void shouldThrowEmptyRolesException() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
            .id(userId)
            .email("user@platform.com")
            .passwordHash("hash")
            .fullName("Platform User")
            .roles(Set.of())
            .enabled(true)
            .internal(true)
            .build();

        when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepositoryPort.findByIds(Set.of())).thenReturn(Set.of());

        AssignRolesCommand command = new AssignRolesCommand(
            userId,
            Set.of(),
            UUID.randomUUID()
        );

        assertThrows(EmptyRolesException.class, () ->
            assignRolesService.assignRoles(command)
        );
    }

    @Test
    @DisplayName(
        "Should propagate SelfRoleRevocationException when admin revokes own admin role"
    )
    void shouldThrowSelfRoleRevocationException() {
        UUID adminId = UUID.randomUUID();
        UUID staffRoleId = UUID.randomUUID();

        Role adminRole = Role.builder()
            .id(UUID.randomUUID())
            .code("ROLE_ADMIN")
            .name("Admin")
            .permissions(Set.of())
            .build();
        Role staffRole = Role.builder()
            .id(staffRoleId)
            .code("ROLE_STAFF")
            .name("Staff")
            .permissions(Set.of())
            .build();

        User adminUser = User.builder()
            .id(adminId)
            .email("admin@platform.com")
            .passwordHash("hash")
            .fullName("Admin User")
            .roles(Set.of(adminRole))
            .enabled(true)
            .internal(true)
            .build();

        when(userRepositoryPort.findById(adminId)).thenReturn(
            Optional.of(adminUser)
        );
        when(roleRepositoryPort.findByIds(Set.of(staffRoleId))).thenReturn(
            Set.of(staffRole)
        );

        AssignRolesCommand command = new AssignRolesCommand(
            adminId,
            Set.of(staffRoleId),
            adminId
        );

        assertThrows(SelfRoleRevocationException.class, () ->
            assignRolesService.assignRoles(command)
        );
    }
}
