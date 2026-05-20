package com.urlshortener.service;

import com.urlshortener.entity.UrlMapping;
import com.urlshortener.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlShortenerService Tests")
class UrlShortenerServiceTest {

    @Mock
    private UrlMappingRepository repository;

    @InjectMocks
    private UrlShortenerService service;

    private UrlMapping sampleMapping;

    @BeforeEach
    void setUp() {
        sampleMapping = new UrlMapping();
        sampleMapping.setId(1L);
        sampleMapping.setOriginalUrl("https://www.google.com");
        sampleMapping.setShortCode("aB3xYz");
        sampleMapping.setClickCount(0L);
        sampleMapping.setCreatedAt(LocalDateTime.now());
    }

    // ── shortenUrl ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shortenUrl: should save and return a new UrlMapping")
    void shortenUrl_shouldSaveAndReturnMapping() {
        when(repository.existsByShortCode(anyString())).thenReturn(false);
        when(repository.save(any(UrlMapping.class))).thenReturn(sampleMapping);

        UrlMapping result = service.shortenUrl("https://www.google.com");

        assertThat(result).isNotNull();
        assertThat(result.getOriginalUrl()).isEqualTo("https://www.google.com");
        assertThat(result.getShortCode()).isEqualTo("aB3xYz");
        verify(repository, times(1)).save(any(UrlMapping.class));
    }

    @Test
    @DisplayName("shortenUrl: should generate a unique code if first one already exists")
    void shortenUrl_shouldRetryIfCodeAlreadyExists() {
        // First code exists, second one is free
        when(repository.existsByShortCode(anyString()))
                .thenReturn(true)   // first attempt — taken
                .thenReturn(false); // second attempt — free
        when(repository.save(any(UrlMapping.class))).thenReturn(sampleMapping);

        UrlMapping result = service.shortenUrl("https://www.google.com");

        assertThat(result).isNotNull();
        // existsByShortCode must have been called at least twice
        verify(repository, atLeast(2)).existsByShortCode(anyString());
    }

    @Test
    @DisplayName("shortenUrl: generated short code should be exactly 6 characters")
    void shortenUrl_generatedCodeShouldBeSixChars() {
        when(repository.existsByShortCode(anyString())).thenReturn(false);
        when(repository.save(any(UrlMapping.class))).thenAnswer(invocation -> {
            UrlMapping m = invocation.getArgument(0);
            // Verify code length at save time
            assertThat(m.getShortCode()).hasSize(6);
            return m;
        });

        service.shortenUrl("https://www.google.com");
        verify(repository).save(any(UrlMapping.class));
    }

    // ── getOriginalUrl ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getOriginalUrl: should return mapping and increment click count")
    void getOriginalUrl_shouldReturnMappingAndIncrementClick() {
        when(repository.findByShortCode("aB3xYz")).thenReturn(Optional.of(sampleMapping));
        doNothing().when(repository).incrementClickCount("aB3xYz");

        Optional<UrlMapping> result = service.getOriginalUrl("aB3xYz");

        assertThat(result).isPresent();
        assertThat(result.get().getOriginalUrl()).isEqualTo("https://www.google.com");
        // Click count MUST be incremented on redirect — this was the original bug
        verify(repository, times(1)).incrementClickCount("aB3xYz");
    }

    @Test
    @DisplayName("getOriginalUrl: should return empty and NOT increment if code not found")
    void getOriginalUrl_shouldNotIncrementIfNotFound() {
        when(repository.findByShortCode("NOTFOUND")).thenReturn(Optional.empty());

        Optional<UrlMapping> result = service.getOriginalUrl("NOTFOUND");

        assertThat(result).isEmpty();
        // Must NOT call increment if URL doesn't exist
        verify(repository, never()).incrementClickCount(anyString());
    }

    // ── getStats ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getStats: should return mapping without incrementing click count")
    void getStats_shouldReturnMappingWithoutIncrement() {
        when(repository.findByShortCode("aB3xYz")).thenReturn(Optional.of(sampleMapping));

        Optional<UrlMapping> result = service.getStats("aB3xYz");

        assertThat(result).isPresent();
        assertThat(result.get().getClickCount()).isEqualTo(0L);
        // Stats must NEVER increment click count
        verify(repository, never()).incrementClickCount(anyString());
    }

    @Test
    @DisplayName("getStats: should return empty optional for unknown code")
    void getStats_shouldReturnEmptyForUnknownCode() {
        when(repository.findByShortCode("UNKNOWN")).thenReturn(Optional.empty());

        Optional<UrlMapping> result = service.getStats("UNKNOWN");

        assertThat(result).isEmpty();
    }

    // ── getAllUrls ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllUrls: should return all saved mappings")
    void getAllUrls_shouldReturnAllMappings() {
        UrlMapping second = new UrlMapping();
        second.setId(2L);
        second.setOriginalUrl("https://www.github.com");
        second.setShortCode("xY9kLm");
        second.setClickCount(5L);

        when(repository.findAll()).thenReturn(List.of(sampleMapping, second));

        List<UrlMapping> result = service.getAllUrls();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getShortCode()).isEqualTo("aB3xYz");
        assertThat(result.get(1).getShortCode()).isEqualTo("xY9kLm");
    }

    @Test
    @DisplayName("getAllUrls: should return empty list when no URLs exist")
    void getAllUrls_shouldReturnEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        List<UrlMapping> result = service.getAllUrls();

        assertThat(result).isEmpty();
    }
}
