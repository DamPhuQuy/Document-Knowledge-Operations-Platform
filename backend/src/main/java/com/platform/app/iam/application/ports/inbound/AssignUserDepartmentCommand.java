package com.platform.app.iam.application.ports.inbound;

import java.util.UUID;

public record AssignUserDepartmentCommand(
    UUID targetUserId,
    UUID departmentId,
    boolean internal,
    UUID operatorUserId
) {}
