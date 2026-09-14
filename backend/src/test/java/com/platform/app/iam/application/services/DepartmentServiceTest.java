package com.platform.app.iam.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
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

import com.platform.app.iam.application.dto.DepartmentCreatedEvent;
import com.platform.app.iam.application.dto.DepartmentResponseDto;
import com.platform.app.iam.application.dto.DepartmentUpdatedEvent;
import com.platform.app.iam.application.dto.UserDepartmentAssignedEvent;
import com.platform.app.iam.application.dto.UserDepartmentResponseDto;
import com.platform.app.iam.application.ports.inbound.AssignUserDepartmentCommand;
import com.platform.app.iam.application.ports.inbound.CreateDepartmentCommand;
import com.platform.app.iam.application.ports.inbound.UpdateDepartmentCommand;
import com.platform.app.iam.application.ports.outbound.DepartmentRepositoryPort;
import com.platform.app.iam.application.ports.outbound.UserRepositoryPort;
import com.platform.app.iam.domain.exception.DepartmentCodeConflictException;
import com.platform.app.iam.domain.exception.DepartmentNotFoundException;
import com.platform.app.iam.domain.exception.InvalidDepartmentCodeException;
import com.platform.app.iam.domain.model.Department;
import com.platform.app.iam.domain.model.User;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

  @Mock private DepartmentRepositoryPort departmentRepositoryPort;
  @Mock private UserRepositoryPort userRepositoryPort;
  @Mock private ApplicationEventPublisher eventPublisher;

  private DepartmentService departmentService;

  @BeforeEach
  void setUp() {
    departmentService =
        new DepartmentService(departmentRepositoryPort, userRepositoryPort, eventPublisher);
  }

  @Test
  @DisplayName("Should create department successfully and publish DepartmentCreatedEvent")
  void shouldCreateDepartmentSuccessfully() {
    UUID operatorId = UUID.randomUUID();
    CreateDepartmentCommand cmd =
        new CreateDepartmentCommand("HR", "Human Resources", "Handles hiring", operatorId);

    when(departmentRepositoryPort.existsByCode("HR")).thenReturn(false);
    when(departmentRepositoryPort.save(any(Department.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DepartmentResponseDto response = departmentService.createDepartment(cmd);

    assertNotNull(response);
    assertEquals("HR", response.code());
    assertEquals("Human Resources", response.name());
    assertEquals("Handles hiring", response.description());

    ArgumentCaptor<DepartmentCreatedEvent> eventCaptor =
        ArgumentCaptor.forClass(DepartmentCreatedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    DepartmentCreatedEvent event = eventCaptor.getValue();
    assertEquals("HR", event.code());
    assertEquals(operatorId, event.operatorUserId());
  }

  @Test
  @DisplayName("Should reject duplicate department code with DepartmentCodeConflictException")
  void shouldRejectDuplicateCodeOnCreate() {
    CreateDepartmentCommand cmd =
        new CreateDepartmentCommand("HR", "Human Resources", "Handles hiring", UUID.randomUUID());

    when(departmentRepositoryPort.existsByCode("HR")).thenReturn(true);

    assertThrows(
        DepartmentCodeConflictException.class,
        () -> departmentService.createDepartment(cmd));
  }

  @Test
  @DisplayName("Should reject invalid code with InvalidDepartmentCodeException")
  void shouldRejectInvalidCodeOnCreate() {
    CreateDepartmentCommand cmd =
        new CreateDepartmentCommand("invalid-code", "Invalid Dept", "Desc", UUID.randomUUID());

    assertThrows(
        InvalidDepartmentCodeException.class,
        () -> departmentService.createDepartment(cmd));
  }

  @Test
  @DisplayName("Should update department successfully and publish DepartmentUpdatedEvent")
  void shouldUpdateDepartmentSuccessfully() {
    UUID deptId = UUID.randomUUID();
    UUID operatorId = UUID.randomUUID();
    Department existing =
        Department.builder()
            .id(deptId)
            .code("HR")
            .name("HR Old")
            .description("Desc Old")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    when(departmentRepositoryPort.findById(deptId)).thenReturn(Optional.of(existing));
    when(departmentRepositoryPort.existsByCodeAndIdNot("PEOPLE", deptId)).thenReturn(false);
    when(departmentRepositoryPort.save(any(Department.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    UpdateDepartmentCommand cmd =
        new UpdateDepartmentCommand(deptId, "PEOPLE", "People & Culture", "New Desc", operatorId);

    DepartmentResponseDto response = departmentService.updateDepartment(cmd);

    assertNotNull(response);
    assertEquals("PEOPLE", response.code());
    assertEquals("People & Culture", response.name());

    ArgumentCaptor<DepartmentUpdatedEvent> eventCaptor =
        ArgumentCaptor.forClass(DepartmentUpdatedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    DepartmentUpdatedEvent event = eventCaptor.getValue();
    assertEquals("PEOPLE", event.code());
    assertEquals(operatorId, event.operatorUserId());
  }

  @Test
  @DisplayName("Should reject duplicate code on department update")
  void shouldRejectDuplicateCodeOnUpdate() {
    UUID deptId = UUID.randomUUID();
    Department existing =
        Department.builder()
            .id(deptId)
            .code("HR")
            .name("HR")
            .build();

    when(departmentRepositoryPort.findById(deptId)).thenReturn(Optional.of(existing));
    when(departmentRepositoryPort.existsByCodeAndIdNot("FIN", deptId)).thenReturn(true);

    UpdateDepartmentCommand cmd =
        new UpdateDepartmentCommand(deptId, "FIN", "Finance", "Desc", UUID.randomUUID());

    assertThrows(
        DepartmentCodeConflictException.class,
        () -> departmentService.updateDepartment(cmd));
  }

  @Test
  @DisplayName("Should throw DepartmentNotFoundException when updating non-existent department")
  void shouldThrowNotFoundOnUpdate() {
    UUID deptId = UUID.randomUUID();
    when(departmentRepositoryPort.findById(deptId)).thenReturn(Optional.empty());

    UpdateDepartmentCommand cmd =
        new UpdateDepartmentCommand(deptId, "FIN", "Finance", "Desc", UUID.randomUUID());

    assertThrows(
        DepartmentNotFoundException.class,
        () -> departmentService.updateDepartment(cmd));
  }

  @Test
  @DisplayName("Should get department by ID and list all departments")
  void shouldGetDepartmentByIdAndList() {
    UUID deptId = UUID.randomUUID();
    Department dept =
        Department.builder()
            .id(deptId)
            .code("IT")
            .name("Information Technology")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    when(departmentRepositoryPort.findById(deptId)).thenReturn(Optional.of(dept));
    when(departmentRepositoryPort.findAll()).thenReturn(List.of(dept));

    DepartmentResponseDto single = departmentService.getDepartmentById(deptId);
    assertEquals(deptId, single.id());
    assertEquals("IT", single.code());

    List<DepartmentResponseDto> list = departmentService.listDepartments();
    assertEquals(1, list.size());
    assertEquals("IT", list.get(0).code());
  }

  @Test
  @DisplayName("Should assign user department and internal flag and publish UserDepartmentAssignedEvent")
  void shouldAssignUserDepartmentSuccessfully() {
    UUID userId = UUID.randomUUID();
    UUID deptId = UUID.randomUUID();
    UUID operatorId = UUID.randomUUID();

    User user =
        User.builder()
            .id(userId)
            .email("user@platform.com")
            .passwordHash("hash")
            .fullName("Jane Doe")
            .departmentId(null)
            .enabled(true)
            .internal(true)
            .roles(Set.of())
            .build();

    Department dept =
        Department.builder()
            .id(deptId)
            .code("LEGAL")
            .name("Legal & Compliance")
            .build();

    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
    when(departmentRepositoryPort.findById(deptId)).thenReturn(Optional.of(dept));
    when(userRepositoryPort.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    AssignUserDepartmentCommand cmd =
        new AssignUserDepartmentCommand(userId, deptId, false, operatorId);

    UserDepartmentResponseDto response = departmentService.assignUserDepartment(cmd);

    assertNotNull(response);
    assertEquals(userId, response.userId());
    assertEquals(deptId, response.departmentId());
    assertEquals("LEGAL", response.departmentCode());
    assertEquals("Legal & Compliance", response.departmentName());
    assertFalse(response.internal());

    ArgumentCaptor<UserDepartmentAssignedEvent> eventCaptor =
        ArgumentCaptor.forClass(UserDepartmentAssignedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    UserDepartmentAssignedEvent event = eventCaptor.getValue();
    assertEquals(userId, event.targetUserId());
    assertEquals(deptId, event.departmentId());
    assertFalse(event.internal());
    assertEquals(operatorId, event.operatorUserId());
  }

  @Test
  @DisplayName("Should throw DepartmentNotFoundException when assigning user to non-existent department (NF1)")
  void shouldThrowNotFoundWhenAssigningToMissingDepartment() {
    UUID userId = UUID.randomUUID();
    UUID deptId = UUID.randomUUID();

    User user =
        User.builder()
            .id(userId)
            .email("user@platform.com")
            .passwordHash("hash")
            .fullName("Jane Doe")
            .departmentId(null)
            .enabled(true)
            .internal(true)
            .roles(Set.of())
            .build();

    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
    when(departmentRepositoryPort.findById(deptId)).thenReturn(Optional.empty());

    AssignUserDepartmentCommand cmd =
        new AssignUserDepartmentCommand(userId, deptId, true, UUID.randomUUID());

    assertThrows(
        DepartmentNotFoundException.class,
        () -> departmentService.assignUserDepartment(cmd));
  }

  @Test
  @DisplayName("Should allow clearing user department when departmentId is null")
  void shouldAllowClearingUserDepartment() {
    UUID userId = UUID.randomUUID();
    User user =
        User.builder()
            .id(userId)
            .email("user@platform.com")
            .passwordHash("hash")
            .fullName("Jane Doe")
            .departmentId(UUID.randomUUID())
            .enabled(true)
            .internal(true)
            .roles(Set.of())
            .build();

    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
    when(userRepositoryPort.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    AssignUserDepartmentCommand cmd =
        new AssignUserDepartmentCommand(userId, null, false, UUID.randomUUID());

    UserDepartmentResponseDto response = departmentService.assignUserDepartment(cmd);

    assertNotNull(response);
    assertNull(response.departmentId());
    assertNull(response.departmentCode());
    assertFalse(response.internal());
  }
}
