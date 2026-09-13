package com.platform.app.iam.application.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserRolesUpdatedEvent(
    UUID targetUserId,
    Set<String> oldRoleCodes,
    Set<String> newRoleCodes,
    UUID operatorUserId,
    Instant timestamp
) {}
