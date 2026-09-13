package com.platform.app.iam.application.ports.inbound;

import java.util.Set;
import java.util.UUID;

public record AssignRolesCommand(
    UUID targetUserId,
    Set<UUID> roleIds,
    UUID operatorUserId
) {}
