package com.paranietharan.tripgrid.auth.service;

import com.paranietharan.tripgrid.auth.exception.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;

@Service
public class VerificationCodeService {

    private static final Logger log = LoggerFactory.getLogger(VerificationCodeService.class);
    private static final String VERIFICATION_KEY_PREFIX = "auth:verification:";
    private static final String RATE_LIMIT_KEY_PREFIX = "auth:rate:resend:";

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom;
    private final int expirationMinutes;
    private final int maxResendAttempts;
    private final int resendWindowMinutes;

    public VerificationCodeService(
            StringRedisTemplate redisTemplate,
            @Value("${tripgrid.verification.code-expiration-minutes:10}") int expirationMinutes,
            @Value("${tripgrid.verification.max-resend-attempts:3}") int maxResendAttempts,
            @Value("${tripgrid.verification.resend-window-minutes:15}") int resendWindowMinutes) {
        this.redisTemplate = redisTemplate;
        this.expirationMinutes = expirationMinutes;
        this.maxResendAttempts = maxResendAttempts;
        this.resendWindowMinutes = resendWindowMinutes;
        this.secureRandom = new SecureRandom();
    }

    public String generateAndStoreCode(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        
        // Generate 6-digit cryptographically secure code
        int codeInt = 100000 + secureRandom.nextInt(900000);
        String code = String.valueOf(codeInt);

        // Store hashed code in Redis with TTL
        String codeHash = hashValue(code);
        String redisKey = VERIFICATION_KEY_PREFIX + normalizedEmail;
        redisTemplate.opsForValue().set(redisKey, codeHash, Duration.ofMinutes(expirationMinutes));

        log.debug("Verification code generated and stored in Redis with TTL {} minutes for {}", expirationMinutes, normalizedEmail);
        return code;
    }

    public boolean verifyCode(String email, String rawCode) {
        String normalizedEmail = email.trim().toLowerCase();
        String redisKey = VERIFICATION_KEY_PREFIX + normalizedEmail;

        String storedHash = redisTemplate.opsForValue().get(redisKey);
        if (storedHash == null) {
            return false;
        }

        String inputHash = hashValue(rawCode.trim());
        boolean matches = MessageDigest.isEqual(
                storedHash.getBytes(StandardCharsets.UTF_8),
                inputHash.getBytes(StandardCharsets.UTF_8)
        );

        if (matches) {
            // Single-use: remove code immediately after successful verification
            redisTemplate.delete(redisKey);
        }

        return matches;
    }

    public void checkAndIncrementRateLimit(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        String rateLimitKey = RATE_LIMIT_KEY_PREFIX + normalizedEmail;

        Long attempts = redisTemplate.opsForValue().increment(rateLimitKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(rateLimitKey, Duration.ofMinutes(resendWindowMinutes));
        }

        if (attempts != null && attempts > maxResendAttempts) {
            log.warn("Rate limit exceeded for resend verification email for {}", normalizedEmail);
            throw new RateLimitExceededException("Too many verification requests. Please try again in a few minutes.");
        }
    }

    private String hashValue(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
