package com.example.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "short_urls")
public class ShortUrl{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //primary key, auto increments

    @Column(unique = true, nullable = false, length=16)
    private String shortCode;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    @Column(nullable= false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private int clickCount = 0;
    private LocalDateTime lastAccessedAt;

    public ShortUrl(){}
    public ShortUrl(String shortCode, String originalUrl, LocalDateTime createdAt, LocalDateTime expiresAt, int clickCount, LocalDateTime lastAccessedAt){
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.clickCount = clickCount;
        this.lastAccessedAt = lastAccessedAt;

    }

    @PrePersist
    public void PrePersist(){
        if (createdAt == null){
            createdAt = LocalDateTime.now();

        }
    }

    public Long getId(){
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }
    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }
    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getClickCount() {
        return clickCount;
    }
    public void setClickCount(int clickCount) {
        this.clickCount = clickCount;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }
    

}