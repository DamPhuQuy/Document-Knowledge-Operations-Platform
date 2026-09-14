package com.platform.app.iam.application.ports.inbound;

import java.util.UUID;

public record CreateDepartmentCommand(
    String code,
    String name,
    String description,
    UUID operatorUserId
) {}
