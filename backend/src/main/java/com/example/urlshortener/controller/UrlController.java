package com.example.urlshortener.controller;

import com.example.urlshortener.model.UrlMapping;
import com.example.urlshortener.repository.UrlMappingRepository;
import com.example.urlshortener.service.ClickCountService;
import com.example.urlshortener.service.UrlShortenerService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.*;

@RestController
@CrossOrigin
public class UrlController {

    private final UrlShortenerService urlShortenerService;
    private final ClickCountService clickCountService;
    private final UrlMappingRepository repository;

    public UrlController(UrlShortenerService urlShortenerService,
                         ClickCountService clickCountService,
                         UrlMappingRepository repository) {
        this.urlShortenerService = urlShortenerService;
        this.clickCountService = clickCountService;
        this.repository = repository;
    }

    public record ShortenRequest(@NotBlank String url) {}

    @PostMapping("/api/shorten")
    public ResponseEntity<?> shorten(@RequestBody ShortenRequest request) {
        String originalUrl = request.url();
        if (!isValidUrl(originalUrl)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please enter a valid http(s) URL."));
        }
        String shortUrl = urlShortenerService.createShortUrl(originalUrl);
        return ResponseEntity.ok(Map.of("shortUrl", shortUrl, "originalUrl", originalUrl));
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        Optional<String> originalUrl = urlShortenerService.resolveOriginalUrl(shortCode);
        if (originalUrl.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        clickCountService.recordClick(shortCode);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(originalUrl.get()));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/api/stats/{shortCode}")
    public ResponseEntity<?> stats(@PathVariable String shortCode) {
        Optional<UrlMapping> mapping = repository.findById(shortCode);
        if (mapping.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        long liveCount = Math.max(mapping.get().getClickCount(), clickCountService.getCount(shortCode));
        return ResponseEntity.ok(Map.of(
                "shortCode", shortCode,
                "originalUrl", mapping.get().getOriginalUrl(),
                "clickCount", liveCount,
                "createdAt", mapping.get().getCreatedAt()
        ));
    }

    @GetMapping("/api/leaderboard")
    public ResponseEntity<?> leaderboard(@RequestParam(defaultValue = "10") int limit) {
        Set<ZSetOperations.TypedTuple<String>> topEntries = clickCountService.getTopN(limit);

        if (topEntries == null || topEntries.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> entry : topEntries) {
            String shortCode = entry.getValue();
            long clicks = entry.getScore() != null ? entry.getScore().longValue() : 0L;

            // Look up the original URL from MongoDB for display
            String originalUrl = repository.findById(shortCode)
                    .map(UrlMapping::getOriginalUrl)
                    .orElse("unknown");

            result.add(Map.of(
                    "shortCode", shortCode,
                    "originalUrl", originalUrl,
                    "clickCount", clicks
            ));
        }

        return ResponseEntity.ok(result);
    }

    private boolean isValidUrl(String url) {
        if (url == null || url.isBlank()) return false;
        try {
            URI uri = URI.create(url);
            return uri.isAbsolute() && (uri.getScheme().equals("http") || uri.getScheme().equals("https"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
