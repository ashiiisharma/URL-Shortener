package com.example.controller;

import com.example.dto.CreateShortUrlRequest;
import com.example.dto.CreateShortUrlResponse;
import com.example.dto.UrlAnalyticsResponse;
import com.example.service.RateLimiterService;
import com.example.service.ShortUrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/urls")
@Validated
public class ShortUrlController {

    private final ShortUrlService shortUrlService;
    private final RateLimiterService rateLimiterService;

    public ShortUrlController(ShortUrlService shortUrlService, RateLimiterService rateLimiterService) {
        this.shortUrlService = shortUrlService;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping
    public ResponseEntity<CreateShortUrlResponse> createShortUrl(@Valid @RequestBody CreateShortUrlRequest request,
                                                                 HttpServletRequest httpServletRequest) {
        String clientIp = httpServletRequest.getRemoteAddr();
        rateLimiterService.validateCreateUrlRateLimit(clientIp);

        CreateShortUrlResponse response = shortUrlService.createShortUrl(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{shortCode}/analytics")
    public ResponseEntity<UrlAnalyticsResponse> getAnalytics(@PathVariable String shortCode) {
        UrlAnalyticsResponse response = shortUrlService.getAnalytics(shortCode);
        return ResponseEntity.ok(response);
    }
}