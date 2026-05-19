package com.example.controller;

import com.example.dto.CreateShortUrlRequest;
import com.example.dto.CreateShortUrlResponse;
import com.example.dto.UrlAnalyticsResponse;
import com.example.service.RateLimiterService;
import com.example.service.ShortUrlService;
import com.example.controller.ShortUrlController;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortUrlControllerTest {

    @Mock
    private ShortUrlService shortUrlService;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private ShortUrlController shortUrlController;

    @Test
    void createShortUrl_shouldReturnCreatedResponse() {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl("https://www.google.com");
        request.setExpiresAt(LocalDateTime.now().plusDays(5));

        CreateShortUrlResponse serviceResponse = new CreateShortUrlResponse(
                "abc123",
                "http://localhost:8080/abc123",
                "https://www.google.com",
                LocalDateTime.now(),
                request.getExpiresAt()
        );

        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(shortUrlService.createShortUrl(request)).thenReturn(serviceResponse);

        ResponseEntity<CreateShortUrlResponse> response =
                shortUrlController.createShortUrl(request, httpServletRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("abc123", response.getBody().getShortCode());

        verify(rateLimiterService).validateCreateUrlRateLimit("127.0.0.1");
        verify(shortUrlService).createShortUrl(request);
    }

    @Test
    void getAnalytics_shouldReturnOkResponse() {
        UrlAnalyticsResponse analyticsResponse = new UrlAnalyticsResponse(
                "abc123",
                "https://www.google.com",
                2,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(5),
                LocalDateTime.now()
        );

        when(shortUrlService.getAnalytics("abc123")).thenReturn(analyticsResponse);

        ResponseEntity<UrlAnalyticsResponse> response = shortUrlController.getAnalytics("abc123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getClickCount());
    }
}