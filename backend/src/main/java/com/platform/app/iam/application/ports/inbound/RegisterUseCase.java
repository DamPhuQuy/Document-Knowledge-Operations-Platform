package com.platform.app.iam.application.ports.inbound;

import com.platform.app.iam.application.dto.UserProfileDto;

public interface RegisterUseCase {
    UserProfileDto execute(RegisterCommand command);
}
