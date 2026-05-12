package com.example.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateShortUrlRequest {
    @NotBlank(message = "Original URL is required")
    @Size(max = 2048, message = "URL is too long")
    private String originalUrl;
    private LocalDateTime expiresAt;

    private CreateShortUrlRequest(){
    }

    public String getOriginalUrl(){
        return originalUrl;
    }
    public void setOriginalUrl(String originalUrl){
        this.originalUrl = originalUrl;
    }

    public LocalDateTime getExpiresAt(){
        return expiresAt;
    }
    
}
