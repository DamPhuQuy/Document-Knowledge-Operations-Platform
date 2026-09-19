package com.platform.app.iam.application.ports.outbound;

public interface AccountLockoutPort {
    boolean isLocked(String email);

    void recordFailure(String email);

    void resetAttempts(String email);
}
