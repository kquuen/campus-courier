package com.campus.courier.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 将令牌加入黑名单
     */
    public void addToBlacklist(String token, long expirationTime) {
        if (token == null || token.isEmpty()) {
            return;
        }

        long remainingTime = expirationTime - System.currentTimeMillis();
        if (remainingTime > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "blacklisted", remainingTime, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 检查令牌是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    /**
     * 清理过期的黑名单令牌
     */
    public void cleanupExpiredTokens() {
        // Redis会自动清理过期的key，这里不需要额外处理
    }
}