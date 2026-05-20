package com.urlshortener.service;

import com.urlshortener.entity.UrlMapping;
import com.urlshortener.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UrlShortenerService {

    private final UrlMappingRepository repository;

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;

    // FIX #3: SecureRandom instead of Random (thread-safe + cryptographically strong)
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public UrlMapping shortenUrl(String originalUrl) {
        String shortCode = generateUniqueCode();
        UrlMapping mapping = new UrlMapping();
        mapping.setOriginalUrl(originalUrl);
        mapping.setShortCode(shortCode);
        return repository.save(mapping);
    }

    // FIX #1: redirect() now calls THIS method which increments click count
    // Uses atomic DB update - no race condition
    @Transactional
    public Optional<UrlMapping> getOriginalUrl(String shortCode) {
        Optional<UrlMapping> mapping = repository.findByShortCode(shortCode);
        if (mapping.isPresent()) {
            repository.incrementClickCount(shortCode);
        }
        return mapping;
    }

    // Stats endpoint - read only, does NOT increment click count
    @Transactional(readOnly = true)
    public Optional<UrlMapping> getStats(String shortCode) {
        return repository.findByShortCode(shortCode);
    }

    // NEW: Get all URLs (useful for admin / dashboard)
    @Transactional(readOnly = true)
    public List<UrlMapping> getAllUrls() {
        return repository.findAll();
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CHARACTERS.charAt(secureRandom.nextInt(CHARACTERS.length())));
            }
            code = sb.toString();
        } while (repository.existsByShortCode(code));
        return code;
    }
}
