package com.example.urlshortener.service;

import com.example.urlshortener.model.UrlMapping;
import com.example.urlshortener.repository.UrlMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ClickCountFlushJob {

    private static final Logger log = LoggerFactory.getLogger(ClickCountFlushJob.class);

    private final ClickCountService clickCountService;
    private final UrlMappingRepository repository;

    public ClickCountFlushJob(ClickCountService clickCountService, UrlMappingRepository repository) {
        this.clickCountService = clickCountService;
        this.repository = repository;
    }

    @Scheduled(fixedRateString = "${app.click-flush-interval-ms}")
    public void flushDirtyCounts() {
        Set<String> dirtyCodes = clickCountService.getDirtyShortCodes();
        if (dirtyCodes == null || dirtyCodes.isEmpty()) {
            return;
        }

        for (String shortCode : dirtyCodes) {
            long redisCount = clickCountService.getCount(shortCode);

            repository.findById(shortCode).ifPresent(mapping -> {
                mapping.setClickCount(redisCount);
                repository.save(mapping);
            });

            clickCountService.clearDirty(shortCode);
        }

        log.debug("Flushed click counts for {} short code(s) to MongoDB", dirtyCodes.size());
    }
}
