package com.example.controller;

import com.example.service.ShortUrlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedirectControllerTest {

    @Mock
    private ShortUrlService shortUrlService;

    @InjectMocks
    private RedirectController redirectController;

    @Test
    void redirectToOriginalUrl_shouldReturn302WithLocationHeader() {
        when(shortUrlService.resolveAndTrackShortUrl("abc123"))
                .thenReturn("https://www.google.com");

        ResponseEntity<Void> response = redirectController.redirectToOriginalUrl("abc123");

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals(URI.create("https://www.google.com"), response.getHeaders().getLocation());
    }
}