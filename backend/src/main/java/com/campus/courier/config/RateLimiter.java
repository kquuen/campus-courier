package com.campus.courier.config;


import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimiter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.builder().capacity(100).refillIntervally(100, Duration.ofMinutes(1)).build();
        return Bucket.builder().addLimit(limit).build();
    }

    public boolean tryConsume(String apiKey) {
        Bucket bucket = buckets.computeIfAbsent(apiKey, k -> createNewBucket());
        boolean consumed = bucket.tryConsume(1);
        
        if (!consumed) {
            log.warn("限流触发: apiKey={}, 请求被拒绝", apiKey);
        }
        
        return consumed;
    }

    public void reset(String apiKey) {
        buckets.remove(apiKey);
    }
}
