package com.platform.app.iam.infrastructure.adapters.secondary.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisOtpAdapterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisOtpAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedisOtpAdapter(redisTemplate);
    }

    @Test
    @DisplayName("Should save, retrieve, and delete OTP successfully using Redis")
    void shouldSaveRetrieveAndDeleteOtpViaRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:register:user@platform.com")).thenReturn("654321");

        // Save
        adapter.saveOtp("USER@Platform.COM", "654321", Duration.ofMinutes(5));
        verify(valueOperations).set("otp:register:user@platform.com", "654321", Duration.ofMinutes(5));

        // Get
        Optional<String> otp = adapter.getOtp("user@platform.com");
        assertThat(otp).contains("654321");

        // Delete
        adapter.deleteOtp("user@platform.com");
        verify(redisTemplate).delete("otp:register:user@platform.com");
    }

    @Test
    @DisplayName("Should fallback to in-memory store when Redis fails on save and get")
    void shouldFallbackToInMemoryStoreWhenRedisFails() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Redis connection refused"))
            .when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        doThrow(new RuntimeException("Redis connection refused"))
            .when(valueOperations).get(anyString());

        // Save fails in Redis, falls back to memory
        adapter.saveOtp("fallback@platform.com", "999888", Duration.ofMinutes(10));

        // Get succeeds from in-memory fallback
        Optional<String> otp = adapter.getOtp("fallback@platform.com");
        assertThat(otp).contains("999888");

        // Delete cleans up in-memory fallback even if Redis throws
        doThrow(new RuntimeException("Redis error on delete")).when(redisTemplate).delete("otp:register:fallback@platform.com");
        adapter.deleteOtp("fallback@platform.com");

        Optional<String> afterDelete = adapter.getOtp("fallback@platform.com");
        assertThat(afterDelete).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when OTP not found in Redis and in-memory")
    void shouldReturnEmptyWhenOtpNotFound() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:register:unknown@platform.com")).thenReturn(null);

        Optional<String> otp = adapter.getOtp("unknown@platform.com");
        assertThat(otp).isEmpty();
    }
}
