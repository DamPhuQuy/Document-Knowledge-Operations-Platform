package com.platform.app.iam.application.ports.inbound;

import java.util.UUID;

public record UpdateDepartmentCommand(
    UUID departmentId,
    String code,
    String name,
    String description,
    UUID operatorUserId
) {}
