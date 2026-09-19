package com.platform.app.iam.application.services;

import com.platform.app.iam.application.ports.inbound.VerifyOtpCommand;
import com.platform.app.iam.application.ports.inbound.VerifyOtpUseCase;
import com.platform.app.iam.application.ports.outbound.OtpRepositoryPort;
import com.platform.app.iam.application.ports.outbound.UserRepositoryPort;
import com.platform.app.iam.domain.exception.InvalidCredentialsException;
import com.platform.app.iam.domain.exception.InvalidOtpException;
import com.platform.app.iam.domain.exception.OtpExpiredException;
import com.platform.app.iam.domain.model.User;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerifyOtpService implements VerifyOtpUseCase {

    private final OtpRepositoryPort otpRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    @Override
    @Transactional
    public void execute(VerifyOtpCommand command) {
        Objects.requireNonNull(command, "VerifyOtpCommand must not be null");
        String email = command.email().trim().toLowerCase();
        String submittedOtp = command.otp().trim();

        String storedOtp = otpRepositoryPort.getOtp(email).orElseThrow(() -> {
            log.warn(
                "OTP verification failed: OTP expired or not found for email [{}]",
                email
            );
            return new OtpExpiredException(
                "OTP has expired or does not exist. Please request a new one."
            );
        });

        if (!storedOtp.equals(submittedOtp)) {
            log.warn(
                "OTP verification failed: invalid OTP for email [{}]",
                email
            );
            throw new InvalidOtpException("Invalid OTP provided.");
        }

        // OTP matched -> delete from Redis immediately to prevent replay
        otpRepositoryPort.deleteOtp(email);

        // Activate user account
        User user = userRepositoryPort
            .findByEmail(email)
            .orElseThrow(() ->
                new InvalidCredentialsException("User not found: " + email)
            );

        user.enable();
        userRepositoryPort.save(user);

        log.info(
            "Account successfully activated via OTP for user ID [{}] and email [{}]",
            user.getId(),
            email
        );
    }
}
