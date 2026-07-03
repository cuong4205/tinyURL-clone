package com.example.urlshortener.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class ClickCountService {

    private static final String CLICK_KEY_PREFIX = "clicks:count:";
    private static final String DIRTY_SET_KEY    = "clicks:dirty";
    static final String LEADERBOARD_KEY          = "clicks:leaderboard";

    private final StringRedisTemplate redisTemplate;

    public ClickCountService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Atomically increments the click count for a short code in Redis,
     * marks it dirty for the flush job, and updates the leaderboard sorted set.
     */
    public void recordClick(String shortCode) {
        Long newCount = redisTemplate.opsForValue().increment(CLICK_KEY_PREFIX + shortCode);
        redisTemplate.opsForSet().add(DIRTY_SET_KEY, shortCode);
        // Keep the leaderboard in sync: score = current running total
        if (newCount != null) {
            redisTemplate.opsForZSet().add(LEADERBOARD_KEY, shortCode, newCount);
        }
    }

    /**
     * Seeds the leaderboard entry for a newly created short code (score 0).
     * Ensures it appears in the leaderboard even before its first click.
     */
    public void initLeaderboardEntry(String shortCode) {
        redisTemplate.opsForZSet().addIfAbsent(LEADERBOARD_KEY, shortCode, 0);
    }

    /** Returns the top N short codes by click count, highest first. */
    public Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> getTopN(int n) {
        return redisTemplate.opsForZSet()
                .reverseRangeWithScores(LEADERBOARD_KEY, 0, n - 1);
    }

    public Set<String> getDirtyShortCodes() {
        return redisTemplate.opsForSet().members(DIRTY_SET_KEY);
    }

    public long getCount(String shortCode) {
        String value = redisTemplate.opsForValue().get(CLICK_KEY_PREFIX + shortCode);
        return value == null ? 0L : Long.parseLong(value);
    }

    public void clearDirty(String shortCode) {
        redisTemplate.opsForSet().remove(DIRTY_SET_KEY, shortCode);
    }
}
