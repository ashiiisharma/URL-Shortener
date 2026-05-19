package com.example.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.example.exception.RateLimitExceededException;

@Service
public class RateLimiterService {

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.rate-limit.create-url.max-requests}")
    private int maxRequests;

    @Value("${app.rate-limit.create-url.window-minutes}")
    private int windowMinutes;

    public RateLimiterService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void validateCreateUrlRateLimit(String clientId) {
        String key = "rate_limit:create:" + clientId;

        Long currentCount = stringRedisTemplate.opsForValue().increment(key);

        if (currentCount != null && currentCount == 1) {
            stringRedisTemplate.expire(key, Duration.ofMinutes(windowMinutes));
        }

        if (currentCount != null && currentCount > maxRequests) {
            throw new RateLimitExceededException("Rate limit exceeded. Please try again later.");
        }
    }
}