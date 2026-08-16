package com.platform.app.shared.security;

import com.platform.app.shared.config.properties.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        jwtTokenProvider = new JwtTokenProvider(appProperties);
        jwtTokenProvider.init();
    }

    @Test
    void shouldGenerateAndValidateToken() {
        Long userId = 1L;
        String email = "test@example.com";
        String role = "CUSTOMER";

        String token = jwtTokenProvider.generateTokenFromUser(userId, email, role);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(email, jwtTokenProvider.getUsernameFromToken(token));
    }

    @Test
    void shouldRejectInvalidToken() {
        String invalidToken = "invalid.jwt.token";

        assertFalse(jwtTokenProvider.validateToken(invalidToken));
    }
}
