package com.platform.app.iam.application.ports.outbound;

import com.platform.app.iam.domain.model.User;

public interface TokenProviderPort {
  String generateAccessToken(User user);

  String generateRefreshTokenString();

  long getAccessTokenExpirationSeconds();
}
