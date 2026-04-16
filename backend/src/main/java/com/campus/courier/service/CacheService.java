package com.campus.courier.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String[] CACHE_PREFIXES = {
            "user-profile:", "order-details:", "user-orders:", "courier-orders:", "pwd-reset:"
    };

    /** 手动设置缓存 */
    public void setCache(String key, Object value, long timeout, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /** 获取缓存 */
    public Object getCache(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /** 删除缓存 */
    public void deleteCache(String key) {
        redisTemplate.delete(key);
    }

    /** 清除所有业务缓存（使用 SCAN 避免阻塞） */
    public void clearAllCaches() {
        for (String prefix : CACHE_PREFIXES) {
            Set<String> keys = scanKeys(prefix + "*");
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }

    private Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        return keys;
    }
}
