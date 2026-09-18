package com.platform.app.iam.application.services;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.platform.app.iam.application.dto.DepartmentCreatedEvent;
import com.platform.app.iam.application.dto.DepartmentResponseDto;
import com.platform.app.iam.application.dto.DepartmentUpdatedEvent;
import com.platform.app.iam.application.dto.UserDepartmentAssignedEvent;
import com.platform.app.iam.application.dto.UserDepartmentResponseDto;
import com.platform.app.iam.application.ports.inbound.AssignUserDepartmentCommand;
import com.platform.app.iam.application.ports.inbound.AssignUserDepartmentUseCase;
import com.platform.app.iam.application.ports.inbound.CreateDepartmentCommand;
import com.platform.app.iam.application.ports.inbound.CreateDepartmentUseCase;
import com.platform.app.iam.application.ports.inbound.GetDepartmentUseCase;
import com.platform.app.iam.application.ports.inbound.UpdateDepartmentCommand;
import com.platform.app.iam.application.ports.inbound.UpdateDepartmentUseCase;
import com.platform.app.iam.application.ports.outbound.DepartmentRepositoryPort;
import com.platform.app.iam.application.ports.outbound.UserRepositoryPort;
import com.platform.app.shared.util.IdGenerator;
import com.platform.app.iam.domain.exception.DepartmentCodeConflictException;
import com.platform.app.iam.domain.exception.DepartmentNotFoundException;
import com.platform.app.iam.domain.model.Department;
import com.platform.app.iam.domain.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentService
    implements CreateDepartmentUseCase,
        UpdateDepartmentUseCase,
        GetDepartmentUseCase,
        AssignUserDepartmentUseCase {

  private final DepartmentRepositoryPort departmentRepositoryPort;
  private final UserRepositoryPort userRepositoryPort;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public DepartmentResponseDto createDepartment(CreateDepartmentCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    String validatedCode = Department.validateCode(command.code());

    if (departmentRepositoryPort.existsByCode(validatedCode)) {
      throw new DepartmentCodeConflictException(
          "Department code already exists: " + validatedCode);
    }

    Department department =
        Department.builder()
            .id(IdGenerator.nextId())
            .code(validatedCode)
            .name(command.name())
            .description(command.description())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    Department saved = departmentRepositoryPort.save(department);
    log.info("Created department [{}] with id [{}] by operator [{}]", saved.getCode(), saved.getId(), command.operatorUserId());

    eventPublisher.publishEvent(
        DepartmentCreatedEvent.builder()
            .departmentId(saved.getId())
            .code(saved.getCode())
            .name(saved.getName())
            .operatorUserId(command.operatorUserId())
            .timestamp(Instant.now())
            .build());

    return toResponseDto(saved);
  }

  @Override
  @Transactional
  public DepartmentResponseDto updateDepartment(UpdateDepartmentCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    Objects.requireNonNull(command.departmentId(), "departmentId must not be null");

    Department department =
        departmentRepositoryPort
            .findById(command.departmentId())
            .orElseThrow(
                () ->
                    new DepartmentNotFoundException(
                        "Department not found: " + command.departmentId()));

    String validatedCode = Department.validateCode(command.code());
    if (departmentRepositoryPort.existsByCodeAndIdNot(validatedCode, command.departmentId())) {
      throw new DepartmentCodeConflictException(
          "Department code already exists: " + validatedCode);
    }

    department.updateDetails(validatedCode, command.name(), command.description());
    Department updated = departmentRepositoryPort.save(department);
    log.info("Updated department [{}] ({}) by operator [{}]", updated.getCode(), updated.getId(), command.operatorUserId());

    eventPublisher.publishEvent(
        DepartmentUpdatedEvent.builder()
            .departmentId(updated.getId())
            .code(updated.getCode())
            .name(updated.getName())
            .operatorUserId(command.operatorUserId())
            .timestamp(Instant.now())
            .build());

    return toResponseDto(updated);
  }

  @Override
  @Transactional(readOnly = true)
  public DepartmentResponseDto getDepartmentById(UUID departmentId) {
    Objects.requireNonNull(departmentId, "departmentId must not be null");

    Department department =
        departmentRepositoryPort
            .findById(departmentId)
            .orElseThrow(
                () ->
                    new DepartmentNotFoundException("Department not found: " + departmentId));

    return toResponseDto(department);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DepartmentResponseDto> listDepartments() {
    return departmentRepositoryPort.findAll().stream().map(this::toResponseDto).toList();
  }

  @Override
  @Transactional
  public UserDepartmentResponseDto assignUserDepartment(AssignUserDepartmentCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    Objects.requireNonNull(command.targetUserId(), "targetUserId must not be null");

    User user =
        userRepositoryPort
            .findById(command.targetUserId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "User not found: " + command.targetUserId()));

    Department department = null;
    if (command.departmentId() != null) {
      department =
          departmentRepositoryPort
              .findById(command.departmentId())
              .orElseThrow(
                  () ->
                      new DepartmentNotFoundException(
                          "Department not found: " + command.departmentId()));
    }

    user.assignDepartment(command.departmentId(), command.internal());
    User savedUser = userRepositoryPort.save(user);
    log.info(
        "Assigned user [{}] to department [{}] (internal={}) by operator [{}]",
        savedUser.getId(),
        command.departmentId(),
        command.internal(),
        command.operatorUserId());

    eventPublisher.publishEvent(
        UserDepartmentAssignedEvent.builder()
            .targetUserId(savedUser.getId())
            .departmentId(command.departmentId())
            .internal(command.internal())
            .operatorUserId(command.operatorUserId())
            .timestamp(Instant.now())
            .build());

    return UserDepartmentResponseDto.builder()
        .userId(savedUser.getId())
        .email(savedUser.getEmail())
        .fullName(savedUser.getFullName())
        .departmentId(savedUser.getDepartmentId())
        .departmentCode(department != null ? department.getCode() : null)
        .departmentName(department != null ? department.getName() : null)
        .internal(savedUser.isInternal())
        .updatedAt(savedUser.getUpdatedAt())
        .build();
  }

  private DepartmentResponseDto toResponseDto(Department department) {
    return DepartmentResponseDto.builder()
        .id(department.getId())
        .code(department.getCode())
        .name(department.getName())
        .description(department.getDescription())
        .createdAt(department.getCreatedAt())
        .updatedAt(department.getUpdatedAt())
        .build();
  }
}
