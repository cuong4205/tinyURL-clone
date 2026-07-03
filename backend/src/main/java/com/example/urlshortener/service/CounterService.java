package com.example.urlshortener.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CounterService {

    private static final String COUNTER_KEY = "url:counter";

    private final StringRedisTemplate redisTemplate;

    public CounterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Atomically returns the next sequential id (starting at 1).
     * Redis INCR guarantees this is safe under concurrent requests.
     */
    public long nextId() {
        return redisTemplate.opsForValue().increment(COUNTER_KEY);
    }
}
