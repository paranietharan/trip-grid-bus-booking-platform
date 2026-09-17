package com.paranietharan.tripgrid.auth.service;

import com.paranietharan.tripgrid.auth.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationCodeServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private VerificationCodeService verificationCodeService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        verificationCodeService = new VerificationCodeService(redisTemplate, 10, 3, 15);
    }

    @Test
    @DisplayName("Should generate 6-digit code and store hash in Redis with TTL")
    void shouldGenerateAndStoreCode() {
        String email = "user@example.com";
        String code = verificationCodeService.generateAndStoreCode(email);

        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("^[0-9]{6}$"));

        verify(valueOperations).set(eq("auth:verification:" + email), anyString(), eq(Duration.ofMinutes(10)));
    }

    @Test
    @DisplayName("Should verify valid code successfully and delete Redis key (single-use)")
    void shouldVerifyValidCode() {
        String email = "user@example.com";
        String code = "123456";

        // Hash for "123456"
        // SHA-256 for "123456" = 8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92
        String expectedHash = "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";
        when(valueOperations.get("auth:verification:" + email)).thenReturn(expectedHash);

        boolean result = verificationCodeService.verifyCode(email, code);

        assertTrue(result);
        verify(redisTemplate).delete("auth:verification:" + email);
    }

    @Test
    @DisplayName("Should reject invalid code")
    void shouldRejectInvalidCode() {
        String email = "user@example.com";
        String wrongCode = "654321";
        String expectedHash = "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";

        when(valueOperations.get("auth:verification:" + email)).thenReturn(expectedHash);

        boolean result = verificationCodeService.verifyCode(email, wrongCode);

        assertFalse(result);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("Should reject verification when code is expired or absent in Redis")
    void shouldRejectExpiredCode() {
        String email = "user@example.com";
        when(valueOperations.get("auth:verification:" + email)).thenReturn(null);

        boolean result = verificationCodeService.verifyCode(email, "123456");

        assertFalse(result);
    }

    @Test
    @DisplayName("Should enforce rate limiting on resend verification")
    void shouldThrowWhenRateLimitExceeded() {
        String email = "user@example.com";
        when(valueOperations.increment("auth:rate:resend:" + email)).thenReturn(4L);

        assertThrows(RateLimitExceededException.class, () -> verificationCodeService.checkAndIncrementRateLimit(email));
    }
}
