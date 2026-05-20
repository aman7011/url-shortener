package com.urlshortener.controller;

import com.urlshortener.entity.UrlMapping;
import com.urlshortener.service.UrlShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Tag(name = "URL Shortener", description = "Shorten, redirect, and track URLs")
public class UrlShortenerController {

    private final UrlShortenerService service;

    // ── Shorten a URL ────────────────────────────────────────────────────────
    @PostMapping("/api/shorten")
    @Operation(summary = "Shorten a URL", description = "Returns a 6-character short code for the given URL")
    public ResponseEntity<?> shortenUrl(@RequestBody Map<String, String> body) {
        String originalUrl = body.get("url");
        if (originalUrl == null || originalUrl.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("URL must not be empty");
        }
        // Basic URL format guard
        if (!originalUrl.startsWith("http://") && !originalUrl.startsWith("https://")) {
            return ResponseEntity.badRequest().body("URL must start with http:// or https://");
        }
        UrlMapping mapping = service.shortenUrl(originalUrl);
        Map<String, String> response = new HashMap<>();
        response.put("shortCode", mapping.getShortCode());
        response.put("shortUrl", "http://localhost:8080/" + mapping.getShortCode());
        response.put("originalUrl", mapping.getOriginalUrl());
        return ResponseEntity.ok(response);
    }

    // ── Redirect ─────────────────────────────────────────────────────────────
    // FIX #1: Now calls getOriginalUrl() which DOES increment the click count
    // Previously called getStats() which did NOT increment - bug fixed
    @GetMapping("/{shortCode:[a-zA-Z0-9]{6}}")
    @Operation(summary = "Redirect to original URL", description = "Redirects to the original URL and increments click count")
    public ResponseEntity<?> redirect(@PathVariable String shortCode) {
        Optional<UrlMapping> mapping = service.getOriginalUrl(shortCode);
        return mapping
                .<ResponseEntity<?>>map(m -> ResponseEntity
                        .status(HttpStatus.FOUND)
                        .location(URI.create(m.getOriginalUrl()))
                        .build())
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Short URL not found"));
    }

    // ── Stats for one URL ─────────────────────────────────────────────────────
    @GetMapping("/api/stats/{shortCode}")
    @Operation(summary = "Get stats for a short code", description = "Returns click count and creation date")
    public ResponseEntity<?> getStats(@PathVariable String shortCode) {
        Optional<UrlMapping> mapping = service.getStats(shortCode);
        return mapping
                .<ResponseEntity<?>>map(m -> {
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("shortCode", m.getShortCode());
                    stats.put("originalUrl", m.getOriginalUrl());
                    stats.put("clickCount", m.getClickCount());
                    stats.put("createdAt", m.getCreatedAt().toString());
                    stats.put("shortUrl", "http://localhost:8080/" + m.getShortCode());
                    return ResponseEntity.ok(stats);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Short URL not found"));
    }

    // ── NEW: All URLs (for dashboard) ─────────────────────────────────────────
    @GetMapping("/api/urls")
    @Operation(summary = "List all shortened URLs")
    public ResponseEntity<List<UrlMapping>> getAllUrls() {
        return ResponseEntity.ok(service.getAllUrls());
    }
}
