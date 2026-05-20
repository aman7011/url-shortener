package com.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.entity.UrlMapping;
import com.urlshortener.service.UrlShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlShortenerController.class)
@DisplayName("UrlShortenerController Tests")
class UrlShortenerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UrlShortenerService service;

    private UrlMapping sampleMapping;

    @BeforeEach
    void setUp() {
        sampleMapping = new UrlMapping();
        sampleMapping.setId(1L);
        sampleMapping.setOriginalUrl("https://www.google.com");
        sampleMapping.setShortCode("aB3xYz");
        sampleMapping.setClickCount(3L);
        sampleMapping.setCreatedAt(LocalDateTime.now());
    }

    // ── POST /api/shorten ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/shorten: should return 200 with short URL for valid input")
    void shorten_shouldReturn200ForValidUrl() throws Exception {
        when(service.shortenUrl("https://www.google.com")).thenReturn(sampleMapping);

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("url", "https://www.google.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("aB3xYz"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/aB3xYz"))
                .andExpect(jsonPath("$.originalUrl").value("https://www.google.com"));
    }

    @Test
    @DisplayName("POST /api/shorten: should return 400 for empty URL")
    void shorten_shouldReturn400ForEmptyUrl() throws Exception {
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("url", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/shorten: should return 400 for URL without http/https")
    void shorten_shouldReturn400ForInvalidUrlFormat() throws Exception {
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("url", "www.google.com"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/shorten: should return 400 when url field is missing")
    void shorten_shouldReturn400WhenUrlFieldMissing() throws Exception {
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("wrongField", "https://google.com"))))
                .andExpect(status().isBadRequest());
    }

    // ── GET /{shortCode} ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /{shortCode}: should redirect 302 to original URL")
    void redirect_shouldReturn302ForValidCode() throws Exception {
        when(service.getOriginalUrl("aB3xYz")).thenReturn(Optional.of(sampleMapping));

        mockMvc.perform(get("/aB3xYz"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://www.google.com"));
    }

    @Test
    @DisplayName("GET /{shortCode}: should return 404 for unknown short code")
    void redirect_shouldReturn404ForUnknownCode() throws Exception {
        when(service.getOriginalUrl("XXXXXX")).thenReturn(Optional.empty());

        mockMvc.perform(get("/XXXXXX"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /{shortCode}: should not match non-6-char paths")
    void redirect_shouldNotMatchNonSixCharCode() throws Exception {
        // 5 chars — should NOT match the redirect route
        mockMvc.perform(get("/abcde"))
                .andExpect(status().isNotFound()); // Spring returns 404 — no handler
    }

    // ── GET /api/stats/{shortCode} ────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/stats/{code}: should return stats for valid code")
    void getStats_shouldReturnStatsForValidCode() throws Exception {
        when(service.getStats("aB3xYz")).thenReturn(Optional.of(sampleMapping));

        mockMvc.perform(get("/api/stats/aB3xYz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("aB3xYz"))
                .andExpect(jsonPath("$.clickCount").value(3))
                .andExpect(jsonPath("$.originalUrl").value("https://www.google.com"));
    }

    @Test
    @DisplayName("GET /api/stats/{code}: should return 404 for unknown code")
    void getStats_shouldReturn404ForUnknownCode() throws Exception {
        when(service.getStats(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/stats/XXXXXX"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/urls ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/urls: should return list of all URLs")
    void getAllUrls_shouldReturnList() throws Exception {
        when(service.getAllUrls()).thenReturn(List.of(sampleMapping));

        mockMvc.perform(get("/api/urls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].shortCode").value("aB3xYz"))
                .andExpect(jsonPath("$[0].originalUrl").value("https://www.google.com"))
                .andExpect(jsonPath("$[0].clickCount").value(3));
    }

    @Test
    @DisplayName("GET /api/urls: should return empty list when no URLs")
    void getAllUrls_shouldReturnEmptyList() throws Exception {
        when(service.getAllUrls()).thenReturn(List.of());

        mockMvc.perform(get("/api/urls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
