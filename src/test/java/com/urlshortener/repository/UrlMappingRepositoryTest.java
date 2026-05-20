package com.urlshortener.repository;

import com.urlshortener.entity.UrlMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// @DataJpaTest uses H2 in-memory automatically — no MySQL needed for tests
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UrlMappingRepository Tests")
class UrlMappingRepositoryTest {

    @Autowired
    private UrlMappingRepository repository;

    private UrlMapping saved;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        UrlMapping mapping = new UrlMapping();
        mapping.setOriginalUrl("https://www.google.com");
        mapping.setShortCode("aB3xYz");
        mapping.setClickCount(0L);
        saved = repository.save(mapping);
    }

    @Test
    @DisplayName("findByShortCode: should return mapping for existing code")
    void findByShortCode_shouldReturnMapping() {
        Optional<UrlMapping> result = repository.findByShortCode("aB3xYz");

        assertThat(result).isPresent();
        assertThat(result.get().getOriginalUrl()).isEqualTo("https://www.google.com");
    }

    @Test
    @DisplayName("findByShortCode: should return empty for unknown code")
    void findByShortCode_shouldReturnEmptyForUnknown() {
        Optional<UrlMapping> result = repository.findByShortCode("XXXXXX");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByShortCode: should return true for existing code")
    void existsByShortCode_shouldReturnTrue() {
        assertThat(repository.existsByShortCode("aB3xYz")).isTrue();
    }

    @Test
    @DisplayName("existsByShortCode: should return false for unknown code")
    void existsByShortCode_shouldReturnFalse() {
        assertThat(repository.existsByShortCode("XXXXXX")).isFalse();
    }

    @Test
    @DisplayName("incrementClickCount: should increase click count by 1")
    void incrementClickCount_shouldIncreaseByOne() {
        assertThat(saved.getClickCount()).isEqualTo(0L);

        repository.incrementClickCount("aB3xYz");

        UrlMapping updated = repository.findByShortCode("aB3xYz").orElseThrow();
        assertThat(updated.getClickCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("incrementClickCount: should increment correctly multiple times")
    void incrementClickCount_shouldIncrementMultipleTimes() {
        repository.incrementClickCount("aB3xYz");
        repository.incrementClickCount("aB3xYz");
        repository.incrementClickCount("aB3xYz");

        UrlMapping updated = repository.findByShortCode("aB3xYz").orElseThrow();
        assertThat(updated.getClickCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("save: should auto-set createdAt via @PrePersist")
    void save_shouldAutoSetCreatedAt() {
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("save: short_code should be unique — duplicate should fail")
    void save_duplicateShortCodeShouldFail() {
        UrlMapping duplicate = new UrlMapping();
        duplicate.setOriginalUrl("https://www.github.com");
        duplicate.setShortCode("aB3xYz"); // same code
        duplicate.setClickCount(0L);

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            repository.saveAndFlush(duplicate);
        });
    }
}
