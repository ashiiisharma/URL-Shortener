package com.example;

import com.example.dto.CreateShortUrlRequest;
import com.example.dto.CreateShortUrlResponse;
import com.example.dto.UrlAnalyticsResponse;
import com.example.entity.ShortUrl;
import com.example.exception.InvalidExpiryException;
import com.example.exception.ShortUrlNotFoundException;
import com.example.exception.UrlExpiredException;
import com.example.repository.ShortUrlRepository;
import com.example.service.ShortUrlService;
import com.example.service.UrlCacheService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private UrlCacheService urlCacheService;

    @InjectMocks
    private ShortUrlService shortUrlService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(shortUrlService, "baseUrl", "http://localhost:8080");
    }

    @Test
    void createShortUrl_shouldCreateAndReturnResponse() {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl("https://www.google.com");
        request.setExpiresAt(LocalDateTime.now().plusDays(10));

        when(shortUrlRepository.existsByShortCode(anyString())).thenReturn(false);

        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateShortUrlResponse response = shortUrlService.createShortUrl(request);

        assertNotNull(response);
        assertNotNull(response.getShortCode());
        assertEquals("https://www.google.com", response.getOriginalUrl());
        assertEquals("http://localhost:8080/" + response.getShortCode(), response.getShortUrl());
        assertNotNull(response.getCreatedAt());
        assertEquals(request.getExpiresAt(), response.getExpiresAt());

        ArgumentCaptor<ShortUrl> captor = ArgumentCaptor.forClass(ShortUrl.class);
        verify(shortUrlRepository).save(captor.capture());

        ShortUrl saved = captor.getValue();
        assertEquals("https://www.google.com", saved.getOriginalUrl());
        assertEquals(0, saved.getClickCount());
        assertNull(saved.getLastAccessedAt());
    }

    @Test
    void createShortUrl_shouldThrowWhenExpiryIsInPast() {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl("https://www.google.com");
        request.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        assertThrows(InvalidExpiryException.class, () -> shortUrlService.createShortUrl(request));
        verify(shortUrlRepository, never()).save(any());
    }

    @Test
    void resolveAndTrackShortUrl_shouldReturnCachedUrlAndUpdateAnalytics() {
        String shortCode = "abc123";
        String originalUrl = "https://www.google.com";

        when(urlCacheService.getCachedOriginalUrl(shortCode)).thenReturn(originalUrl);
        when(shortUrlRepository.updateClickAnalytics(eq(shortCode), any(LocalDateTime.class))).thenReturn(1);

        String resolved = shortUrlService.resolveAndTrackShortUrl(shortCode);

        assertEquals(originalUrl, resolved);
        verify(shortUrlRepository).updateClickAnalytics(eq(shortCode), any(LocalDateTime.class));
        verify(shortUrlRepository, never()).findByShortCode(anyString());
    }

    @Test
    void resolveAndTrackShortUrl_shouldFallbackToDbWhenCacheMiss() {
        String shortCode = "abc123";

        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setShortCode(shortCode);
        shortUrl.setOriginalUrl("https://www.google.com");
        shortUrl.setCreatedAt(LocalDateTime.now());
        shortUrl.setExpiresAt(LocalDateTime.now().plusDays(2));
        shortUrl.setClickCount(0);

        when(urlCacheService.getCachedOriginalUrl(shortCode)).thenReturn(null);
        when(shortUrlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(shortUrl));
        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String resolved = shortUrlService.resolveAndTrackShortUrl(shortCode);

        assertEquals("https://www.google.com", resolved);
        verify(shortUrlRepository).save(any(ShortUrl.class));
        verify(urlCacheService).cacheOriginalUrl(eq(shortCode), eq("https://www.google.com"), any(LocalDateTime.class));
    }

    @Test
    void resolveAndTrackShortUrl_shouldThrowWhenNotFound() {
        when(urlCacheService.getCachedOriginalUrl("missing")).thenReturn(null);
        when(shortUrlRepository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThrows(ShortUrlNotFoundException.class,
                () -> shortUrlService.resolveAndTrackShortUrl("missing"));
    }

    @Test
    void resolveAndTrackShortUrl_shouldThrowWhenExpired() {
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setShortCode("expired1");
        shortUrl.setOriginalUrl("https://www.google.com");
        shortUrl.setCreatedAt(LocalDateTime.now().minusDays(2));
        shortUrl.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(urlCacheService.getCachedOriginalUrl("expired1")).thenReturn(null);
        when(shortUrlRepository.findByShortCode("expired1")).thenReturn(Optional.of(shortUrl));

        assertThrows(UrlExpiredException.class,
                () -> shortUrlService.resolveAndTrackShortUrl("expired1"));
    }

    @Test
    void getAnalytics_shouldReturnAnalyticsResponse() {
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setShortCode("abc123");
        shortUrl.setOriginalUrl("https://www.google.com");
        shortUrl.setCreatedAt(LocalDateTime.now().minusDays(1));
        shortUrl.setExpiresAt(LocalDateTime.now().plusDays(5));
        shortUrl.setClickCount(3);
        shortUrl.setLastAccessedAt(LocalDateTime.now());

        when(shortUrlRepository.findByShortCode("abc123")).thenReturn(Optional.of(shortUrl));

        UrlAnalyticsResponse response = shortUrlService.getAnalytics("abc123");

        assertNotNull(response);
        assertEquals("abc123", response.getShortCode());
        assertEquals("https://www.google.com", response.getOriginalUrl());
        assertEquals(3, response.getClickCount());
        assertNotNull(response.getLastAccessedAt());
    }
}