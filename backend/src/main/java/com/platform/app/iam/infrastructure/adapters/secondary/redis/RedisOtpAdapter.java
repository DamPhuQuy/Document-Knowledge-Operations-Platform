package com.platform.app.iam.infrastructure.adapters.secondary.redis;

import com.platform.app.iam.application.ports.outbound.OtpRepositoryPort;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisOtpAdapter implements OtpRepositoryPort {

    private static final String KEY_PREFIX = "otp:register:";

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentMap<String, OtpEntry> inMemoryFallback =
        new ConcurrentHashMap<>();

    private record OtpEntry(String otp, Instant expiresAt) {}

    @Override
    public void saveOtp(String email, String otp, Duration ttl) {
        String key = buildKey(email);
        try {
            redisTemplate.opsForValue().set(key, otp, ttl);
            log.debug(
                "Saved OTP to Redis for key [{}] with TTL [{}]",
                key,
                ttl
            );
        } catch (Exception ex) {
            log.warn(
                "Redis unavailable, falling back to in-memory store for OTP key [{}]: {}",
                key,
                ex.getMessage()
            );
            inMemoryFallback.put(
                key,
                new OtpEntry(otp, Instant.now().plus(ttl))
            );
        }
    }

    @Override
    public Optional<String> getOtp(String email) {
        String key = buildKey(email);
        try {
            String otp = redisTemplate.opsForValue().get(key);
            if (otp != null) {
                return Optional.of(otp);
            }
        } catch (Exception ex) {
            log.warn(
                "Redis unavailable, reading OTP from in-memory fallback for key [{}]: {}",
                key,
                ex.getMessage()
            );
        }

        OtpEntry entry = inMemoryFallback.get(key);
        if (entry != null) {
            if (Instant.now().isBefore(entry.expiresAt())) {
                return Optional.of(entry.otp());
            }
            inMemoryFallback.remove(key);
        }
        return Optional.empty();
    }

    @Override
    public void deleteOtp(String email) {
        String key = buildKey(email);
        try {
            redisTemplate.delete(key);
        } catch (Exception ex) {
            log.warn(
                "Redis unavailable, deleting OTP from in-memory fallback for key [{}]: {}",
                key,
                ex.getMessage()
            );
        }
        inMemoryFallback.remove(key);
    }

    private String buildKey(String email) {
        return KEY_PREFIX + email.trim().toLowerCase();
    }
}
