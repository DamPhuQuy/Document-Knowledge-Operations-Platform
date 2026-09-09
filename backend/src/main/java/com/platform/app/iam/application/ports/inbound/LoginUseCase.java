package com.platform.app.iam.application.ports.inbound;

import com.platform.app.iam.application.dto.AuthTokensDto;

public interface LoginUseCase {
  AuthTokensDto execute(LoginCommand command);
}
