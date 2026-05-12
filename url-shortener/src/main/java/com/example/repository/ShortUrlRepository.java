package com.example.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entity.ShortUrl;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {
    Optional<ShortUrl> findByShortCode(String shortCode);
    boolean existsByShortCode(String shortCode);
    
}