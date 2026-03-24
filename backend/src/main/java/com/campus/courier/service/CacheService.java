package com.campus.courier.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

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

    /** 清除所有相关缓存 */
    public void clearAllCaches() {
        Set<String> keys = redisTemplate.keys("campus:courier:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
