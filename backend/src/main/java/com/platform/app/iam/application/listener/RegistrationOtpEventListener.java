package com.platform.app.iam.application.listener;

import com.platform.app.iam.application.dto.UserRegisteredOtpEvent;
import com.platform.app.iam.application.ports.outbound.EmailNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationOtpEventListener {

    private final EmailNotificationPort emailNotificationPort;

    @Async
    @EventListener
    public void handleUserRegisteredOtp(UserRegisteredOtpEvent event) {
        log.info(
            "Processing async OTP email dispatch for email [{}]",
            event.email()
        );
        emailNotificationPort.sendOtpEmail(
            event.email(),
            event.otp(),
            event.fullName()
        );
    }
}
