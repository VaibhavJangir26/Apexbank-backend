package com.bluewave.apexbank.utils.services;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private final RedissonClient redissonClient;

    /**
     * Standard Endpoints: 20 requests per 1 minute per IP
     */
    public boolean allowStandardRequest(String clientIp) {
        String key = "ratelimit:standard:" + clientIp;
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);

        // Sets limit if key is newly initialized (20 requests / 1 min)
        rateLimiter.trySetRate(RateType.OVERALL, 20, 1, RateIntervalUnit.MINUTES);
        rateLimiter.expireAsync(Duration.ofMinutes(5)); // Auto-cleanup unused keys in Redis

        return rateLimiter.tryAcquire(1);
    }

    /**
     * Financial Transfer Endpoint: 5 requests per 1 minute per IP
     */
    public boolean allowTransferRequest(String clientIp) {
        String key = "ratelimit:transfer:" + clientIp;
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);

        // Sets strict limit if newly initialized (5 requests / 1 min)
        rateLimiter.trySetRate(RateType.OVERALL, 5, 1, RateIntervalUnit.MINUTES);
        rateLimiter.expireAsync(Duration.ofMinutes(5));

        return rateLimiter.tryAcquire(1);
    }
}