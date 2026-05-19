package com.example.service;

import com.example.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService(stringRedisTemplate);
        ReflectionTestUtils.setField(rateLimiterService, "maxRequests", 5);
        ReflectionTestUtils.setField(rateLimiterService, "windowMinutes", 1);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void validateCreateUrlRateLimit_shouldAllowWithinLimit() {
        when(valueOperations.increment("rate_limit:create:127.0.0.1")).thenReturn(1L);

        rateLimiterService.validateCreateUrlRateLimit("127.0.0.1");

        verify(stringRedisTemplate).expire("rate_limit:create:127.0.0.1", Duration.ofMinutes(1));
    }

    @Test
    void validateCreateUrlRateLimit_shouldThrowWhenLimitExceeded() {
        when(valueOperations.increment("rate_limit:create:127.0.0.1")).thenReturn(6L);

        assertThrows(RateLimitExceededException.class,
                () -> rateLimiterService.validateCreateUrlRateLimit("127.0.0.1"));
    }
}