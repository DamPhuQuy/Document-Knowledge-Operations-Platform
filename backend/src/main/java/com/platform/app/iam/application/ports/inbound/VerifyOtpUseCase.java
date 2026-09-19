package com.platform.app.iam.application.ports.inbound;

public interface VerifyOtpUseCase {
    void execute(VerifyOtpCommand command);
}
