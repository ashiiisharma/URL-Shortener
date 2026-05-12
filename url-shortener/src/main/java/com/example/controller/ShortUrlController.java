package com.example.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.example.dto.CreateShortUrlRequest;
import com.example.dto.CreateShortUrlResponse;
import com.example.service.ShortUrlService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/urls")
@Validated
public class ShortUrlController {
     private final ShortUrlService shortUrlService;

     public ShortUrlController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
     }

    @PostMapping
    public ResponseEntity<CreateShortUrlResponse> createShortUrl(@Valid @RequestBody CreateShortUrlRequest request) {
        CreateShortUrlResponse response = shortUrlService.createShortUrl(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
