package com.example.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class UrlCacheService {

    private final StringRedisTemplate stringRedisTemplate;

    public UrlCacheService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private String buildKey(String shortCode) {
        return "url:" + shortCode;
    }

    public String getCachedOriginalUrl(String shortCode) {
        return stringRedisTemplate.opsForValue().get(buildKey(shortCode));
    }

    public void cacheOriginalUrl(String shortCode, String originalUrl, LocalDateTime expiresAt) {
        String key = buildKey(shortCode);

        if (expiresAt != null) {
            Duration ttl = Duration.between(LocalDateTime.now(), expiresAt);
            if (!ttl.isNegative() && !ttl.isZero()) {
                stringRedisTemplate.opsForValue().set(key, originalUrl, ttl);
                return;
            }
        }

        stringRedisTemplate.opsForValue().set(key, originalUrl);
    }

    public void evict(String shortCode) {
        stringRedisTemplate.delete(buildKey(shortCode));
    }
}