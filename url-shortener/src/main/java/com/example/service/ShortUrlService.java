package com.example.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dto.CreateShortUrlRequest;
import com.example.dto.CreateShortUrlResponse;
import com.example.dto.UrlAnalyticsResponse;
import com.example.entity.ShortUrl;
import com.example.exception.InvalidExpiryException;
import com.example.exception.InvalidUrlException;
import com.example.exception.ShortUrlNotFoundException;
import com.example.exception.UrlExpiredException;
import com.example.repository.ShortUrlRepository;
import com.example.util.ShortCodeGenerator;

@Service
public class ShortUrlService {

    private final ShortUrlRepository shortUrlRepository;
    private final UrlCacheService urlCacheService;

    @Value("${app.base-url}")
    private String baseUrl;

    public ShortUrlService(ShortUrlRepository shortUrlRepository, UrlCacheService urlCacheService) {
        this.shortUrlRepository = shortUrlRepository;
        this.urlCacheService = urlCacheService;
    }

    public CreateShortUrlResponse createShortUrl(CreateShortUrlRequest request) {
        validateOriginalUrl(request.getOriginalUrl());
        validateExpiry(request.getExpiresAt());

        String shortCode = generateUniqueShortCode();

        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setShortCode(shortCode);
        shortUrl.setOriginalUrl(request.getOriginalUrl());
        shortUrl.setCreatedAt(LocalDateTime.now());
        shortUrl.setExpiresAt(request.getExpiresAt());
        shortUrl.setClickCount(0);
        shortUrl.setLastAccessedAt(null);

        ShortUrl savedShortUrl = shortUrlRepository.save(shortUrl);

        return new CreateShortUrlResponse(
                savedShortUrl.getShortCode(),
                baseUrl + "/" + savedShortUrl.getShortCode(),
                savedShortUrl.getOriginalUrl(),
                savedShortUrl.getCreatedAt(),
                savedShortUrl.getExpiresAt()
        );
    }

    @Transactional
    public String resolveAndTrackShortUrl(String shortCode) {
        String cachedOriginalUrl = urlCacheService.getCachedOriginalUrl(shortCode);

        if (cachedOriginalUrl != null) {
            shortUrlRepository.updateClickAnalytics(shortCode, LocalDateTime.now());
            return cachedOriginalUrl;
        }

        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException("Short URL not found"));

        if (shortUrl.getExpiresAt() != null && !shortUrl.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new UrlExpiredException("Short URL has expired");
        }

        shortUrl.setClickCount(shortUrl.getClickCount() + 1);
        shortUrl.setLastAccessedAt(LocalDateTime.now());
        shortUrlRepository.save(shortUrl);

        urlCacheService.cacheOriginalUrl(
                shortUrl.getShortCode(),
                shortUrl.getOriginalUrl(),
                shortUrl.getExpiresAt()
        );

        return shortUrl.getOriginalUrl();
    }

    public UrlAnalyticsResponse getAnalytics(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException("Short URL not found"));

        return new UrlAnalyticsResponse(
                shortUrl.getShortCode(),
                shortUrl.getOriginalUrl(),
                shortUrl.getClickCount(),
                shortUrl.getCreatedAt(),
                shortUrl.getExpiresAt(),
                shortUrl.getLastAccessedAt()
        );
    }

    private void validateOriginalUrl(String originalUrl) {
        try {
            URI uri = new URI(originalUrl);
            if (uri.getScheme() == null || (!uri.getScheme().equals("http") && !uri.getScheme().equals("https"))) {
                throw new InvalidUrlException("URL must start with http or https");
            }
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("Invalid URL format");
        }
    }

    private void validateExpiry(LocalDateTime expiresAt) {
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            throw new InvalidExpiryException("Expiry time cannot be in the past");
        }
    }

    private String generateUniqueShortCode() {
        String shortCode;

        do {
            shortCode = ShortCodeGenerator.generateShortCode();
        } while (shortUrlRepository.existsByShortCode(shortCode));

        return shortCode;
    }
}