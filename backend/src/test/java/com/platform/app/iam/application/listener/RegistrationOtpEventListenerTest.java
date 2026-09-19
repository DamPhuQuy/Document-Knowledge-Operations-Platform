package com.platform.app.iam.application.listener;

import static org.mockito.Mockito.verify;

import com.platform.app.iam.application.dto.UserRegisteredOtpEvent;
import com.platform.app.iam.application.ports.outbound.EmailNotificationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrationOtpEventListenerTest {

    @Mock
    private EmailNotificationPort emailNotificationPort;

    private RegistrationOtpEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new RegistrationOtpEventListener(emailNotificationPort);
    }

    @Test
    @DisplayName("Should delegate OTP email dispatch to EmailNotificationPort")
    void shouldDelegateOtpEmailDispatchToEmailNotificationPort() {
        // Arrange
        UserRegisteredOtpEvent event = UserRegisteredOtpEvent.builder()
            .email("test@platform.com")
            .otp("123456")
            .fullName("Test User")
            .build();

        // Act
        listener.handleUserRegisteredOtp(event);

        // Assert
        verify(emailNotificationPort).sendOtpEmail("test@platform.com", "123456", "Test User");
    }
}
