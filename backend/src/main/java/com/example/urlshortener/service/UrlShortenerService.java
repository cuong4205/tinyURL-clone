package com.example.urlshortener.service;

import com.example.urlshortener.model.UrlMapping;
import com.example.urlshortener.repository.UrlMappingRepository;
import com.example.urlshortener.util.Base62Encoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class UrlShortenerService {

    private static final String URL_CACHE_PREFIX = "url:cache:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final UrlMappingRepository repository;
    private final CounterService counterService;
    private final ClickCountService clickCountService;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.base-url}")
    private String baseUrl;

    public UrlShortenerService(UrlMappingRepository repository,
                               CounterService counterService,
                               ClickCountService clickCountService,
                               StringRedisTemplate redisTemplate) {
        this.repository = repository;
        this.counterService = counterService;
        this.clickCountService = clickCountService;
        this.redisTemplate = redisTemplate;
    }

    public String createShortUrl(String originalUrl) {
        // Return the existing short URL if this original URL was already shortened
        Optional<UrlMapping> existing = repository.findByOriginalUrl(originalUrl);
        if (existing.isPresent()) {
            return baseUrl + existing.get().getId();
        }

        long id = counterService.nextId();
        String shortCode = Base62Encoder.encode(id);

        UrlMapping mapping = new UrlMapping(shortCode, originalUrl, 0L, Instant.now());
        repository.save(mapping);

        redisTemplate.opsForValue().set(URL_CACHE_PREFIX + shortCode, originalUrl, CACHE_TTL);
        clickCountService.initLeaderboardEntry(shortCode);

        return baseUrl + shortCode;
    }

    public Optional<String> resolveOriginalUrl(String shortCode) {
        String cached = redisTemplate.opsForValue().get(URL_CACHE_PREFIX + shortCode);
        if (cached != null) {
            return Optional.of(cached);
        }

        Optional<UrlMapping> mapping = repository.findById(shortCode);
        mapping.ifPresent(m ->
                redisTemplate.opsForValue().set(URL_CACHE_PREFIX + shortCode, m.getOriginalUrl(), CACHE_TTL));

        return mapping.map(UrlMapping::getOriginalUrl);
    }
}
