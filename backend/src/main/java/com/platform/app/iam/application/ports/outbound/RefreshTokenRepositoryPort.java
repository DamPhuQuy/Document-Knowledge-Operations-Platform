package com.platform.app.iam.application.ports.outbound;

import com.platform.app.iam.domain.model.RefreshToken;
import java.util.Optional;

public interface RefreshTokenRepositoryPort {
    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByToken(String token);
}
